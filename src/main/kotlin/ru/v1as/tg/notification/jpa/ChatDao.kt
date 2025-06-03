package ru.v1as.tg.notification.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.v1as.tg.notification.jpa.enitity.ChatEntity

interface ChatDao : JpaRepository<ChatEntity, Long>
