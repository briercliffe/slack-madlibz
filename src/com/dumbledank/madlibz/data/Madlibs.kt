package com.dumbledank.madlibz.data

import org.jetbrains.exposed.dao.*
import java.util.*

object Madlibs : UUIDTable() {
    // jackson serialised MadlibContent
    val contentJson = text("content")
    val author = varchar("author", 20)
}

class MadlibEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MadlibEntity>(Madlibs)

    var contentJson by Madlibs.contentJson
    var author by Madlibs.author
}

data class MadlibContent(
    val text: String,
    val prompts: List<String>
)
