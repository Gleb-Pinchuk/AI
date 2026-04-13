package ru.vibekodik.aiassistant.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.diagnostic.Logger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.vibekodik.aiassistant.model.ChatMessage
import ru.vibekodik.aiassistant.model.OllamaChatRequest
import ru.vibekodik.aiassistant.model.OllamaChatResponse
import java.util.concurrent.TimeUnit

class OllamaClient(
    private val model: String,
    private val endpoint: String = "http://localhost:11434/api/chat",
    private val maxOutputTokens: Int = 1024
) {
    private val logger = Logger.getInstance(OllamaClient::class.java)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .callTimeout(300, TimeUnit.SECONDS)
        .build()

    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun chat(systemPrompt: String, userPrompt: String): Result<String> {
        return try {
            val requestBody = OllamaChatRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                stream = false,
                options = ru.vibekodik.aiassistant.model.OllamaOptions(num_predict = maxOutputTokens)
            )

            val json = mapper.writeValueAsString(requestBody)
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string().orEmpty()
                    logger.warn("Ollama request failed: ${response.code} $errorBody")
                    return Result.failure(
                        IllegalStateException("Ollama request failed: HTTP ${response.code}. Проверьте, запущен ли Ollama и загружена ли модель '$model'.")
                    )
                }

                val responseBody = response.body?.string().orEmpty()
                val chatResponse = mapper.readValue(responseBody, OllamaChatResponse::class.java)
                val content = chatResponse.message?.content?.trim()
                    ?: return Result.failure(IllegalStateException("Ollama не вернул контент ответа"))

                Result.success(content)
            }
        } catch (e: Exception) {
            logger.warn("Ollama request exception", e)
            Result.failure(
                IllegalStateException(
                    "Ошибка подключения к Ollama. Убедитесь, что сервис доступен на $endpoint. Причина: ${e.message}",
                    e
                )
            )
        }
    }
}
