package com.dumbledank.madlibz.data

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.Column
import java.util.*

object Sessions : UUIDTable() {
    val user: Column<String> = varchar("user", 20)
    val channel: Column<String> = varchar("channel", 20)
    val madlib = reference("madlib", Madlibs)
    val active: Column<Boolean> = bool("active").default(true)
}

class SessionEntity(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<SessionEntity>(Sessions)

    var user by Sessions.user
    var channel by Sessions.channel
    var madlib by MadlibEntity referencedOn Sessions.madlib
    var active by Sessions.active
}
