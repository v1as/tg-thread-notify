package ru.v1as.tg.notification.service

import ru.v1as.tg.notification.jpa.enitity.ChatEntity
import ru.v1as.tg.notification.jpa.enitity.ChatTopicEntity

data class ChatSync(val message: String, val chat: ChatEntity? = null, val topic: ChatTopicEntity? = null)