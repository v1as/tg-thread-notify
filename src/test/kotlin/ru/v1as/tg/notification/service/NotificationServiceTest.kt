package ru.v1as.tg.notification.service

import org.assertj.core.api.Assertions.assertThat
import ru.v1as.tg.notification.jpa.enitity.ChatTopicEntity
import ru.v1as.tg.notification.model.NotificationTemplateDto
import ru.v1as.tg.notification.model.TopicTemplateDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationServiceTest {
    @Test
    fun shouldCompile() {
        val compiled =
            compileText(
                listOf("Hi, {{username}}", "It's {{day}}!"),
                mapOf("username" to "Bob", "day" to "wednesday"),
            )
        assertThat(compiled).contains("Hi, Bob", "It's wednesday!")
    }

    @Test
    fun shouldMatchTemplate() {
        val template =
            template(TopicTemplateDto(null, "branch", ".*(TM-[0-9]+).*", "^(TM-[0-9]+).*"))

        val destination =
            computeDestination(
                template,
                1,
                listOf(
                    ChatTopicEntity(id = 12, title = "TM-123 Some description"),
                    ChatTopicEntity(id = 13, title = "TM-124 Some description"),
                ),
                mapOf("branch" to "bugfix/TM-124-some-text"),
            )
        assertEquals(TgDestination(1, 13), destination)
    }

    @Test
    fun shouldMatchTemplateOnFixedTopic() {
        val template =
            template(
                TopicTemplateDto(null, "branch", ".*(TM-[0-9]+).*", "^(TM-[0-9]+).*"),
                TopicTemplateDto(20),
            )

        val destination =
            computeDestination(
                template,
                1,
                listOf(
                    ChatTopicEntity(id = 12, title = "TM-123 Some description"),
                    ChatTopicEntity(id = 13, title = "TM-124 Some description"),
                ),
                mapOf("branch" to "bugfix/TM-128-some-text"),
            )
        assertEquals(TgDestination(1, 20), destination)
    }

    @Test
    fun shouldNotMatchTemplate() {
        val template =
            template(TopicTemplateDto(null, "branch", ".*(TM-[0-9]+).*", "^(TM-[0-9]+).*"))

        val destination =
            computeDestination(
                template,
                1,
                listOf(
                    ChatTopicEntity(id = 12, title = "TM-123 Some description"),
                    ChatTopicEntity(id = 13, title = "TM-124 Some description"),
                ),
                mapOf("branch" to "bugfix/TM-128-some-text"),
            )
        assertNull(destination)
    }
}

private fun template(vararg templates: TopicTemplateDto) =
    NotificationTemplateDto("id", listOf("text"), templates.toList()).toModel()
