package ru.v1as.tg.notification.jpa.enitity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.ManyToOne
import java.io.Serializable

@Entity
@IdClass(ChatTemplateId::class)
data class NotificationTemplateEntity(
    @Id var id: String? = null,
    @Id @ManyToOne var chat: ChatEntity? = null,
    @Column(columnDefinition = "TEXT") var templateYaml: String? = null,
)

data class ChatTemplateId(val id: String = "", val chat: Long = 0) : Serializable
