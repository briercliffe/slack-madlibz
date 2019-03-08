package com.dumbledank.madlibz

import com.dumbledank.madlibz.data.MadlibEntity
import com.dumbledank.madlibz.data.Madlibs
import com.dumbledank.madlibz.data.SessionEntity
import com.dumbledank.madlibz.data.Sessions
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class DataService(dbUrl: String, dbDriver: String, dbUser: String, dbPassword: String) {
    init {
        Database.connect(dbUrl, dbDriver, dbUser, dbPassword)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Madlibs, Sessions)
//            MadlibEntity.new {
//                contentJson = "This is only a test"
//            }
        }
    }

    fun readMadlibForSession(session: SessionEntity): MadlibEntity {
        return transaction {
            MadlibEntity.wrapRow(session.madlib.readValues)
        }
    }

    fun createMadlib(user: String, contentJson: String) {
        transaction {
            MadlibEntity.new {
                this.author = user
                this.contentJson = contentJson
            }
        }
    }

    fun createNewSession(user: String, channel: String): SessionEntity {
        val nextMadlib = findRandomUnseenMadlib(user) ?: findRandomMadlib()
        return transaction {
            val session = SessionEntity.new {
                this.user = user
                this.channel = channel
                this.active = true
                this.madlib = nextMadlib
            }
            session.readValues
            session.madlib.readValues
            session
        }
    }

    fun getActiveSessionsForUserInChannel(user: String, channel: String): List<SessionEntity> {
        return transaction {
            SessionEntity.find {
                (Sessions.user eq user) and (Sessions.channel eq channel) and Sessions.active
            }.toList()
        }
    }

    private fun findRandomMadlib(): MadlibEntity {
        return transaction {
            MadlibEntity.all().shuffled().first()
        }
    }

    private fun findRandomUnseenMadlib(user: String): MadlibEntity? {
        return transaction {
            Madlibs.join(Sessions, JoinType.FULL, Madlibs.id, Sessions.madlib)
                .slice(Madlibs.columns + Sessions.id)
                .select { Sessions.id.isNull() }
                .map { MadlibEntity.wrapRow(it) }
                .firstOrNull()
        }
    }

}
