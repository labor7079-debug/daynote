package com.kangtaeyoung.daynote.data.remote.openai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * OpenAI Chat Completions 전용 클라이언트(멀티플랫폼 Ktor). 엔진은 플랫폼별로 클래스패스에서
 * 자동 선택된다(Android=OkHttp, Desktop=CIO). 제공자 분기 없이 OpenAI 하나만 호출한다(CLAUDE.md 4-B).
 */
class OpenAiClient(
    private val baseUrl: String = "https://api.openai.com/",
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
    }

    /**
     * 한 번의 chat completion 호출. 성공 시 첫 choice 의 메시지 본문을 반환한다.
     * 인증 실패(401)·기타 비정상 응답은 메시지를 담아 예외로 던진다(상위 Repository 가 Result 로 감쌈).
     */
    suspend fun chat(
        apiKey: String,
        model: String,
        system: String,
        user: String,
        maxTokens: Int = 1024,
        temperature: Double = 0.7,
    ): String {
        val response: HttpResponse = client.post("${baseUrl}v1/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(
                ChatRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage("system", system),
                        ChatMessage("user", user),
                    ),
                    maxTokens = maxTokens,
                    temperature = temperature,
                ),
            )
        }

        if (!response.status.isSuccess()) {
            val detail = runCatching { response.bodyAsText() }.getOrNull().orEmpty()
            val hint = when (response.status) {
                HttpStatusCode.Unauthorized -> "API 키가 올바른지 확인하세요."
                HttpStatusCode.TooManyRequests -> "요청 한도(또는 잔액)를 초과했습니다."
                else -> ""
            }
            throw OpenAiException("OpenAI 오류 ${response.status.value}. $hint\n$detail".trim())
        }

        val parsed: ChatResponse = response.body()
        return parsed.choices.firstOrNull()?.message?.content?.trim()
            ?: throw OpenAiException("OpenAI 응답이 비어 있습니다.")
    }

    private fun HttpStatusCode.isSuccess(): Boolean = value in 200..299
}

class OpenAiException(message: String) : Exception(message)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 1024,
    val temperature: Double = 0.7,
)

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatResponse(val choices: List<Choice> = emptyList()) {
    @Serializable
    data class Choice(val message: ChatMessage)
}
