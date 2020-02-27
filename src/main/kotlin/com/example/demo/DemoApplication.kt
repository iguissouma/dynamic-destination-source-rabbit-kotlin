package com.example.demo

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import java.util.function.Consumer
import java.util.function.Supplier


@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@RestController
class DynamicDestinationController(private val jsonMapper: ObjectMapper) {

    private val processor: EmitterProcessor<Message<String>> = EmitterProcessor.create<Message<String>>()

    @RequestMapping(path = ["/api/dest/{destName}"], method = [GET], consumes = ["*/*"])
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun handleRequest(@PathVariable destName:String) {
        val message: Message<String> = MessageBuilder.withPayload("body")
                .setHeader("spring.cloud.stream.sendto.destination", destName).build()
        processor.onNext(message)
    }

    @Bean
    fun supplier(): Supplier<Flux<Message<String>>> {
        return Supplier { processor }
    }
}

const val destResourceUrl = "http://localhost:8080/api/dest"
@Component
class TestConsumer() {

    private val restTemplate: RestTemplate = RestTemplate()
    private val logger: Log = LogFactory.getLog(javaClass)

    @Bean
    fun consume1(): Consumer<String> = Consumer {
        logger.info("==============>consume1")
        restTemplate.getForEntity("$destResourceUrl/customer-1", String::class.java)
    }

    @Bean
    fun consume2(): Consumer<String> = Consumer {
        logger.info("==============>consume2")
        restTemplate.getForEntity("$destResourceUrl/customer-2", String::class.java)
    }
}


@Component
class TestSink {
    private val logger: Log = LogFactory.getLog(javaClass)
	@Bean
	fun receive1(): Consumer<String> = Consumer {
		logger.info("Data received customer-1..." + it);
	}

	@Bean
	fun receive2(): Consumer<String> = Consumer {
		logger.info("Data received customer-2..." + it);
	}
}

