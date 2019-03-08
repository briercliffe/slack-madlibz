package com.dumbledank.madlibz

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class MadlibServiceTest {
    val madlibService = MadlibService()
    @Test
    fun assembleResult() {
        val rawText = "This is a [Noun], to [Verb] it works!"
        val responses = listOf("Test", "Make Sure")

        val actual = madlibService.assembleResult(rawText, responses)
        assertEquals("This is a Test, to Make Sure it works!", actual)
    }
}
