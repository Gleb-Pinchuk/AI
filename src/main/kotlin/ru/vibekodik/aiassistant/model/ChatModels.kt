package ru.vibekodik.aiassistant.model

data class ChatMessage(
    val role: String,
    val content: String
)

data class OllamaOptions(
    val temperature: Double = 0.2,
    val num_predict: Int = 1024
)

data class OllamaChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val options: OllamaOptions = OllamaOptions()
)

data class OllamaChatResponse(
    val model: String?,
    val message: ChatMessage?,
    val done: Boolean?
)
data class LearningRecord(
    val prompt: String,
    val response: String,
    val accepted: Boolean,
    val timestamp: Long
)
