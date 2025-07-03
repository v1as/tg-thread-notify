package ru.v1as.tg.notification.jpa.enitity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["userId", "chatId", "templateId"])])
data class UserNotificationSubscriptionEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    var userId: Long? = null,
    var chatId: Long? = null,
    var templateId: String? = null,
    var regexp: String? = null,
)
