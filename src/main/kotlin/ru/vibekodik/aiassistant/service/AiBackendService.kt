package ru.vibekodik.aiassistant.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import ru.vibekodik.aiassistant.model.LearningRecord
import java.util.ArrayDeque

@Service(Service.Level.PROJECT)
class AiBackendService(private val project: Project) {
    private val logger = Logger.getInstance(AiBackendService::class.java)
    private val learningService = project.getService(LearningService::class.java)
    private val projectContextService = project.getService(ProjectContextService::class.java)

    private val ollamaModel = System.getenv("OLLAMA_MODEL") ?: "qwen2.5-coder:7b"
    private val ollamaEndpoint = System.getenv("OLLAMA_ENDPOINT") ?: "http://localhost:11434/api/chat"
    private val maxOutputTokens = System.getenv("AI_MAX_OUTPUT_TOKENS")?.toIntOrNull() ?: 1024
    private val maxRequestsPerMinute = System.getenv("AI_MAX_REQUESTS_PER_MINUTE")?.toIntOrNull() ?: 20

    private val requestLimiter = SlidingWindowRateLimiter(maxRequestsPerMinute, 60_000)

    fun processUserPrompt(rawPrompt: String): Result<String> {
        if (!requestLimiter.tryAcquire()) {
            return Result.failure(
                IllegalStateException(
                    "Превышен лимит запросов: $maxRequestsPerMinute/мин. Подождите немного или увеличьте AI_MAX_REQUESTS_PER_MINUTE."
                )
            )
        }

        val client = OllamaClient(
            model = ollamaModel,
            endpoint = ollamaEndpoint,
            maxOutputTokens = maxOutputTokens
        )

        val context = projectContextService.collectContextSnippet()
        val systemPrompt = learningService.buildAdaptiveSystemPrompt()
        val userPrompt = """
            User request:
            $rawPrompt

            Current editor context:
            $context
        """.trimIndent()

        val result = client.chat(systemPrompt, userPrompt)
        result.onSuccess { response ->
            learningService.addRecord(
                LearningRecord(
                    prompt = rawPrompt,
                    response = response,
                    accepted = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        }.onFailure {
            logger.warn("AI backend processing failed", it)
        }

        return result
    }

    fun markLastAsAccepted(prompt: String, response: String) {
        learningService.addRecord(
            LearningRecord(
                prompt = prompt,
                response = response,
                accepted = true,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun teachAssistant(instruction: String) {
        learningService.userTeach(instruction)
    }

    fun showRules(): List<String> {
        return learningService.showRules()
    }
}
private class SlidingWindowRateLimiter(
    private val maxRequests: Int,
    private val windowMillis: Long
) {
    private val timestamps = ArrayDeque<Long>()

    @Synchronized
    fun tryAcquire(now: Long = System.currentTimeMillis()): Boolean {
        while (timestamps.isNotEmpty() && now - timestamps.first() > windowMillis) {
            timestamps.removeFirst()
        }

        if (timestamps.size >= maxRequests) {
            return false
        }

        timestamps.addLast(now)
        return true
    }
}
