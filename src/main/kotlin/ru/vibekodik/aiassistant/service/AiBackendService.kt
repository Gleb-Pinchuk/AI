package ru.vibekodik.aiassistant.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import ru.vibekodik.aiassistant.model.LearningRecord

@Service(Service.Level.PROJECT)
class AiBackendService(private val project: Project) {
    private val logger = Logger.getInstance(AiBackendService::class.java)
    private val learningService = project.getService(LearningService::class.java)
    private val projectContextService = project.getService(ProjectContextService::class.java)

    fun processUserPrompt(rawPrompt: String): Result<String> {
        val apiKey = System.getenv("OPENAI_API_KEY")
            ?: return Result.failure(IllegalStateException("OPENAI_API_KEY is not set"))

        val client = OpenAiClient(apiKey)
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
}
