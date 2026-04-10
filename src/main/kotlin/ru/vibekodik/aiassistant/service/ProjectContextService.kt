package ru.vibekodik.aiassistant.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ProjectContextService(private val project: Project) {
    fun collectContextSnippet(): String {
        val editors = EditorFactory.getInstance().allEditors
        val relevantEditor = editors.firstOrNull { editor ->
            val editorProject = editor.project
            editorProject != null && editorProject == project
        } ?: return "No active editor context available."

        val docText = relevantEditor.document.text
        return if (docText.length > 2000) {
            docText.take(2000)
        } else {
            docText
        }
    }
}
