package ru.v1as.tg.notification.jpa.enitity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.ManyToOne
import java.io.Serializable

@Entity
@IdClass(ChatTopicId::class)
data class ChatTopicEntity(
    @Id var id: Int? = null,
    @Id @ManyToOne var chat: ChatEntity? = null,
    var title: String? = null,
)

data class ChatTopicId(val id: Int = 0, val chat: Long = 0) : Serializable
