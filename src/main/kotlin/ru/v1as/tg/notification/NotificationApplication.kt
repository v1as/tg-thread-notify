package ru.v1as.tg.notification

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication class NotificationApplication

fun main(args: Array<String>) {
    runApplication<NotificationApplication>(*args)
}
