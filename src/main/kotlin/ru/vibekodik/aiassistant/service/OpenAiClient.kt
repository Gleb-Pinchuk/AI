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
import java.util.concurrent.TimeUnit

class OpenAiClient(
    private val apiKey: String,
    private val endpoint: String = "https://api.openai.com/v1/chat/completions"
) {
    private val logger = Logger.getInstance(OpenAiClient::class.java)
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .build()
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
}
