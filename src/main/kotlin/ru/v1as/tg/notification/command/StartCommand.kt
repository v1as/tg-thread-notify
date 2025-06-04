package ru.v1as.tg.notification.command

import org.springframework.stereotype.Component
import ru.v1as.tg.notification.jpa.ChatDao
import ru.v1as.tg.notification.jpa.enitity.ChatEntity
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest
import ru.v1as.tg.starter.update.sendMessage

@Component
class StartCommand(val tgSender: TgSender, val chatDao: ChatDao) : AbstractCommandHandler("start") {

    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {

        var text =
            if (chat.isUserChat()) {
                "Hello, ${user.usernameOrFullName()}!"
            } else {
                var text = "Chat with id=${chat.getId()} registered"
                command.message.messageThreadId?.let { text += "\nTopic with id=$it registered" }
                chatDao.findById(chat.getId()).orElseGet { chatDao.save(ChatEntity(chat.getId())) }
                text
            }
        tgSender.execute(
            sendMessage {
                chatId(chat.getId())
                text(text)
                command.message.messageThreadId?.let { messageThreadId(it) }
            }
        )
    }
}
