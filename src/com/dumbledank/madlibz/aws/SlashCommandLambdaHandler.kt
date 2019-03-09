package com.dumbledank.madlibz.aws

import com.amazonaws.serverless.proxy.model.AwsProxyRequest
import com.amazonaws.serverless.proxy.model.AwsProxyResponse
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.dumbledank.madlibz.DataService
import com.dumbledank.madlibz.MadlibService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder

class SlashCommandLambdaHandler(private val dbUrl: String = System.getenv("RDS_URL"),
                                private val dbDriver: String = System.getenv("RDS_DRIVER"),
                                private val dbUser: String = System.getenv("RDS_USER"),
                                private val dbPassword: String = System.getenv("RDS_PASSWORD")): RequestStreamHandler {

    private val madlibService = MadlibService()
    private val dataService: DataService by lazy { DataService(dbUrl, dbDriver, dbUser, dbPassword) }

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        val awsProxyRequest = jacksonObjectMapper().readValue<AwsProxyRequest>(input)
        respondWithSuccess(output, "Madlib being created")

        val data = awsProxyRequest.body.split("&").map {
            val parts = it.split("=")
            URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8")
        }.toMap()

        val userId = data["user_id"] ?: return respondWithFailure(output, "No user id found")
        val rawText = data["text"] ?: return respondWithFailure(output, "No input text found")

        context.logger.log("User [$userId], Text [$rawText]")

        val madlib = madlibService.parseInput(rawText)
        dataService.createMadlib(userId, jacksonObjectMapper().writeValueAsString(madlib))
    }

    private fun respondWithSuccess(outputStream: OutputStream, responseBody: String, statusCode: Int = 200, cache: Boolean = true) {
        jacksonObjectMapper().writeValue(
            outputStream,
            AwsProxyResponse(
                statusCode,
                null,
                responseBody)
        )
        outputStream.flush()
    }

    private fun respondWithFailure(outputStream: OutputStream, message: String, statusCode: Int = 400, cache: Boolean = false) {
        jacksonObjectMapper().writeValue(
            outputStream,
            AwsProxyResponse(
                statusCode,
                null,
                message))
    }


}
