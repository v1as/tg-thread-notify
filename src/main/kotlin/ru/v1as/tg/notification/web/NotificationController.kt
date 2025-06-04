package ru.v1as.tg.notification.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.v1as.tg.notification.service.NotificationService

@RestController
@RequestMapping("/notification")
class NotificationController(val notificationService: NotificationService) {
    @GetMapping
    fun getLoginDetails(@RequestParam allParams: Map<String, String>): Map<String, Any> =
        notificationService.notify(allParams)
}
