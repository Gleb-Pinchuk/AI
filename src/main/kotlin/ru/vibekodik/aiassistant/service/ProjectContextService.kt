package ru.vibekodik.aiassistant.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ProjectContextService(private val project: Project) {
    private val maxContextChars = (System.getenv("AI_CONTEXT_MAX_CHARS")?.toIntOrNull() ?: 300)
        .coerceIn(0, 1200)

    fun collectContextSnippet(): String {
        val editors = EditorFactory.getInstance().allEditors
        val relevantEditor = editors.firstOrNull { editor ->
            val editorProject = editor.project
            editorProject != null && editorProject == project
        } ?: return "No active editor context available."

        if (maxContextChars == 0) {
            return "Context disabled by AI_CONTEXT_MAX_CHARS=0"
        }

        val docText = relevantEditor.document.text
        return if (docText.length > maxContextChars) {
            docText.take(maxContextChars)
        } else {
            docText
        }
    }
}
