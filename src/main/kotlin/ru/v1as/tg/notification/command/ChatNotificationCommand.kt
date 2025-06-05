package ru.v1as.tg.notification.command

import org.springframework.stereotype.Component
import ru.v1as.tg.notification.jpa.ChatDao
import ru.v1as.tg.notification.service.ChatService
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest
import ru.v1as.tg.starter.update.replySendMessage

@Component
class ChatNotificationCommand(
    private val sender: TgSender,
    private val chatService: ChatService,
    private val chatDao: ChatDao
) :
    AbstractCommandHandler("chat_notification") {
    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        val req = command.message
        val synced = chatService.syncChatAndTopic(req)
        val chatEntity = synced.chat
        if (chatEntity == null) {
            sender.execute(req.replySendMessage { text("Only public chat allowed") })
            return
        }
        val enabled = !chatEntity.enabled
        chatEntity.enabled = enabled
        val respText = if (enabled) "🔔 Chat notification enabled" else "🔕 Chat notification disabled"
        chatDao.save(chatEntity)
        sender.execute(req.replySendMessage { text(respText) })
    }
}