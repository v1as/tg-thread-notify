package ru.v1as.tg.notification.jpa.enitity

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity data class ChatEntity(@Id var id: Long? = null, var enabled: Boolean = true)
