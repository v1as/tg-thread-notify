package ru.v1as.tg.notification.command

import mu.KLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode.MARKDOWN
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.v1as.tg.notification.jpa.NotificationTemplateDao
import ru.v1as.tg.notification.jpa.UserNotificationSubscriptionDao
import ru.v1as.tg.notification.jpa.enitity.ChatTemplateId
import ru.v1as.tg.notification.jpa.enitity.UserNotificationSubscriptionEntity
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest
import ru.v1as.tg.starter.update.replySendMessage
import ru.v1as.tg.starter.update.request.RequestUpdateHandler
import ru.v1as.tg.starter.update.request.replyOnMessageRequest
import ru.v1as.tg.starter.update.sendMessage
import kotlin.jvm.optionals.getOrNull

@Component
class SubscribeCommand(
    val tgSender: TgSender,
    val templateDao: NotificationTemplateDao,
    val subscriptionDao: UserNotificationSubscriptionDao,
    val requestUpdateHandler: RequestUpdateHandler,
) : AbstractCommandHandler("subscribe") {

    companion object : KLogging()

    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        if (!chat.isUserChat()) {
            command.message
                .replySendMessage { text("Команда доступна только в приватных чатах") }
                .let { tgSender.execute(it) }
            return
        }

        val respMsg =
            command.message.replySendMessage {
                parseMode(MARKDOWN)
                text(
                    """
                📝 Ответьте на это сообщение в формате:
                
                ```
                chat_id
                template_id
                regexp_pattern
                ```"""
                        .trimIndent()
                )
            }
        val msg = tgSender.execute(respMsg)
        requestUpdateHandler.register(
            replyOnMessageRequest(msg, { registerSubscription(user, it.message) })
        )
    }

    private fun registerSubscription(user: TgUserWrapper, message: Message) {
        try {
            val lines = message.text.trim().lines()
            if (lines.size != 3) {
                tgSender.execute(
                    message.replySendMessage {
                        text("❌ Неверный формат, нужны 3 строки: chat_id, template_id, regexp")
                    }
                )
                return
            }

            val chatId = lines[0].toLongOrNull()
            val templateId = lines[1].trim()
            val regexp = lines[2].trim()

            if (chatId == null) {
                tgSender.execute(
                    message.replySendMessage { text("❌ Неверный chat_id: ${lines[0]}") }
                )
                return
            }

            // Проверяем существование шаблона
            val templateEntity =
                templateDao.findById(ChatTemplateId(templateId, chatId)).getOrNull()
            if (templateEntity == null) {
                tgSender.execute(
                    message.replySendMessage {
                        text("❌ Шаблон '$templateId' не найден для чата $chatId")
                    }
                )
                return
            }

            // Проверяем валидность regexp
            try {
                regexp.toRegex()
            } catch (e: Exception) {
                tgSender.execute(
                    message.replySendMessage {
                        text("❌ Неверное регулярное выражение: ${e.message}")
                    }
                )
                return
            }

            // Сохраняем подписку
            val userId = user.id()
            val subscription =
                subscriptionDao.findByUserIdAndChatIdAndTemplateId(userId, chatId, templateId)
                    ?: UserNotificationSubscriptionEntity(
                        userId = userId,
                        chatId = chatId,
                        templateId = templateId
                    )
            val firstTime = subscription.id == null
            subscription.regexp = regexp
            subscriptionDao.save(subscription)

            tgSender.executeAsync(message.replySendMessage { text("✅ Подписка успешно создана") })
            if (firstTime) {
                tgSender.executeAsync(sendMessage {
                    chatId(chatId)
                    text("👀 ${user.usernameOrFullName()} подписался на уведомления шаблона $templateId")
                })
            }

            logger.info(
                "User $userId subscribed to chat $chatId, template $templateId with regexp: $regexp"
            )
        } catch (e: Exception) {
            tgSender.execute(
                message.replySendMessage { text("❌ Ошибка при создании подписки: ${e.message}") }
            )
            logger.error("Error while creating subscription", e)
        }
    }
}
