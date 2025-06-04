package ru.v1as.tg.notification.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.v1as.tg.notification.jpa.enitity.ChatTopicEntity
import ru.v1as.tg.notification.jpa.enitity.ChatTopicId

interface ChatTopicDao : JpaRepository<ChatTopicEntity, ChatTopicId> {
    fun findByChatId(chatId: Long): List<ChatTopicEntity>

    fun findByIdAndChatId(topicId: Int, chatId: Long): ChatTopicEntity?
}
