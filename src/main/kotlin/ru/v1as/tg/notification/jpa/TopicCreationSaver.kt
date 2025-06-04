package ru.v1as.tg.notification.jpa

import mu.KLogging
import org.springframework.stereotype.Component
import ru.v1as.tg.notification.jpa.enitity.ChatTopicEntity
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.model.TgUser
import ru.v1as.tg.starter.update.handle.Handled
import ru.v1as.tg.starter.update.handle.unmatched
import ru.v1as.tg.starter.update.message.MessageRequest

@Component
class TopicCreationSaver(val chatDao: ChatDao, val topicDao: ChatTopicDao) :
    ru.v1as.tg.starter.update.message.AbstractMessageHandler() {
    companion object : KLogging()

    override fun handle(request: MessageRequest, chat: TgChat, user: TgUser): Handled {
        val newTopic = request.message.forumTopicCreated ?: return unmatched()
        val chatEntity = chatDao.findById(chat.getId()).orElse(null) ?: return unmatched()
        val savedTopic =
            topicDao.save(
                ChatTopicEntity(request.message.messageThreadId, chatEntity, newTopic.name)
            )
        logger.debug { "Topic $savedTopic saved for chat $chat" }
        return unmatched()
    }
}
