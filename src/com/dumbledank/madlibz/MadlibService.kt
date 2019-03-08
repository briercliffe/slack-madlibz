package com.dumbledank.madlibz

import com.dumbledank.madlibz.data.MadlibContent
import java.lang.StringBuilder

class MadlibService {
    fun parseInput(rawText: String): MadlibContent {
        val prompts = "\\[.*?\\]".toRegex().findAll(rawText).map { it.value }
        return MadlibContent(rawText, prompts.toList())
    }

    fun isComplete(madlibContent: MadlibContent, responses: List<String>): Boolean {
        return madlibContent.prompts.size == responses.size
    }

    fun getNextPrompt(madlibContent: MadlibContent, responses: List<String>): String {
        return madlibContent.prompts[responses.size]
    }

    fun assembleResult(rawText: String, responses: List<String>): String {
        val output = StringBuilder()
        var lastPosition = 0
        "\\[.*?\\]".toRegex().findAll(rawText).forEachIndexed { index, matchResult ->
            output.append(rawText.substring(lastPosition, matchResult.range.first))
            output.append(responses[index])
            lastPosition = matchResult.range.endInclusive + 1
        }
        if (lastPosition < rawText.length) {
            output.append(rawText.substring(lastPosition))
        }
        return output.toString()
    }

    fun getRandomPromptFlavor(prompt: String): String {
        return listOf(
            "I'm looking for a $prompt",
            "A $prompt is next",
            "Give me a $prompt",
            "Yo, I need a $prompt"
        ).shuffled().first()
    }
}
