package app.nush.bot.commands

import app.nush.bot.*
import app.nush.bot.Config.Companion.config
import app.nush.bot.commands.utils.excoCommand
import app.nush.bot.server.client
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.jessecorbett.diskord.api.rest.CreateDM
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.sendFile
import io.ktor.client.request.get
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.io.File
import java.io.InputStream

object MembersCommand : Command {
    lateinit var bot: Bot
    override fun init(bot: Bot, prefix: CommandSet) {
        with(bot) {
            with(prefix) {
                excoCommand("import") {
                    if (attachments.isEmpty()) {
                        reply("CSV file required")
                        return@excoCommand
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
                        return@excoCommand
                    }
                    reply("Created ${rows.size} members")
                }
                excoCommand("export", allowBots = false) {
                    val channel =
                        clientStore.discord.createDM(CreateDM(authorId))
                    val client = clientStore.channels[channel.id]
                    val file = File.createTempFile("members", ".csv")
                    file.deleteOnExit()
                    csvWriter().open(file) {
                        writeRow("Email", "Name", "Discord ID", "GitHub")
                        DB.getMembers().filter {
                            it.year <= 6
                        }.forEach {
                            writeRow(it.email,
                                it.name,
                                it.discordID,
                                it.githubUsername)
                        }
                    }
                    client.sendFile(file)
                    reply("Check your DMs")
                }
                excoCommand("refresh") {
                    val guildClient = bot.clientStore.guilds[config.guildId]
                    val num = DB.getMembers()
                        .filter { it.year == 6 }
                        .mapNotNull {
                            it.discordID?.let { discordId ->
                                guildClient.addMemberRole(
                                    discordId.toString(),
                                    config.alumniRole
                                )
                            }
                        }.size
                    reply("$num members graduated.")
                }
            }
        }
    }
}
