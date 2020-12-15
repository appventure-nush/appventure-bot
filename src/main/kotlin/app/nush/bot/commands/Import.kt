package app.nush.bot.commands

import app.nush.bot.DB
import app.nush.bot.commands.utils.isExco
import app.nush.bot.server.client
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.util.authorId
import io.ktor.client.request.get
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.io.InputStream

object Import : Command {
    lateinit var bot: Bot
    override fun init(bot: Bot, prefix: CommandSet) {
        with(bot) {
            with(prefix) {
                command("import") {
                    val guildId = guildId ?: return@command
                    if (!isExco(authorId, guildId, bot)) {
                        reply("You are not authorized")
                        return@command
                    }
                    if (attachments.isEmpty()) {
                        reply("CSV file required")
                        return@command
                    }
                    val stream =
                        client.get<InputStream>(attachments.first().url)
                    val rows = csvReader().readAllWithHeader(stream)
                    try {
                        rows.map {
                            val name =
                                it["name"] ?: throw IllegalStateException()
                            val email =
                                it["email"] ?: throw IllegalStateException()
                            try {
                                DB.createMember(email, name)
                            } catch (e: ExposedSQLException) {
                                reply(e.localizedMessage)
                                throw e
                            }
                        }
                    } catch (e: IllegalStateException) {
                        reply("Invalid format. Ensure that `name` and `email` columns exist.")
                        return@command
                    }
                    reply("Created ${rows.size} members")
                }
            }
        }
    }
}
