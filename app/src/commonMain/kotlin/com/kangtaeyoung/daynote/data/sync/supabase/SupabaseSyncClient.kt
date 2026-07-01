package com.kangtaeyoung.daynote.data.sync.supabase

import com.kangtaeyoung.daynote.data.sync.SupabaseConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Supabase REST/Auth 클라이언트(Ktor — Android·Desktop 공유). PostgREST 의 upsert(merge-duplicates)와
 * 델타 조회(updated_at=gt)를 사용한다. anon key 는 항상 `apikey` 헤더로, 사용자 토큰은 `Authorization` 로.
 */
class SupabaseSyncClient {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    // 일괄 upsert 시 모든 객체의 키가 동일해야 한다(PostgREST PGRST102).
                    // 기본값(null/false 등) 필드도 항상 직렬화해 키 집합을 통일한다.
                    encodeDefaults = true
                    explicitNulls = true
                },
            )
        }
    }

    // --- 인증 ---

    suspend fun signIn(config: SupabaseConfig, email: String, password: String): AuthSession =
        auth(config, email, password, grantType = "password")

    suspend fun signUp(config: SupabaseConfig, email: String, password: String): AuthSession {
        val res = client.post("${base(config)}/auth/v1/signup") {
            header("apikey", config.anonKey)
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(email.trim(), password))
        }
        ensureSuccess(res, "회원가입 실패")
        val body: AuthResponse = res.body()
        // 이메일 확인이 켜져 있으면 토큰이 비어 올 수 있다 → 명확히 안내.
        if (body.accessToken.isBlank()) {
            error("가입됨. 이메일 확인이 필요하거나, 로그인으로 진행하세요.")
        }
        return body.toSession()
    }

    suspend fun refresh(config: SupabaseConfig, refreshToken: String): AuthSession {
        val res = client.post("${base(config)}/auth/v1/token") {
            header("apikey", config.anonKey)
            parameter("grant_type", "refresh_token")
            contentType(ContentType.Application.Json)
            setBody(mapOf("refresh_token" to refreshToken))
        }
        ensureSuccess(res, "세션 갱신 실패")
        return res.body<AuthResponse>().toSession()
    }

    private suspend fun auth(
        config: SupabaseConfig,
        email: String,
        password: String,
        grantType: String,
    ): AuthSession {
        val res = client.post("${base(config)}/auth/v1/token") {
            header("apikey", config.anonKey)
            parameter("grant_type", grantType)
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(email.trim(), password))
        }
        ensureSuccess(res, "로그인 실패 (이메일/비밀번호 확인)")
        return res.body<AuthResponse>().toSession()
    }

    // --- notes ---

    suspend fun pushNotes(config: SupabaseConfig, accessToken: String, rows: List<NoteRow>) {
        if (rows.isEmpty()) return
        val res = client.post("${base(config)}/rest/v1/notes") {
            restHeaders(config, accessToken, upsert = true)
            parameter("on_conflict", "id")
            setBody(rows)
        }
        ensureSuccess(res, "메모 업로드 실패")
    }

    suspend fun pullNotes(config: SupabaseConfig, accessToken: String, since: Long): List<NoteRow> {
        val res = client.get("${base(config)}/rest/v1/notes") {
            restHeaders(config, accessToken, upsert = false)
            parameter("select", "*")
            parameter("updated_at", "gt.$since")
        }
        ensureSuccess(res, "메모 내려받기 실패")
        return res.body()
    }

    // --- tasks ---

    suspend fun pushTasks(config: SupabaseConfig, accessToken: String, rows: List<TaskRow>) {
        if (rows.isEmpty()) return
        val res = client.post("${base(config)}/rest/v1/tasks") {
            restHeaders(config, accessToken, upsert = true)
            parameter("on_conflict", "id")
            setBody(rows)
        }
        ensureSuccess(res, "할 일 업로드 실패")
    }

    suspend fun pullTasks(config: SupabaseConfig, accessToken: String, since: Long): List<TaskRow> {
        val res = client.get("${base(config)}/rest/v1/tasks") {
            restHeaders(config, accessToken, upsert = false)
            parameter("select", "*")
            parameter("updated_at", "gt.$since")
        }
        ensureSuccess(res, "할 일 내려받기 실패")
        return res.body()
    }

    // --- 공통 ---

    private fun io.ktor.client.request.HttpRequestBuilder.restHeaders(
        config: SupabaseConfig,
        accessToken: String,
        upsert: Boolean,
    ) {
        header("apikey", config.anonKey)
        header("Authorization", "Bearer $accessToken")
        contentType(ContentType.Application.Json)
        if (upsert) header("Prefer", "resolution=merge-duplicates,return=minimal")
    }

    /**
     * 프로젝트 기본 URL 로 정규화한다. 사용자가 실수로 `/rest/v1` 또는 `/auth/v1` 까지 붙여 넣어도
     * (대시보드 Data API 화면이 그 형태로 보여줄 때가 있다) 제거해 올바른 호출 경로를 만든다.
     */
    private fun base(config: SupabaseConfig): String {
        var u = config.url.trim().trimEnd('/')
        for (suffix in listOf("/rest/v1", "/auth/v1")) {
            if (u.endsWith(suffix)) u = u.removeSuffix(suffix).trimEnd('/')
        }
        return u
    }

    private fun AuthResponse.toSession(): AuthSession {
        val uid = user?.id ?: ""
        if (accessToken.isBlank() || uid.isBlank()) error("인증 응답이 올바르지 않습니다.")
        return AuthSession(accessToken = accessToken, refreshToken = refreshToken, userId = uid)
    }

    private suspend fun ensureSuccess(res: HttpResponse, context: String) {
        if (res.status.value in 200..299) return
        val detail = runCatching { res.bodyAsText() }.getOrNull().orEmpty()
        throw SupabaseException("$context (${res.status.value})\n$detail".trim(), status = res.status.value)
    }
}

class SupabaseException(message: String, val status: Int? = null) : Exception(message)
