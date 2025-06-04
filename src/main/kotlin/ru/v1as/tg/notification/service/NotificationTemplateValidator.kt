package ru.v1as.tg.notification.service

import ru.v1as.tg.notification.model.NotificationTemplate

class NotificationTemplateValidator {
    fun validate(template: NotificationTemplate): List<String> {
        val errors = mutableListOf<String>()
        if (template.id.isBlank()) {
            errors.add("Template id is blank")
        }
        for (topic in template.topics) {
            if (topic.topicId != null && topic.matchParamName != null) {
                errors.add("Topic template contain both topicId and matchParamName")
            }
            if (topic.matchParamName != null) {}
        }
        return errors
    }
}
