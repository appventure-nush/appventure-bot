package app.nush.bot.commands

import app.nush.bot.Config.Companion.config
import com.jessecorbett.diskord.api.model.*
import com.jessecorbett.diskord.api.rest.CreateChannel
import com.jessecorbett.diskord.api.rest.CreateGuildRole
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.words
import com.jessecorbett.diskord.api.rest.CreateWebhook
import org.kohsuke.github.*
import java.io.IOException

object Projects : Command {
    override fun init(bot: Bot, prefix: CommandSet) {
        with(bot) {
            with(prefix) {
                command("create") {
                    val guildId = guildId ?: return@command
                    val roles =
                        clientStore.guilds[guildId].getMember(authorId).roleIds
                    if (config.excoRole !in roles && !config.dev) {
                        reply("You are not authorized")
                        return@command
                    }
                    if (words.size < 3) {
                        reply("Please specify a project name")
                        return@command
                    }
                    val projName =
                        words.slice(2..words.lastIndex).joinToString("-")
                            .toLowerCase().trim()
                    val guild = bot.clientStore.guilds[guildId]
                    if (guild.getRoles().any { it.name == projName }) {
                        reply("Project already exists")
                        return@command
                    }
                    val role = guild.createRole(
                        CreateGuildRole(
                            projName,
                            Permissions(0),
                            displayedSeparately = false,
                            mentionable = false
                        )
                    )
                    val denyAll = Overwrite(
                        guildId,
                        OverwriteType.ROLE,
                        Permissions.NONE,
                        Permissions.ALL
                    )
                    val overwrites = listOf(
                        denyAll,
                        Overwrite(
                            role.id, OverwriteType.ROLE,
                            Permissions.of(
                                Permission.VIEW_CHANNEL,
                                Permission.EMBED_LINKS,
                                Permission.ATTACH_FILES,
                                Permission.READ_MESSAGE_HISTORY,
                                Permission.SEND_MESSAGES,
                                Permission.ADD_REACTIONS
                            ),
                            denied = Permissions.NONE
                        )
                    )
                    val overwritesVoice = listOf(
                        denyAll,
                        Overwrite(
                            role.id, OverwriteType.ROLE, Permissions.of(
                                Permission.CONNECT,
                                Permission.SPEAK,
                                Permission.VIEW_CHANNEL
                            ), denied = Permissions.NONE
                        )
                    )

                    val channel = guild.createChannel(
                        CreateChannel(
                            projName,
                            ChannelType.GUILD_TEXT,
                            categoryId = config.projectsCategoryId
                        )
                    )
                    bot.clientStore.channels[channel.id].update(
                        channel.copy(
                            permissionOverwrites = overwrites
                        )
                    )

                    val vc = guild.createChannel(
                        CreateChannel(
                            "$projName-voice",
                            ChannelType.GUILD_VOICE,
                            categoryId = config.projectsCategoryId
                        )
                    )
                    bot.clientStore.channels[vc.id].update(
                        vc.copy(
                            permissionOverwrites = overwritesVoice
                        )
                    )
                    reply("Channel <#${channel.id}> created")

                    if (!(words.size == 4 && words[3].equals("channel-only"))) {
                        val discordWebhook =
                            bot.clientStore.channels[channel.id].createWebhook(CreateWebhook("For GitHub"))
                        createWH(projName, discordWebhook.id, discordWebhook.token)
                        reply("GitHub repository $projName created")
                        reply("Link: https://github.com/appventure-nush/$projName")
                    }
                }
                command("linkrepo") {
                    val guildId = guildId ?: return@command
                    val roles =
                        clientStore.guilds[guildId].getMember(authorId).roleIds
                    if (config.excoRole !in roles && !config.dev) {
                        reply("You are not authorized")
                        return@command
                    }
                    if (words.size < 3) {
                        reply("Please specify a project name")
                        return@command
                    }
                    val projName = words[2]
                    val discordWebhook = bot.clientStore.channels[channelId].createWebhook(CreateWebhook("For GitHub"))
                    try {
                        createWH(projName, discordWebhook.id, discordWebhook.token)
                        reply("Successfully linked ${channel.get().name} with $projName")
                    } catch (e: IOException) {
                        reply("Repository not found")
                        return@command
                    }

                }
            }
        }
    }

    fun createWH(projName: String, id: String, token: String) {
        val github = GitHubBuilder().withOAuthToken(config.githubToken).build()
        val org = github.getOrganization("appventure-nush")
        val repo = org.getRepository(projName) ?: org.createRepository(projName).create() // if no repo, then create one
//        val repo = org.getRepository("appventure-bot") // use for testing
        val urlstr = "https://discordapp.com/api/webhooks/" + id +
                "/" + token + "/github"
        repo.createHook(
            "web",
            mapOf(
                "url" to
                        urlstr, "content_type" to "json", "insecure_ssl" to "0"
            ),
            listOf(GHEvent.PUSH, GHEvent.PULL_REQUEST),
            true
        )
    }
}
