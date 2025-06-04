package ru.v1as.tg.notification.model

import java.util.regex.Pattern

data class TopicTemplateDto(
    var topicId: Int? = null,
    var matchParamName: String? = null,
    var matchParamRegexp: String? = null,
    var matchTopicTitleRegexp: String? = null,
    var prefix: String? = null,
) {
    fun toModel(): TopicTemplate {
        return TopicTemplate(
            topicId,
            matchParamName,
            matchParamRegexp?.let { Pattern.compile(it) },
            matchTopicTitleRegexp?.let { Pattern.compile(it) },
            prefix,
        )
    }
}
