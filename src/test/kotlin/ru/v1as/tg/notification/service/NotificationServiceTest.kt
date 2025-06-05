package ru.v1as.tg.notification.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import ru.v1as.tg.notification.jpa.enitity.ChatTopicEntity
import ru.v1as.tg.notification.model.NotificationTemplateDto
import ru.v1as.tg.notification.model.TopicTemplateDto

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
    fun shouldCompileWithUndefinedParam() {
        val compiled =
            compileText(listOf("Hi, {{username}}", "It's {{day}}!"), mapOf("username" to "Bob"))
        assertThat(compiled).contains("Hi, Bob", "It's !")
    }

    @Test
    fun shouldMatchTemplate() {
        val template =
            template(TopicTemplateDto(null, "branch", ".*(TM-[0-9]+).*", "^(TM-[0-9]+).*"))

        val expectedTopic = ChatTopicEntity(id = 13, title = "TM-124 Some description")
        val destination =
            computeDestination(
                template,
                1,
                listOf(ChatTopicEntity(id = 12, title = "TM-123 Some description"), expectedTopic),
                mapOf("branch" to "bugfix/TM-124-some-text"),
            )
        assertEquals(TgDestination(1, expectedTopic), destination)
    }

    @Test
    fun shouldMatchOnFixedTopic() {
        val template =
            template(
                TopicTemplateDto(null, "branch", ".*(TM-[0-9]+).*", "^(TM-[0-9]+).*"),
                TopicTemplateDto(13),
            )

        val expectedTopic = ChatTopicEntity(id = 13, title = "Any text")
        val topicEntities =
            listOf(ChatTopicEntity(id = 12, title = "TM-123 Some description"), expectedTopic)
        val params = mapOf("branch" to "any value")
        val destination = computeDestination(template, 1, topicEntities, params)
        assertEquals(TgDestination(1, expectedTopic), destination)
    }

    @Test
    fun shouldMatchOnFixedTopicByParam() {
        val template =
            template(
                TopicTemplateDto(null, "branch", ".*(TM-[0-9]+).*", "^(TM-[0-9]+).*"),
                TopicTemplateDto(13, "branch", ".*(dev).*"),
            )

        val expectedTopic = ChatTopicEntity(id = 13, title = "Any text")
        val topicEntities =
            listOf(ChatTopicEntity(id = 12, title = "TM-123 Some description"), expectedTopic)
        val params = mapOf("branch" to "bugfix/dev-23")
        val destination = computeDestination(template, 1, topicEntities, params)
        assertEquals(TgDestination(1, expectedTopic), destination)
    }

    @Test
    fun shouldExceptionOnUnknownTopic() {
        val template =
            template(
                TopicTemplateDto(null, "branch", ".*(TM-[0-9]+).*", "^(TM-[0-9]+).*"),
                TopicTemplateDto(20),
            )

        val topicEntities =
            listOf(
                ChatTopicEntity(id = 12, title = "TM-123 Some description"),
                ChatTopicEntity(id = 13, title = "TM-124 Some description"),
            )
        val params = mapOf("branch" to "bugfix/TM-128-some-text")
        val ex: IllegalStateException = assertThrows {
            computeDestination(template, 1, topicEntities, params)
        }
        assertThat(ex).hasMessage("Unknown topicId 20")
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
