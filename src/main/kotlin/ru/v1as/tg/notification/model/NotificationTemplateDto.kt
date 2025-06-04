package ru.v1as.tg.notification.model

data class NotificationTemplateDto(
    var id: String? = null,
    var text: List<String> = emptyList(),
    var format: TextFormat = TextFormat.TEXT,
    var sendOnUndefinedTopic: Boolean = false,
    var warnInText: Boolean = true,
    var topics: List<TopicTemplateDto> = emptyList(),
) {
    fun toModel(): NotificationTemplate {
        val topicModel = topics.map { topic -> topic.toModel() }
        return NotificationTemplate(
            id ?: throw IllegalArgumentException("Topic id cannot be null"),
            text,
            format,
            sendOnUndefinedTopic,
            warnInText,
            topicModel,
        )
    }
}
