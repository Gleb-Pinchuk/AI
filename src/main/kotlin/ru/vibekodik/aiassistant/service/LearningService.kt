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
    private val userRules = CopyOnWriteArrayList<String>()

    fun addRecord(record: LearningRecord) {
        memory.add(record)
        logger.info("Learning record added. accepted=${record.accepted}, total=${memory.size}")
    }

    fun buildAdaptiveSystemPrompt(): String {
        val accepted = memory.filter { it.accepted && it.prompt != "user_instruction" }.takeLast(8)
        val rules = userRules.takeLast(12)

        val ruleSection = if (rules.isEmpty()) {
            "- No custom user rules yet."
        } else {
            rules.mapIndexed { index, rule -> "${index + 1}. $rule" }.joinToString("\n")
        }

        val patternsSection = if (accepted.isEmpty()) {
            "- No accepted examples yet."
        } else {
            accepted.joinToString("\n") {
                "- Accepted example: prompt='${it.prompt.take(150)}', response='${it.response.take(220)}'"
            }
        }

        return """
            You are Qwen coder assistant running locally in Kodik plugin.
            Core behavior:
            - Return production-ready code.
            - Explain trade-offs briefly.
            - Prefer safe and testable solutions.
            - For edits: show full changed block ready to paste.

            User custom rules (highest priority):
            $ruleSection

            Learned accepted examples:
            $patternsSection
        """.trimIndent()
    }

    fun userTeach(instruction: String) {
        val normalized = instruction.trim()
        if (normalized.isBlank()) {
            return
        }

        userRules.add(normalized)
        val synthetic = LearningRecord(
            prompt = "user_instruction",
            response = normalized,
            accepted = true,
            timestamp = System.currentTimeMillis()
        )
        addRecord(synthetic)
    }

    fun showRules(): List<String> = userRules.toList()
}
