package com.kangtaeyoung.daynote.data.sync

import com.kangtaeyoung.daynote.core.nowMillis
import com.kangtaeyoung.daynote.data.security.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Properties

/**
 * 데스크톱용 구글 OAuth 클라이언트 설정 — 빌드 시 keystore.properties 의
 * `googleDesktopClientId`/`googleDesktopClientSecret` 를 리소스로 주입한 값을 읽는다
 * (build.gradle.kts 의 generateDesktopOAuthProps). 값이 비면 데스크톱 동기화는 비활성.
 *
 * 데스크톱(설치형) 앱의 client secret 은 구글 정책상 기밀로 취급되지 않는다
 * (배포물에 포함되는 게 정상 — PKCE 가 실제 보호를 담당).
 */
object DesktopOAuthConfig {
    private val props: Properties by lazy {
        Properties().apply {
            DesktopOAuthConfig::class.java.getResourceAsStream("/google-oauth-desktop.properties")
                ?.use { load(it) }
        }
    }

    val clientId: String get() = props.getProperty("clientId").orEmpty().trim()
    val clientSecret: String get() = props.getProperty("clientSecret").orEmpty().trim()
    val isConfigured: Boolean get() = clientId.isNotBlank()
}

/**
 * 데스크톱 구글 인증 — **PKCE + 루프백** 표준 데스크톱 앱 플로우(새 의존성 0).
 *
 * 1) 임시 로컬 포트(ServerSocket)를 열고, 시스템 브라우저로 구글 동의 화면을 띄운다.
 * 2) 동의 후 브라우저가 `http://127.0.0.1:{port}/?code=…` 로 돌아오면 인가 코드를 받는다.
 * 3) 토큰 엔드포인트에서 코드 → 액세스/리프레시 토큰 교환, [SecureStore] 에 저장.
 * 4) 이후에는 리프레시 토큰으로 **무음 갱신** — 재시작해도 브라우저를 다시 열 필요가 없다.
 */
class DesktopGoogleAuth(private val secure: SecureStore) {

    val isConfigured: Boolean get() = DesktopOAuthConfig.isConfigured

    /** 저장된 리프레시 토큰이 있는지(=한 번 로그인한 세션이 살아 있는지). */
    fun hasSession(): Boolean = secure.get(KEY_REFRESH) != null

    fun clear() {
        secure.remove(KEY_ACCESS)
        secure.remove(KEY_EXPIRY)
        secure.remove(KEY_REFRESH)
    }

    /**
     * 브라우저 동의 플로우 실행(블로킹, IO 디스패처) — 성공 시 액세스 토큰 반환·세션 저장.
     * 사용자가 3분 내에 동의하지 않으면 시간 초과 예외.
     */
    suspend fun signIn(): String = withContext(Dispatchers.IO) {
        check(isConfigured) { "데스크톱 OAuth 클라이언트가 설정되지 않았습니다(keystore.properties)." }
        val verifier = randomUrlSafe()
        val challenge = sha256Base64Url(verifier)

        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            server.soTimeout = SIGN_IN_TIMEOUT_MS
            val redirectUri = "http://127.0.0.1:${server.localPort}"
            val authUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + enc(DesktopOAuthConfig.clientId) +
                "&redirect_uri=" + enc(redirectUri) +
                "&response_type=code" +
                "&scope=" + enc(GOOGLE_CALENDAR_SCOPES.joinToString(" ")) +
                "&code_challenge=" + enc(challenge) +
                "&code_challenge_method=S256" +
                // 리프레시 토큰을 항상 받기 위해 offline + consent.
                "&access_type=offline&prompt=consent"

            check(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                "이 환경에서는 브라우저를 열 수 없습니다."
            }
            Desktop.getDesktop().browse(URI(authUrl))

            // 브라우저 리디렉션 대기 — favicon 등 잡요청은 걸러내고 code/error 요청만 처리.
            val code = awaitAuthCode(server)
            exchangeCode(code, verifier, redirectUri)
        }
    }

    /**
     * 유효한 액세스 토큰 확보 — 캐시가 살아 있으면 그대로, 아니면 리프레시 토큰으로 무음 갱신.
     * 리프레시 토큰이 없거나 폐기됐으면 null(→ 설정 화면에서 재로그인 필요).
     */
    suspend fun ensureAccessToken(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = secure.get(KEY_ACCESS)
            val expiry = secure.get(KEY_EXPIRY)?.toLongOrNull() ?: 0L
            if (cached != null && nowMillis() < expiry) return@withContext cached
        }
        val refresh = secure.get(KEY_REFRESH) ?: return@withContext null
        runCatching { refreshAccessToken(refresh) }.getOrNull()
    }

    private fun awaitAuthCode(server: ServerSocket): String {
        val deadline = System.nanoTime() + SIGN_IN_TIMEOUT_MS * 1_000_000L
        repeat(MAX_LOOPBACK_REQUESTS) {
            if (System.nanoTime() > deadline) error("로그인 대기 시간이 초과됐습니다. 다시 시도해 주세요.")
            val socket = server.accept()
            socket.use { s ->
                val params = readQueryParams(s)
                when {
                    params.containsKey("code") -> {
                        respondHtml(s, "<h2>DayNote 로그인 완료</h2><p>이 창을 닫고 DayNote 로 돌아가세요.</p>")
                        return params.getValue("code")
                    }
                    params.containsKey("error") -> {
                        respondHtml(s, "<h2>로그인이 취소되었습니다</h2>")
                        error("구글 로그인 거부/실패: ${params["error"]}")
                    }
                    else -> respondHtml(s, "", notFound = true) // favicon 등 — 계속 대기.
                }
            }
        }
        error("로그인 응답을 받지 못했습니다. 다시 시도해 주세요.")
    }

    /** 루프백으로 들어온 HTTP 요청 첫 줄에서 쿼리 파라미터를 파싱한다. */
    private fun readQueryParams(socket: Socket): Map<String, String> {
        val reader: BufferedReader = socket.getInputStream().bufferedReader()
        val requestLine = reader.readLine() ?: return emptyMap()
        // 예: "GET /?code=xxx&scope=... HTTP/1.1"
        val path = requestLine.substringAfter(' ', "").substringBefore(' ', "")
        val query = path.substringAfter('?', "")
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull { pair ->
            val key = pair.substringBefore('=')
            if (key.isBlank()) return@mapNotNull null
            key to URLDecoder.decode(pair.substringAfter('=', ""), Charsets.UTF_8.name())
        }.toMap()
    }

    private fun respondHtml(socket: Socket, bodyHtml: String, notFound: Boolean = false) {
        val status = if (notFound) "404 Not Found" else "200 OK"
        val html = "<!doctype html><meta charset=\"utf-8\"><body style=\"font-family:sans-serif\">$bodyHtml</body>"
        val bytes = html.toByteArray(Charsets.UTF_8)
        socket.getOutputStream().apply {
            write(
                (
                    "HTTP/1.1 $status\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Connection: close\r\n\r\n"
                    ).toByteArray(Charsets.US_ASCII),
            )
            write(bytes)
            flush()
        }
    }

    private fun exchangeCode(code: String, verifier: String, redirectUri: String): String {
        val json = tokenRequest(
            "code=" + enc(code) +
                "&client_id=" + enc(DesktopOAuthConfig.clientId) +
                "&client_secret=" + enc(DesktopOAuthConfig.clientSecret) +
                "&redirect_uri=" + enc(redirectUri) +
                "&grant_type=authorization_code" +
                "&code_verifier=" + enc(verifier),
        )
        val access = json.getValue("access_token").jsonPrimitive.content
        val expiresIn = json["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
        json["refresh_token"]?.jsonPrimitive?.content?.let { secure.put(KEY_REFRESH, it) }
        storeAccess(access, expiresIn)
        return access
    }

    private fun refreshAccessToken(refreshToken: String): String? {
        val json = try {
            tokenRequest(
                "client_id=" + enc(DesktopOAuthConfig.clientId) +
                    "&client_secret=" + enc(DesktopOAuthConfig.clientSecret) +
                    "&refresh_token=" + enc(refreshToken) +
                    "&grant_type=refresh_token",
            )
        } catch (e: CalendarApiException) {
            // invalid_grant(400) = 리프레시 토큰 폐기(비밀번호 변경·권한 회수 등) → 세션 정리, 재로그인 필요.
            if (e.code == 400) {
                clear()
                return null
            }
            throw e
        }
        val access = json.getValue("access_token").jsonPrimitive.content
        val expiresIn = json["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
        storeAccess(access, expiresIn)
        return access
    }

    private fun storeAccess(access: String, expiresInSec: Long) {
        secure.put(KEY_ACCESS, access)
        // 만료 60초 전부터 갱신 대상으로 취급(시계 오차·전송 지연 여유).
        secure.put(KEY_EXPIRY, (nowMillis() + (expiresInSec - 60) * 1000).toString())
    }

    private fun tokenRequest(formBody: String): kotlinx.serialization.json.JsonObject {
        val conn = (URL("https://oauth2.googleapis.com/token").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            outputStream.use { it.write(formBody.toByteArray(Charsets.UTF_8)) }
        }
        try {
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) throw CalendarApiException(code, "토큰 요청 실패 $code: ${text.take(300)}")
            return Json.parseToJsonElement(text).jsonObject
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun randomUrlSafe(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256Base64Url(input: String): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.US_ASCII)))

    private companion object {
        const val KEY_ACCESS = "google_desktop_access_token"
        const val KEY_EXPIRY = "google_desktop_access_expiry"
        const val KEY_REFRESH = "google_desktop_refresh_token"
        const val SIGN_IN_TIMEOUT_MS = 180_000
        const val MAX_LOOPBACK_REQUESTS = 10
    }
}
