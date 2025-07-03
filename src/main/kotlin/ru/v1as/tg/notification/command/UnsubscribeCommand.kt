package ru.v1as.tg.notification.command

import mu.KLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.meta.api.methods.ParseMode.MARKDOWN
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.v1as.tg.notification.jpa.UserNotificationSubscriptionDao
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest
import ru.v1as.tg.starter.update.replySendMessage
import ru.v1as.tg.starter.update.request.RequestUpdateHandler
import ru.v1as.tg.starter.update.request.replyOnMessageRequest

@Component
class UnsubscribeCommand(
    val tgSender: TgSender,
    val subscriptionDao: UserNotificationSubscriptionDao,
    val requestUpdateHandler: RequestUpdateHandler,
) : AbstractCommandHandler("unsubscribe") {

    companion object : KLogging()

    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        if (!chat.isUserChat()) {
            command.message
                .replySendMessage { text("Команда доступна только в приватных чатах") }
                .let { tgSender.execute(it) }
            return
        }

        val userId = user.id()
        val subscriptions = subscriptionDao.findByUserId(userId)

        if (subscriptions.isEmpty()) {
            command.message
                .replySendMessage { text("❌ У вас нет активных подписок") }
                .let { tgSender.execute(it) }
            return
        }

        val subscriptionsList = subscriptions.map { "${it.chatId}:${it.templateId}" }

        val respMsg = command.message.replySendMessage {
            parseMode(MARKDOWN)
            text(
                """
                |📋 Ваши подписки:
                |
                |```
                |${subscriptionsList.joinToString("\n|")}
                |```
                |
                |📝 Ответьте на это сообщение:
                |`chatId:templateId` - для отписки от конкретной подписки
                |`ALL` - для отписки от всех подписок
            """.trimMargin()
            )
        }
        val msg = tgSender.execute(respMsg)
        requestUpdateHandler.register(
            replyOnMessageRequest(msg, { processUnsubscribe(user, it.message) })
        )
    }

    @Transactional
    fun processUnsubscribe(user: TgUserWrapper, message: Message) {
        try {
            val input = message.text.trim()
            val userId = user.id()

            if (input.equals("ALL", ignoreCase = true)) {
                val count = subscriptionDao.findByUserId(userId).size
                subscriptionDao.deleteByUserId(userId)

                tgSender.execute(
                    message.replySendMessage {
                        text("✅ Успешно удалено подписок: $count")
                    }
                )
                logger.info("User $userId unsubscribed from all subscriptions ($count total)")
                return
            }

            val parts = input.split(":")
            if (parts.size != 2) {
                tgSender.execute(
                    message.replySendMessage {
                        text("❌ Неверный формат! Используйте 'chatId:templateId' или 'ALL'")
                    }
                )
                return
            }

            val chatId = parts[0].toLongOrNull()
            val templateId = parts[1].trim()

            if (chatId == null) {
                tgSender.execute(
                    message.replySendMessage {
                        text("❌ Неверный chat_id: ${parts[0]}")
                    }
                )
                return
            }

            // Проверяем существование подписки
            val subscription = subscriptionDao.findByUserIdAndChatIdAndTemplateId(userId, chatId, templateId)
            if (subscription == null) {
                tgSender.execute(
                    message.replySendMessage {
                        text("❌ Подписка на чат $chatId и шаблон '$templateId' не найдена")
                    }
                )
                return
            }

            // Удаляем подписку
            subscriptionDao.deleteByUserIdAndChatIdAndTemplateId(userId, chatId, templateId)

            tgSender.execute(
                message.replySendMessage {
                    text("✅ Подписка успешно удалена: $chatId:$templateId")
                }
            )

            logger.info("User $userId unsubscribed from chat $chatId, template $templateId")
        } catch (e: Exception) {
            tgSender.execute(
                message.replySendMessage { text("❌ Ошибка при удалении подписки: ${e.message}") }
            )
            logger.error("Error while processing unsubscribe", e)
        }
    }
}