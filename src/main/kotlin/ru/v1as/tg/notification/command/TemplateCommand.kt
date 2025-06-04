package ru.v1as.tg.notification.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import mu.KLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.v1as.tg.notification.jpa.ChatDao
import ru.v1as.tg.notification.jpa.NotificationTemplateDao
import ru.v1as.tg.notification.jpa.enitity.ChatEntity
import ru.v1as.tg.notification.jpa.enitity.NotificationTemplateEntity
import ru.v1as.tg.notification.model.NotificationTemplateDto
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest
import ru.v1as.tg.starter.update.request.RequestUpdateHandler
import ru.v1as.tg.starter.update.request.replyOnMessageRequest

@Component
class TemplateCommand(
    val tgSender: TgSender,
    val chatDao: ChatDao,
    val templateDao: NotificationTemplateDao,
    val requestUpdateHandler: RequestUpdateHandler,
) : AbstractCommandHandler("template") {

    companion object : KLogging()

    private val yamlReader = ObjectMapper(YAMLFactory()).reader()

    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        val msg =
            tgSender.execute(
                SendMessage(chat.idStr(), "Reply on this message by notification template")
            )
        requestUpdateHandler.register(
            replyOnMessageRequest(msg, { registerTemplate(chat, it.message) })
        )
    }

    private fun registerTemplate(chat: TgChat, message: Message) {
        try {
            val chatEntity =
                chatDao.findById(chat.getId()).orElseGet { chatDao.save(ChatEntity(chat.getId())) }
            val templateBody = message.text.trim()
            val templateDto =
                yamlReader.readValue(templateBody, NotificationTemplateDto::class.java)
            val template = templateDto.toModel()
            templateDao.save(NotificationTemplateEntity(template.id, chatEntity, templateBody))
            tgSender.message(chat, "Template ${template.id} saved")
        } catch (e: Exception) {
            tgSender.message(chat, "Error: ${e.message}")
            logger.warn("Error while template save", e)
        }
    }
}
