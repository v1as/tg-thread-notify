package ru.v1as.tg.notification.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.samskivert.mustache.Mustache
import kotlin.jvm.optionals.getOrNull
import mu.KLogging
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.ParseMode
import ru.v1as.tg.notification.jpa.ChatDao
import ru.v1as.tg.notification.jpa.ChatTopicDao
import ru.v1as.tg.notification.jpa.NotificationTemplateDao
import ru.v1as.tg.notification.jpa.UserNotificationSubscriptionDao
import ru.v1as.tg.notification.jpa.enitity.ChatEntity
import ru.v1as.tg.notification.jpa.enitity.ChatTemplateId
import ru.v1as.tg.notification.jpa.enitity.ChatTopicEntity
import ru.v1as.tg.notification.jpa.enitity.NotificationTemplateEntity
import ru.v1as.tg.notification.model.NotificationTemplate
import ru.v1as.tg.notification.model.NotificationTemplateDto
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.update.sendMessage

const val CHAT_ID_PARAM = "chat"
const val TEMPLATE_ID_PARAM = "template"

private fun resp(message: String, status: String): Map<String, String> =
    mapOf("message" to message, "status" to status)

fun compileText(
    text: List<String>,
    params: Map<String, String>,
    escapeHtml: Boolean = true,
): String {
    return Mustache.compiler()
        .defaultValue("")
        .escapeHTML(escapeHtml)
        .compile(text.joinToString("\n"))
        .execute(params)
        .trimIndent()
}

@Service
class NotificationService(
    val chatDao: ChatDao,
    val topicDao: ChatTopicDao,
    val templateDao: NotificationTemplateDao,
    val subscriptionDao: UserNotificationSubscriptionDao,
    val sender: TgSender,
) {
    companion object : KLogging()

    private val mapper = ObjectMapper(YAMLFactory())

    fun notify(params: Map<String, String>, silent: Boolean = false): Map<String, String> {
        val chatParam = params[CHAT_ID_PARAM]
        val chatEntity =
            chatParam?.toLongOrNull()?.let { chatDao.findById(it) }?.getOrNull()
                ?: return resp("Wrong chat param: $chatParam", "400")
        val templateId = params[TEMPLATE_ID_PARAM]
        val templateEntity =
            templateId?.let {
                templateDao.findById(ChatTemplateId(it, chatEntity.id!!)).getOrNull()
            } ?: return resp("No template found: $templateId", "400")

        if (!chatEntity.enabled) {
            return resp("Chat notification disabled", "200")
        }

        try {
            val template =
                mapper
                    .reader()
                    .readValue(templateEntity.templateYaml, NotificationTemplateDto::class.java)
                    .toModel()
            val subscriptionResp =
                try {
                    handleSubscriptionNotification(chatEntity.id!!, template, params)
                } catch (e: Exception) {
                    logger.error("Error while subscription processing ", e)
                    resp(e.message ?: "Unexpected error", "500")
                }
            if (silent) {
                return subscriptionResp
            }
            val topicEntities = topicDao.findByChatId(chatEntity.id!!)
            var destination = computeDestination(template, chatEntity.id!!, topicEntities, params)
            if (destination?.topic?.enabled == false) {
                return resp("Chat topic notification disabled", "200")
            }
            destination =
                destination
                    ?: template.sendOnUndefinedTopic
                        .takeIf { it }
                        ?.let { TgDestination(chatEntity.id!!, null, "Undefined topic") }

            return destination?.let {
                val text = addPrefix(it.prefix, template.text)
                val message =
                    sender.execute(
                        sendMessage {
                            chatId(it.chatId)
                            text(compileText(text, params, template.html))
                            it.topic?.let { messageThreadId(it.id) }
                            if (template.html) {
                                parseMode(ParseMode.HTML)
                            }
                        }
                    )
                resp("Message sent: ${message?.messageId}", "200")
            } ?: resp("Message was not sent", "200")
        } catch (e: Exception) {
            logger.error(e) { "Error while notification processing $params" }
            return resp(e.message ?: "Unexpected error", "500")
        }
    }

    private fun addPrefix(prefix: String?, texts: List<String>): List<String> =
        prefix?.takeIf { it.isNotBlank() }?.let { texts.toMutableList().apply { addFirst(it) } }
            ?: texts

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

    private fun handleSubscriptionNotification(
        chatId: Long,
        template: NotificationTemplate,
        params: Map<String, String>,
    ): Map<String, String> {
        val subscriptions = subscriptionDao.findByChatIdAndTemplateId(chatId, template.id)

        if (subscriptions.isEmpty()) {
            return resp(
                "No subscriptions found for chat $chatId and template ${template.id}",
                "200",
            )
        }
        val compiledText = compileText(template.text, params, template.html)
        var sentCount = 0

        for (subscription in subscriptions) {
            try {
                val regex = subscription.regexp!!.toRegex()

                if (regex.containsMatchIn(compiledText)) {
                    sender.execute(
                        sendMessage {
                            chatId(subscription.userId!!)
                            text(compiledText)
                            if (template.html) {
                                parseMode(ParseMode.HTML)
                            }
                        }
                    )

                    sentCount++
                    logger.info(
                        "Notification sent to user ${subscription.userId} for chat $chatId, template ${template.id}"
                    )
                }
            } catch (e: Exception) {
                logger.error("Error sending silent notification to user ${subscription.userId}", e)
            }
        }

        return resp("Silent notifications sent to $sentCount users", "200")
    }
}

data class TgDestination(val chatId: Long, val topic: ChatTopicEntity?, val prefix: String? = null)

fun computeDestination(
    template: NotificationTemplate,
    chatId: Long,
    topicEntities: List<ChatTopicEntity>,
    params: Map<String, String>,
): TgDestination? {
    val id2TopicEntity = topicEntities.groupBy { it.id }
    if (template.topics.isEmpty()) {
        return TgDestination(chatId, null, null)
    }
    for (templateItem in template.topics) {
        val fixedTopicEntity =
            templateItem.topicId?.let {
                id2TopicEntity[it] ?: throw IllegalStateException("Unknown topicId $it")
            }
        if (
            fixedTopicEntity != null &&
            templateItem.matchParamRegexp == null &&
            templateItem.matchTopicTitleRegexp == null
        ) {
            return TgDestination(chatId, fixedTopicEntity.first(), templateItem.prefix)
        }

        val matchParamName = templateItem.matchParamName ?: continue
        val paramMatcher = matchParamName
            .let { params[it] }
            ?.let { templateItem.matchParamRegexp?.matcher(it) }
        val paramMatched = paramMatcher?.matches() ?: false



        if (paramMatched && fixedTopicEntity != null) {
            return TgDestination(chatId, fixedTopicEntity.first(), templateItem.prefix)
        }
        if (!paramMatched || templateItem.matchTopicTitleRegexp == null) {
            continue
        }

        val matchId = paramMatcher?.group(1)
        for (topicEntity in topicEntities) {
            topicEntity.title
                ?.let { templateItem.matchTopicTitleRegexp.matcher(it) }
                ?.takeIf { it.matches() }
                ?.group(1)
                ?.takeIf { matchId == it }
                ?.let {
                    return TgDestination(chatId, topicEntity, templateItem.prefix)
                }
        }
    }
    return null
}
