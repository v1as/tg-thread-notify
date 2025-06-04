package ru.v1as.tg.notification.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.samskivert.mustache.Mustache
import mu.KLogging
import org.springframework.stereotype.Service
import ru.v1as.tg.notification.jpa.ChatDao
import ru.v1as.tg.notification.jpa.ChatTopicDao
import ru.v1as.tg.notification.jpa.NotificationTemplateDao
import ru.v1as.tg.notification.jpa.enitity.ChatEntity
import ru.v1as.tg.notification.jpa.enitity.ChatTemplateId
import ru.v1as.tg.notification.jpa.enitity.ChatTopicEntity
import ru.v1as.tg.notification.jpa.enitity.NotificationTemplateEntity
import ru.v1as.tg.notification.model.NotificationTemplate
import ru.v1as.tg.notification.model.NotificationTemplateDto
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.update.sendMessage
import kotlin.jvm.optionals.getOrNull

const val CHAT_ID_PARAM = "chat"
const val TEMPLATE_ID_PARAM = "template"

private fun error(errorText: String): Map<String, String> = mapOf("error" to errorText)

fun compileText(text: List<String>, params: Map<String, String>): String {
    return Mustache.compiler().compile(text.joinToString("\n")).execute(params)
}

@Service
class NotificationService(
    val chatDao: ChatDao,
    val topicDao: ChatTopicDao,
    val templateDao: NotificationTemplateDao,
    val sender: TgSender,
) {
    companion object : KLogging()

    private val mapper = ObjectMapper(YAMLFactory())

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
                mapper
                    .reader()
                    .readValue(templateEntity.templateYaml, NotificationTemplateDto::class.java)
                    .toModel()
            val topicEntities = topicDao.findByChatId(chatEntity.id!!)
            computeDestination(template, chatEntity.id!!, topicEntities, params)?.let {
                sender.execute(
                    sendMessage {
                        chatId(it.chatId)
                        text(compileText(template.text, params))
                        it.topicId?.let { messageThreadId(it) }
                    }
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error while notification processing $params" }
            return error(e.message ?: "Unexpected error")
        }
        return mapOf("status" to "OK")
    }

    fun saveTemplate(chat: ChatEntity, templateDto: NotificationTemplateDto): List<String> {
        val template =
            try {
                templateDto.toModel()
            } catch (e: Exception) {
                return listOf(e.message ?: "Unexpected error")
            }
        val templateBody = mapper.writer().writeValueAsString(templateDto)
        templateDao.save(NotificationTemplateEntity(template.id, chat, templateBody))
        return emptyList()
    }
}

data class TgDestination(val chatId: Long, val topicId: Int?, val prefix: String?)

fun computeDestination(
    template: NotificationTemplate,
    chatId: Long,
    topicEntities: List<ChatTopicEntity>,
    params: Map<String, String>,
): TgDestination? {
    for (template in template.topics) {
        if (template.topicId != null) {
            return TgDestination(chatId, template.topicId, template.prefix)
        }
        val matchParamName = template.matchParamName ?: continue
        val matchId =
            matchParamName
                .let { params[it] }
                ?.let { template.matchParamRegexp?.matcher(it) }
                ?.takeIf { it.matches() }
                ?.group(1)
        if (matchId == null || template.matchTopicTitleRegexp == null) {
            continue
        }

        for (topicEntity in topicEntities) {
            topicEntity.title
                ?.let { template.matchTopicTitleRegexp.matcher(it) }
                ?.takeIf { it.matches() }
                ?.group(1)
                ?.takeIf { matchId == it }
                ?.let {
                    return TgDestination(chatId, topicEntity.id, template.prefix)
                }
        }
    }
    return null
}
