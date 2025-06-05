package ru.v1as.tg.notification.command

import org.springframework.stereotype.Component
import ru.v1as.tg.notification.jpa.ChatTopicDao
import ru.v1as.tg.notification.service.ChatService
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest
import ru.v1as.tg.starter.update.replySendMessage

@Component
class ChatTopicNotificationCommand(
    private val sender: TgSender,
    private val chatService: ChatService,
    private val topicDao: ChatTopicDao
) : AbstractCommandHandler("topic_notification") {
    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        val req = command.message
        val synced = chatService.syncChatAndTopic(req)
        val chatEntity = synced.chat
        if (chatEntity == null) {
            sender.execute(req.replySendMessage { text("Only public chat allowed") })
            return
        }
        if (req.chat?.isForum == false) {
            sender.execute(req.replySendMessage { text("Only forum chat allowed") })
            return
        }
        val topicEntity = synced.topic
        if (topicEntity == null) {
            sender.execute(req.replySendMessage { text("Unknown topic") })
            return
        }
        val enabled = !topicEntity.enabled!!
        topicEntity.enabled = enabled
        val respText = if (enabled) "🔔 Topic notification enabled" else "🔕 Topic notification disabled"
        topicDao.save(topicEntity)
        sender.execute(req.replySendMessage { text(respText) })
    }
}