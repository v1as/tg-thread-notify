package ru.v1as.tg.notification.model

data class NotificationTemplate(
    val id: String,
    val text: List<String>,
    val format: TextFormat,
    val sendOnUndefinedTopic: Boolean,
    val warnInText: Boolean,
    val topics: List<TopicTemplate>,
)
