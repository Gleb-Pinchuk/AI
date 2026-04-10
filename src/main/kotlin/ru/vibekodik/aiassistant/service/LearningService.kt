package ru.vibekodik.aiassistant.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import ru.vibekodik.aiassistant.model.LearningRecord
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
class LearningService(private val project: Project) {
    private val logger = Logger.getInstance(LearningService::class.java)
    private val memory = CopyOnWriteArrayList<LearningRecord>()

    fun addRecord(record: LearningRecord) {
        memory.add(record)
        logger.info("Learning record added. accepted=${record.accepted}, total=${memory.size}")
    }

    fun buildAdaptiveSystemPrompt(): String {
        val accepted = memory.filter { it.accepted }.takeLast(5)
        if (accepted.isEmpty()) {
            return "You are a precise coding assistant for PyCharm plugin users. Return concise, production-ready code."
        }

        val patterns = accepted.joinToString("\n") {
            "- Successful style pattern: prompt='${it.prompt.take(120)}', response='${it.response.take(120)}'"
        }

        return """
            You are a precise coding assistant for PyCharm plugin users.
            Adapt to previously accepted responses.
            Follow these successful patterns:
            $patterns
            Always return concise, production-ready code.
        """.trimIndent()
    }

    fun userTeach(instruction: String) {
        val synthetic = LearningRecord(
            prompt = "user_instruction",
            response = instruction,
            accepted = true,
            timestamp = System.currentTimeMillis()
        )
        addRecord(synthetic)
    }
}
