package ru.vibekodik.aiassistant.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextFieldimport ru.vibekodik.aiassistant.service.AiBackendService
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder

class ChatPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val transcript = JTextPane()
    private val input = JBTextField()
    private val backend = project.getService(AiBackendService::class.java)

    private var lastPrompt: String = ""
    private var lastResponse: String = ""

    init {
        border = EmptyBorder(8, 8, 8, 8)

        transcript.isEditable = false
        transcript.background = JBColor.PanelBackground

        val scrollPane = JBScrollPane(transcript)
        scrollPane.preferredSize = Dimension(420, 420)

        val inputPanel = JPanel(BorderLayout(8, 0))
        input.toolTipText = "Опишите задачу: исправить код, сгенерировать файл, объяснить ошибку"

        val sendButton = JButton("Отправить")
        sendButton.addActionListener { onSend() }

        inputPanel.add(input, BorderLayout.CENTER)
        inputPanel.add(sendButton, BorderLayout.EAST)

        val actionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0))
        val insertButton = JButton("Вставить в редактор")
        val fixButton = JButton("Исправить код")
        val generateFileButton = JButton("Сгенерировать файл")
        val teachButton = JButton("Обучить AI")
        val showRulesButton = JButton("Показать правила")

        insertButton.addActionListener { insertToEditor(lastResponse) }
        fixButton.addActionListener { onQuickFix() }
        generateFileButton.addActionListener { onGenerateFile() }
        teachButton.addActionListener { onTeach() }
        showRulesButton.addActionListener { onShowRules() }

        actionsPanel.add(insertButton)
        actionsPanel.add(fixButton)
        actionsPanel.add(generateFileButton)
        actionsPanel.add(teachButton)
        actionsPanel.add(showRulesButton)
        add(JLabel("Kodik AI Assistant"), BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)

        val southPanel = JPanel(BorderLayout(0, 6))
        southPanel.add(inputPanel, BorderLayout.NORTH)
        southPanel.add(actionsPanel, BorderLayout.SOUTH)
        add(southPanel, BorderLayout.SOUTH)
    }

    private fun onSend() {
        val prompt = input.text.trim()
        if (prompt.isBlank()) {
            return
        }
        append("👤 You: $prompt\n")
        append("⏳ Обрабатываю запрос локальной моделью Qwen...\n")
        input.text = ""

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = backend.processUserPrompt(prompt)
            SwingUtilities.invokeLater {
                result.onSuccess { response ->
                    lastPrompt = prompt
                    lastResponse = response
                    append("🤖 AI:\n$response\n\n")
                }.onFailure { error ->
                    append("❌ Error: ${error.message}\n\n")
                }
            }
        }
    }

    private fun onQuickFix() {
        val prompt = "Исправь ошибки в текущем коде и верни улучшенный вариант."
        input.text = prompt
        onSend()
    }

    private fun onGenerateFile() {
        val prompt = "Сгенерируй новый файл для текущего проекта с production-ready кодом и поясни куда его положить."
        input.text = prompt
        onSend()
    }

    private fun onTeach() {
        val instruction = Messages.showInputDialog(
            project,
            "Введите правило, которому ассистент должен следовать:\nПример: Всегда предлагай минимум 2 варианта рефакторинга и тесты pytest.",
            "Обучение ассистента",
            Messages.getQuestionIcon()
        ) ?: return

        backend.teachAssistant(instruction)
        append("🧠 AI обучен новому правилу: $instruction\n\n")
    }

    private fun onShowRules() {
        val rules = backend.showRules()
        if (rules.isEmpty()) {
            append("📭 Пока нет обучающих правил.\n\n")
            return
        }

        val formatted = rules.mapIndexed { index, value -> "${index + 1}) $value" }.joinToString("\n")
        append("📘 Текущие правила обучения:\n$formatted\n\n")
    }

    private fun insertToEditor(text: String) {        if (text.isBlank()) {
            append("⚠️ Нет ответа для вставки.\n\n")
            return
        }

        val editor = WindowManager.getInstance().getIdeFrame(project)?.project
            ?.let { com.intellij.openapi.fileEditor.FileEditorManager.getInstance(it).selectedTextEditor }

        if (editor == null) {
            append("⚠️ Не найден активный редактор.\n\n")
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            val caret = editor.caretModel.offset
            editor.document.insertString(caret, text)
        }
        backend.markLastAsAccepted(lastPrompt, text)
        append("✅ Ответ вставлен в редактор.\n\n")
    }

    private fun append(text: String) {
        transcript.text = transcript.text + text
        transcript.caretPosition = transcript.document.length
    }
}
