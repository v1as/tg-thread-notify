package ru.v1as.tg.notification.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import mu.KLogging
import org.springframework.stereotype.Service
import ru.v1as.tg.notification.jpa.ChatDao
import ru.v1as.tg.notification.jpa.ChatTopicDao
import ru.v1as.tg.notification.jpa.NotificationTemplateDao
import ru.v1as.tg.notification.jpa.enitity.ChatTemplateId
import ru.v1as.tg.notification.jpa.enitity.ChatTopicId
import ru.v1as.tg.notification.model.NotificationTemplateDto
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.update.sendMessage
import kotlin.jvm.optionals.getOrNull

const val CHAT_ID_PARAM = "chat"
const val TEMPLATE_ID_PARAM = "template"

@Service
class NotificationService(
    val chatDao: ChatDao,
    val topicDao: ChatTopicDao,
    val templateDao: NotificationTemplateDao,
    val sender: TgSender,
) {
    companion object : KLogging()

    private val yamlReader = ObjectMapper(YAMLFactory()).reader()

    fun notify(params: Map<String, String>): Map<String, String> {
        val chatParam = params[CHAT_ID_PARAM]
        val chatEntity =
            chatParam?.toLongOrNull()?.let { chatDao.findById(it) }?.getOrNull()
                ?: return error("Wrong chat param: $chatParam")
        val templateId = params[TEMPLATE_ID_PARAM]
        val templateEntity =
            templateId?.let {
                templateDao.findById(ChatTemplateId(it, chatEntity.id!!)).getOrNull()
            } ?: return error("No template found: $templateId")

        try {
            val template =
                yamlReader
                    .readValue(templateEntity.templateYaml, NotificationTemplateDto::class.java)
                    .toModel()
            val topicId = template.topics.first().topicId
            topicDao.findById(ChatTopicId(topicId!!, chatEntity.id!!))
            sender.execute(
                sendMessage {
                    chatId(chatEntity.id!!)
                    text(template.text.joinToString("\n"))
                    messageThreadId(topicId)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error while notification processing $params" }
            return error(e.message ?: "Unexpected error")
        }
        return mapOf("status" to "OK")
    }

    private fun error(errorText: String): Map<String, String> = mapOf("error" to errorText)
}
