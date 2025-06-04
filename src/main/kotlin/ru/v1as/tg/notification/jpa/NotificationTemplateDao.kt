package ru.v1as.tg.notification.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.v1as.tg.notification.jpa.enitity.ChatTemplateId
import ru.v1as.tg.notification.jpa.enitity.NotificationTemplateEntity

interface NotificationTemplateDao : JpaRepository<NotificationTemplateEntity, ChatTemplateId>
