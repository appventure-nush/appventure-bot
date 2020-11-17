package app.nush.bot

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object Members : Table() {
    val email = varchar("id", 8)
    val name = varchar("name", 100)
    val discordID = long("discordID").nullable()
    val githubUsername = varchar("github", 100).nullable()

    override val primaryKey = PrimaryKey(email)
}

object DB {
    init {
        Database.connect("jdbc:sqlite:./data/data.db", "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(Members)
        }
    }

    fun createMember(email: String, name: String) {
        transaction {
            Members.insert {
                it[Members.email] = email
                it[Members.name] = name
            }
        }
    }

    fun getMemberByEmail(id: String): ResultRow? {
        return transaction {
            Members.select {
                Members.email eq id
            }.map { it }.getOrNull(0)
        }
    }

    fun getMemberByName(name: String): ResultRow? {
        return transaction {
            Members.select {
                Members.name like "%$name%"
            }.map { it }.getOrNull(0)
        }
    }

    fun getMembers(): List<ResultRow> {
        return transaction {
            return@transaction Members.selectAll().map {
                it
            }
        }
    }

    fun setDiscord(id: String, discordId: Long) {
        transaction {
            Members.update({
                Members.email eq id
            }) {
                it[discordID] = discordId
            }
        }
    }
}
