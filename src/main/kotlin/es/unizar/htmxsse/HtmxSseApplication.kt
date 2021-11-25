package es.unizar.htmxsse

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HtmxSseApplication

fun main(args: Array<String>) {
    runApplication<HtmxSseApplication>(*args)
}

fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy { LoggerFactory.getLogger(this::class.java.name) }
}

fun sleep() {
    try {
        Thread.sleep(500)
    } catch (e: InterruptedException) {
    }
}

