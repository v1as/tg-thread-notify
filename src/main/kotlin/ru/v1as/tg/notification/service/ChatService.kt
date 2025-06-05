package ru.v1as.tg.notification.service

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.v1as.tg.notification.jpa.ChatDao
import ru.v1as.tg.notification.jpa.ChatTopicDao
import ru.v1as.tg.notification.jpa.enitity.ChatEntity
import ru.v1as.tg.notification.jpa.enitity.ChatTopicEntity
import ru.v1as.tg.starter.model.base.TgUserWrapper
import kotlin.jvm.optionals.getOrNull

@Service
class ChatService(val chatDao: ChatDao, val topicDao: ChatTopicDao) {

    fun syncChatAndTopic(message: Message): ChatSync {
        val chat = message.chat
        val user = TgUserWrapper(message.from)

        return if (chat.isUserChat) {
            ChatSync("Hello, ${user.usernameOrFullName()}!")
        } else {
            var text = ""
            var chatEntity = chatDao.findById(chat.id).getOrNull()
            var topicEntity: ChatTopicEntity? = null
            if (chatEntity != null) {
                text += "💬 Known chat '${chatEntity.id}'"
            } else {
                chatEntity = chatDao.save(ChatEntity(chat.id))
                text += "✅ Registered chat '${chatEntity.id}'"
            }
            val topicId = message.messageThreadId
            if (topicId != null) {
                topicEntity = topicDao.findByIdAndChatId(topicId, chat.id)
                val topicTitle = message.replyToMessage?.forumTopicCreated?.name
                if (topicEntity != null) {
                    text += "\n📑 Known topic '${topicEntity.id}' with title '${topicEntity.title}'"
                } else if (topicTitle != null) {
                    topicEntity = topicDao.save(ChatTopicEntity(topicId, chatEntity, topicTitle))
                    text += "\n✅ Registered topic '${topicEntity.id}' with title '${topicTitle}'"
                } else {
                    text += "\n❓ Unknown topic"
                }
            }
            ChatSync(text, chatEntity, topicEntity)
        }
    }
}