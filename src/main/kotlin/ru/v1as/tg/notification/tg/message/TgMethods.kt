package ru.v1as.tg.notification.tg.message

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.message.Message

fun Message.replySendMessage(block: SendMessage.SendMessageBuilder<*, *>.() -> Unit): SendMessage {
    val srcMsg = this
    return SendMessage.builder()
        .apply {
            srcMsg.replyToMessage?.messageThreadId?.let { messageThreadId(it) }
            chatId(srcMsg.chatId.toString())
            this.apply(block)
        }
        .build()
}
