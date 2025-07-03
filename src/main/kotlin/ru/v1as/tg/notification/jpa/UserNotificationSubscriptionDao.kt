package ru.v1as.tg.notification.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.v1as.tg.notification.jpa.enitity.UserNotificationSubscriptionEntity

interface UserNotificationSubscriptionDao :
    JpaRepository<UserNotificationSubscriptionEntity, Long> {
    fun findByChatIdAndTemplateId(
        chatId: Long,
        templateId: String,
    ): List<UserNotificationSubscriptionEntity>

    fun findByUserIdAndChatIdAndTemplateId(
        userId: Long,
        chatId: Long,
        templateId: String,
    ): UserNotificationSubscriptionEntity?
}
