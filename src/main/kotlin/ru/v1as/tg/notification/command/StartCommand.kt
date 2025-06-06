package ru.v1as.tg.notification.command

import org.springframework.stereotype.Component
import ru.v1as.tg.notification.service.ChatService
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest
import ru.v1as.tg.starter.update.replySendMessage

@Component
class StartCommand(val tgSender: TgSender, val chatService: ChatService) :
    AbstractCommandHandler("start") {

    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        val synced = chatService.syncChatAndTopic(command.message)

        command.message.replySendMessage { text(synced.message) }.let { tgSender.execute(it) }
    }
}
