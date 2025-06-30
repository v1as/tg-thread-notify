package ru.v1as.tg.notification.model

data class NotificationTemplateDto(
    var id: String? = null,
    var text: List<String> = emptyList(),
    var topics: List<TopicTemplateDto> = emptyList(),
    var html: Boolean = false,
    var sendOnUndefinedTopic: Boolean = false
) {
    fun toModel(): NotificationTemplate {
        val topicModel = topics.map { topic -> topic.toModel() }
        return NotificationTemplate(
            id ?: throw IllegalArgumentException("Topic id cannot be null"),
            text,
            sendOnUndefinedTopic,
            html,
            topicModel,
            this,
        )
    }
}
