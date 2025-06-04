package ru.v1as.tg.notification.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import mu.KLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.v1as.tg.notification.jpa.ChatDao
import ru.v1as.tg.notification.jpa.enitity.ChatEntity
import ru.v1as.tg.notification.model.NotificationTemplateDto
import ru.v1as.tg.notification.model.TopicTemplateDto
import ru.v1as.tg.notification.service.NotificationService
import ru.v1as.tg.notification.tg.message.replySendMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest
import ru.v1as.tg.starter.update.request.RequestUpdateHandler
import ru.v1as.tg.starter.update.request.replyOnMessageRequest

val templateExample =
    ObjectMapper(YAMLFactory())
        .writer()
        .writeValueAsString(
            NotificationTemplateDto(
                "dev_built",
                listOf("🟢 Some notification {{result}}"),
                listOf(
                    TopicTemplateDto(null, "branch", ".*(TM-[0-9]+).*", ".*(TM-[0-9]+).*"),
                    TopicTemplateDto(123, prefix = "Unrecognized branch {{branch}}"),
                ),
            )
        )

@Component
class TemplateCommand(
    val tgSender: TgSender,
    val chatDao: ChatDao,
    val notificationService: NotificationService,
    val requestUpdateHandler: RequestUpdateHandler,
) : AbstractCommandHandler("template") {

    companion object : KLogging()

    private val yamlReader = ObjectMapper(YAMLFactory()).reader()

    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        val respMsg =
            command.message.replySendMessage {
                text("Reply on this message by notification template\n\n```\n$templateExample\n```")
                parseMode("Markdown")
            }
        val msg = tgSender.execute(respMsg)
        requestUpdateHandler.register(
            replyOnMessageRequest(msg, { registerTemplate(chat, it.message) })
        )
    }

    private fun registerTemplate(chat: TgChat, message: Message) {
        try {
            val chatEntity =
                chatDao.findById(chat.getId()).orElseGet {
                    chatDao.save(ChatEntity(chat.getId()))
                }!!
            val templateBody = message.text.trim()
            val templateDto =
                yamlReader.readValue(templateBody, NotificationTemplateDto::class.java)
            val template = templateDto.toModel()
            notificationService.saveTemplate(chatEntity, templateDto)
            tgSender.execute(message.replySendMessage { text("Template ${template.id} saved") })
        } catch (e: Exception) {
            tgSender.message(chat, "Error: ${e.message}")
            logger.warn("Error while template save", e)
        }
    }
}
