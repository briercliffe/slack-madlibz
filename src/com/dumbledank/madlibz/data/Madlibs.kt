package com.dumbledank.madlibz.data

import org.jetbrains.exposed.dao.*
import java.util.*

object Madlibs : UUIDTable() {
    // jackson serialised MadlibContent
    val contentJson = text("content")
}

class MadlibEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MadlibEntity>(Madlibs)

    var contentJson by Madlibs.contentJson
}

data class MadlibContent(
    val text: String,
    val prompts: List<String>
)
