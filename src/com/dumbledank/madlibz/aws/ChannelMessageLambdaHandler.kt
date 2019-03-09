package com.dumbledank.madlibz.aws

import com.amazonaws.serverless.proxy.model.AwsProxyRequest
import com.amazonaws.serverless.proxy.model.AwsProxyResponse
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.dumbledank.madlibz.DataService
import com.dumbledank.madlibz.MadlibService
import com.dumbledank.madlibz.data.MadlibContent
import com.dumbledank.madlibz.event.AppMentionEvent
import com.dumbledank.madlibz.event.ChannelMessageEvent
import com.dumbledank.madlibz.response.AppMentionResponse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

class ChannelMessageLambdaHandler(
    private val dbUrl: String = System.getenv("RDS_URL"),
    private val dbDriver: String = System.getenv("RDS_DRIVER"),
    private val dbUser: String = System.getenv("RDS_USER"),
    private val dbPassword: String = System.getenv("RDS_PASSWORD"),
    private val botToken: String = System.getenv("BOT_TOKEN")) : RequestStreamHandler {

    private val madlibService = MadlibService()
    private val dataService: DataService by lazy { DataService(dbUrl, dbDriver, dbUser, dbPassword) }
    private val httpClient: HttpClient by lazy {
        HttpClient {
            install(JsonFeature) {
                serializer = JacksonSerializer()
            }
        }
    }

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        val awsProxyRequest = jacksonObjectMapper().readValue<AwsProxyRequest>(input)
        val body = jacksonObjectMapper().readTree(awsProxyRequest.body)
        context.logger.log("$body")
        if (body["type"].textValue() == "url_verification") {
            respondWithSuccess(output, body["challenge"].textValue())
        } else {
            respondWithSuccess(output, "Success", 200)
            when (body["event"]["type"].textValue()) {
                "app_mention" -> handleAppMention(body["event"], context.logger)
                "message" -> handleChannelMessage(body["event"], context.logger)
            }
        }
    }

    private fun handleAppMention(eventJson: JsonNode, logger: LambdaLogger) {
        val event = jacksonObjectMapper().treeToValue<AppMentionEvent>(eventJson)
        val activeSession = dataService.getActiveSessionsForUserInChannel(event.user, event.channel)
            .firstOrNull() ?: dataService.createNewSession(event.user, event.channel)
        val madlibEntity = dataService.readMadlibForSession(activeSession)
        val madlib = jacksonObjectMapper().readValue<MadlibContent>(madlibEntity.contentJson)
        val responses = jacksonObjectMapper().readValue<List<String>>(activeSession.responses).toMutableList()

        val nextPrompt = madlibService.getNextPrompt(madlib, responses)
        val slackResponse = "<@${event.user}>, " + madlibService.getRandomPromptFlavor(nextPrompt)
        val packagedResponse = AppMentionResponse(event.channel, slackResponse)
        runBlocking {
            notifySlack(packagedResponse)
        }
    }


    private fun handleChannelMessage(eventJson: JsonNode, logger: LambdaLogger) {
        val event = jacksonObjectMapper().treeToValue<ChannelMessageEvent>(eventJson)
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

                val packagedResponse = AppMentionResponse(event.channel, slackResponse)
                runBlocking {
                    notifySlack(packagedResponse)
                }
            }
        }
    }

    private fun respondWithSuccess(
        outputStream: OutputStream,
        responseBody: String,
        statusCode: Int = 200
    ) {
        jacksonObjectMapper().writeValue(
            outputStream,
            AwsProxyResponse(
                statusCode,
                null,
                responseBody
            )
        )
        outputStream.flush()
    }

    private suspend fun notifySlack(response: AppMentionResponse) {
        httpClient.post<String> {
            url(URL("https://slack.com/api/chat.postMessage"))
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $botToken")
            body = response
        }
    }
}
