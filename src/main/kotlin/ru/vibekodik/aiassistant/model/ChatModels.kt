package ru.vibekodik.aiassistant.model

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.2
)

data class ChatChoice(
    val index: Int,
    val message: ChatMessage
)

data class ChatResponse(
    val id: String,
    val choices: List<ChatChoice>
)

data class LearningRecord(
    val prompt: String,
    val response: String,
    val accepted: Boolean,
    val timestamp: Long
)
