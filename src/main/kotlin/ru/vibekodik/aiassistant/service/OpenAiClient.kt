package ru.vibekodik.aiassistant.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.diagnostic.Logger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.vibekodik.aiassistant.model.ChatMessage
import ru.vibekodik.aiassistant.model.ChatRequest
import ru.vibekodik.aiassistant.model.ChatResponse
import okhttp3.Credentials
import okhttp3.Route
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class OpenAiClient(
    private val apiKey: String,
    private val endpoint: String = "https://api.openai.com/v1/chat/completions"
) {
    private val logger = Logger.getInstance(OpenAiClient::class.java)
    private val httpClient = buildHttpClient()
    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    fun chat(systemPrompt: String, userPrompt: String): Result<String> {
        return try {
            val requestBody = ChatRequest(
                model = "gpt-4o-mini",
                messages = listOf(
                    ChatMessage("system", systemPrompt),
                    ChatMessage("user", userPrompt)
                )
            )

            val json = mapper.writeValueAsString(requestBody)
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string().orEmpty()
                    logger.warn("OpenAI request failed: ${response.code} $errorBody")
                    return Result.failure(IllegalStateException("OpenAI request failed: HTTP ${response.code}"))
                }

                val responseBody = response.body?.string().orEmpty()
                val chatResponse = mapper.readValue(responseBody, ChatResponse::class.java)
                val content = chatResponse.choices.firstOrNull()?.message?.content
                    ?: return Result.failure(IllegalStateException("No response choices returned"))

                Result.success(content)
            }
        } catch (e: Exception) {
            logger.warn("OpenAI request exception", e)
            Result.failure(e)
        }
    }

    private fun buildHttpClient(): OkHttpClient {
        val proxyHost = System.getenv("AI_PROXY_HOST")?.trim().orEmpty()
        val proxyPort = System.getenv("AI_PROXY_PORT")?.toIntOrNull()

        val builder = OkHttpClient.Builder()
            .callTimeout(60, TimeUnit.SECONDS)

        if (proxyHost.isNotBlank() && proxyPort != null) {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
            builder.proxy(proxy)

            val proxyUser = System.getenv("AI_PROXY_USER")?.trim().orEmpty()
            val proxyPassword = System.getenv("AI_PROXY_PASSWORD")?.trim().orEmpty()
            if (proxyUser.isNotBlank()) {
                val credentials = Credentials.basic(proxyUser, proxyPassword)
                builder.proxyAuthenticator { _: Route?, response ->
                    response.request.newBuilder()
                        .header("Proxy-Authorization", credentials)
                        .build()
                }
            }

            logger.info("OpenAI client uses proxy $proxyHost:$proxyPort")
        }

        return builder.build()
    }
}
