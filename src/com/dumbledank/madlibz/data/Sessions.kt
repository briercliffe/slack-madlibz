package com.dumbledank.madlibz.data

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.Column
import java.util.*

object Sessions : UUIDTable() {
    val user: Column<String> = varchar("user", 20)
    val channel: Column<String> = varchar("channel", 20)
    val madlib = reference("madlib", Madlibs)
    val responses: Column<String> = text("responses")
    val active: Column<Boolean> = bool("active").default(true)
    val thread: Column<String> = text("thread").default("")
    val public: Column<Boolean> = bool("public").default(false)
}

class SessionEntity(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<SessionEntity>(Sessions)

    var user by Sessions.user
    var channel by Sessions.channel
    var madlib by MadlibEntity referencedOn Sessions.madlib
    var responses by Sessions.responses
    var active by Sessions.active
    var thread by Sessions.thread
    var public by Sessions.public
}
