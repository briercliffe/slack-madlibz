package com.dumbledank.madlibz

import com.dumbledank.madlibz.data.MadlibContent
import com.dumbledank.madlibz.event.AppMentionEvent
import com.dumbledank.madlibz.event.ChannelMessageEvent
import com.dumbledank.madlibz.response.AppMentionResponse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.jackson.jackson
import io.ktor.request.receiveParameters
import io.ktor.request.receiveStream
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import java.net.URL

@KtorExperimentalAPI
fun Application.main() {
    val dataService = DataService(dbUrl, dbDriver, dbUser, dbPassword)
    val madlibService = MadlibService()

    val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
    }

    install(ContentNegotiation) {
        jackson {}
    }

    suspend fun handleAppMention(eventJson: JsonNode) {
        val event = jacksonObjectMapper().treeToValue<AppMentionEvent>(eventJson)
        println(event)
        val activeSession = dataService.getActiveSessionsForUserInChannel(event.user, event.channel)
            .firstOrNull() ?: dataService.createNewSession(event.user, event.channel)
        val madlibEntity = dataService.readMadlibForSession(activeSession)
        val madlib = jacksonObjectMapper().readValue<MadlibContent>(madlibEntity.contentJson)
        val responses = jacksonObjectMapper().readValue<List<String>>(activeSession.responses).toMutableList()

        val nextPrompt = madlibService.getNextPrompt(madlib, responses)
        val slackResponse = "<@${event.user}>, " + madlibService.getRandomPromptFlavor(nextPrompt)

        client.post<String> {
            url(URL("https://slack.com/api/chat.postMessage"))
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $botToken")
            body = AppMentionResponse(event.channel, slackResponse)
        }
    }

    suspend fun handleChannelMessage(eventJson: JsonNode) {
        val event = jacksonObjectMapper().treeToValue<ChannelMessageEvent>(eventJson)
        println(event)
        if (event.subtype != null || event.text == null || event.text!!.startsWith("<@")) {
            return
        }

        val activeSession = dataService.getActiveSessionsForUserInChannel(event.user!!, event.channel).firstOrNull()
        if (activeSession != null) {
            val madlibEntity = dataService.readMadlibForSession(activeSession)
            val madlib = jacksonObjectMapper().readValue<MadlibContent>(madlibEntity.contentJson)
            val responses = jacksonObjectMapper().readValue<List<String>>(activeSession.responses).toMutableList()

            if (!event.text.isNullOrBlank()) {
                responses.add(event.text!!)
                dataService.addResponse(activeSession, jacksonObjectMapper().writeValueAsString(responses))

                val slackResponse = if (madlibService.isComplete(madlib, responses)) {
                    dataService.markSessionComplete(activeSession)
                    madlibService.assembleResult(madlib.text, responses)
                } else {
                    val nextPrompt = madlibService.getNextPrompt(madlib, responses)
                    "<@${event.user}>, " + madlibService.getRandomPromptFlavor(nextPrompt)
                }

                client.post<String> {
                    url(URL("https://slack.com/api/chat.postMessage"))
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $botToken")
                    body = AppMentionResponse(event.channel, slackResponse)
                }
            }
        }
    }

    routing {
        post("/") {
            val data = jacksonObjectMapper().readTree(call.receiveStream())
            if (data["type"].textValue() == "url_verification") {
                call.respondText(data["challenge"].textValue())
                return@post
            }
            call.respond(200)
            when (data["event"]["type"].textValue()) {
                "app_mention" -> handleAppMention(data["event"])
                "message" -> handleChannelMessage(data["event"])
            }
        }
        post("/create") {
            val data = call.receiveParameters()
            if (data["text"]?.startsWith("create") == true) {
                val rawText = data["text"]!!.substring("create ".length)
                val madlib = madlibService.parseInput(rawText)
                dataService.createMadlib(data["user_id"]!!, jacksonObjectMapper().writeValueAsString(madlib))
                call.respondText("Successfully created madlib")
            } else {
                call.respondText("Unknown command", ContentType.Text.Plain, HttpStatusCode.BadRequest)
            }
        }
    }


}

@KtorExperimentalAPI
val Application.botToken get() = environment.config.property("slack.bot.token").getString()
@KtorExperimentalAPI
val Application.dbUrl get() = environment.config.property("database.url").getString()
@KtorExperimentalAPI
val Application.dbDriver get() = environment.config.property("database.driver").getString()
@KtorExperimentalAPI
val Application.dbUser get() = environment.config.property("database.username").getString()
@KtorExperimentalAPI
val Application.dbPassword get() = environment.config.property("database.password").getString()
