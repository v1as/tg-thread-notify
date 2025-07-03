package ru.v1as.tg.notification.web

import mu.KLogging
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.v1as.tg.notification.service.NotificationService

@RestController
@RequestMapping("/notify")
class NotificationController(val notificationService: NotificationService) {

    companion object : KLogging()

    @GetMapping
    fun getLoginDetails(
        @RequestParam allParams: Map<String, String>,
        @RequestParam(defaultValue = "false") silent: Boolean
    ): ResponseEntity<Map<String, String>> {
        val body = notificationService.notify(allParams, silent)
        val status = HttpStatusCode.valueOf(body["status"]?.toInt() ?: 500)
        logger.info("Notification request: $allParams, silent=$silent, finished with response: $body")
        return ResponseEntity(body, status)
    }
}
