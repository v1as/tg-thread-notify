package ru.v1as.tg.notification.model

import java.util.regex.Pattern

class TopicTemplate(
    val topicId: Int?,
    val matchParamName: Pattern?,
    val matchParamRegexp: Pattern?,
    val matchTopicTitleRegexp: Pattern?,
    val matchTopicFirstMessageRegexp: Pattern?,
    val dto: TopicTemplateDto,
)
