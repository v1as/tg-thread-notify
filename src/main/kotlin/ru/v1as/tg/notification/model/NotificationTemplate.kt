package ru.v1as.tg.notification.model

data class NotificationTemplate(
    val id: String,
    val text: List<String>,
    val sendOnUndefinedTopic: Boolean,
    val topics: List<TopicTemplate>,
    val dto: NotificationTemplateDto,
)
