package com.dumbledank.madlibz

import com.dumbledank.madlibz.event.AppMentionEvent
import com.dumbledank.madlibz.response.AppMentionResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.jackson.jackson
import io.ktor.request.receiveStream
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import java.net.URL

fun Application.main() {
    val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
    }

    install(ContentNegotiation) {
        jackson {}
    }

    routing {
        post("/") {
            call.respond(200)
            val data = jacksonObjectMapper().readTree(call.receiveStream())
            when (data["event"]["type"].textValue()) {
                "app_mention" -> {
                    val event = jacksonObjectMapper().treeToValue<AppMentionEvent>(data["event"])
                    client.post<String> {
                        url(URL("https://slack.com/api/chat.postMessage"))
                        contentType(ContentType.Application.Json)
                        header("Authorization", "Bearer $botToken")
                        body = AppMentionResponse(event.channel, "Hello <@${event.user}>! This works!")
                    }
                }
            }
        }
    }
}

@KtorExperimentalAPI
val Application.botToken get() = environment.config.property("slack.bot.token").getString()
