package ru.v1as.tg.notification.command

import org.springframework.stereotype.Component
import ru.v1as.tg.notification.jpa.ChatDao
import ru.v1as.tg.notification.jpa.ChatTopicDao
import ru.v1as.tg.notification.jpa.enitity.ChatEntity
import ru.v1as.tg.notification.jpa.enitity.ChatTopicEntity
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest
import ru.v1as.tg.starter.update.sendMessage
import kotlin.jvm.optionals.getOrNull

@Component
class StartCommand(val tgSender: TgSender, val chatDao: ChatDao, val topicDao: ChatTopicDao) :
    AbstractCommandHandler("start") {

    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {

        var text =
            if (chat.isUserChat()) {
                "Hello, ${user.usernameOrFullName()}!"
            } else {
                var text = ""
                var chatEntity = chatDao.findById(chat.getId()).getOrNull()
                if (chatEntity != null) {
                    text += "💬 Known chat '${chatEntity.id}'"
                } else {
                    chatEntity = chatDao.save(ChatEntity(chat.getId()))
                    text += "✅ Registered chat '${chatEntity.id}'"
                }
                val topicId = command.message.messageThreadId
                if (topicId != null) {
                    var topicEntity = topicDao.findByIdAndChatId(topicId, chat.getId())
                    val topicTitle = command.message.replyToMessage?.forumTopicCreated?.name
                    if (topicEntity != null) {
                        text +=
                            "\n📑 Known topic '${topicEntity.id}' with title '${topicEntity.title}'"
                    } else if (topicTitle != null) {
                        topicEntity =
                            topicDao.save(ChatTopicEntity(topicId, chatEntity, topicTitle))
                        text +=
                            "\n✅ Registered topic '${topicEntity.id}' with title '${topicTitle}'"
                    } else {
                        text += "\n❓ Unknown topic"
                    }
                }
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
