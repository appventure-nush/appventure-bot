package app.nush.bot.commands

import app.nush.bot.Config.Companion.config
import app.nush.bot.commands.Member.Companion.members
import app.nush.bot.server.PendingVerify
import app.nush.bot.server.pendingRequests
import app.nush.bot.url
import com.jessecorbett.diskord.api.rest.CreateDM
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.changeNickname
import com.jessecorbett.diskord.util.sendMessage
import com.junron.pyrobase.msauth.User

fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { it.capitalize() }

object Verify : Command {
    lateinit var bot: Bot
    override fun init(bot: Bot, prefix: CommandSet) {
        this.bot = bot
        with(prefix) {
            command("sendverify") {
            userVerified(User("Su Zhengchong", "h1810141@nushigh.edu.sg", 0), authorId)
            }
        }
    }

    suspend fun sendVerifyMessage(bot: Bot, userId: String) {
        with(bot) {
            val channel = clientStore.discord.createDM(CreateDM(userId))
            val request = PendingVerify(
                discordUserId = userId,
                timestamp = System.currentTimeMillis()
            )
            pendingRequests += request
            val desc = """
                        Welcome to the AppVenture Discord!
                        
                        To complete verification, click [this link]($url${request.id}) and follow the instructions.
                        The link is valid for 1 hour.
                        Alternatively, you can DM any exco to complete verification manually.
                        
                        [Learn more](https://auth0.com/docs/flows/guides/implicit/add-login-implicit)
                    """.trimIndent()
            clientStore.channels[channel.id].sendMessage(
                "",
                embed {
                    title = "AppVenture Verification"
                    description = desc
                    color = Colors.GREEN
                }
            )
        }
    }

    suspend fun userVerified(user: User, discordUserId: String) {
        val name = user.name.toLowerCase().capitalizeWords()
        with(bot) {
            config.userIsAppVentureMember = false
            val channel = clientStore.discord.createDM(CreateDM(discordUserId))
            val guildClient = clientStore.guilds[config.guildId]
            val channelClient = clientStore.channels[config.excoChannelId]
            val isMember: (Member) -> Boolean = { it.email == user.email }
            if (members.any { isMember(it) }) {
                config.userIsAppVentureMember = true
                guildClient.addMemberRole(
                    discordUserId,
                    config.memberRole
                )
            }
            if (!config.userIsAppVentureMember) {
                val accessMessage =
                    channelClient.sendMessage("$name(<@!$discordUserId>) is requesting to access the server, react with \uD83C\uDF93 if you want to allow them to access the server as an alumni, ✅ if you want to allow them to access the server as a guest, ❎ if you do not want to allow them to access the server")
                accessMessage.react("\uD83C\uDF93")
                accessMessage.react("✅")
                accessMessage.react("❎")
            } else {
                clientStore.channels[channel.id].sendMessage("Welcome, $name")
            }
            guildClient.changeNickname(
                discordUserId,
                name
            )
            reactionAdded { messageReaction ->
                try {
                    if (!guildClient.getMember(messageReaction.userId).user?.isBot!!)
                        if (messageReaction.emoji.name == "\uD83C\uDF93") {
                            clientStore.channels[clientStore.discord.createDM(
                                CreateDM(
                                    channelClient.getMessage(
                                        messageReaction.messageId
                                    ).usersMentioned[0].id
                                )
                            ).id].sendMessage("An exco has allowed your access to the AppVenture server as an alumni.")
                            channelClient.sendMessage(
                                "<@!${messageReaction.userId}> has allowed ${
                                    guildClient.getMember(
                                        channelClient.getMessage(messageReaction.messageId).usersMentioned[0].id
                                    ).nickname
                                }(<@!${channelClient.getMessage(messageReaction.messageId).usersMentioned[0].id}>) to access the server as an alumni."
                            )
                            guildClient.addMemberRole(
                                channelClient.getMessage(messageReaction.messageId).usersMentioned[0].id,
                                config.alumniRole
                            )
                            channelClient.getMessage(messageReaction.messageId).delete()
                        } else if (messageReaction.emoji.name == "❎") {
                            clientStore.channels[clientStore.discord.createDM(
                                CreateDM(
                                    channelClient.getMessage(
                                        messageReaction.messageId
                                    ).usersMentioned[0].id
                                )
                            ).id].sendMessage("You were kicked from the AppVenture server because an exco denied your access.")
                            channelClient.sendMessage(
                                "<@!${messageReaction.userId}> has denied ${
                                    guildClient.getMember(
                                        channelClient.getMessage(messageReaction.messageId).usersMentioned[0].id
                                    ).nickname
                                }(<@!${channelClient.getMessage(messageReaction.messageId).usersMentioned[0].id}>) to access the server."
                            )
                            guildClient.removeMember(channelClient.getMessage(messageReaction.messageId).usersMentioned[0].id)
                            channelClient.getMessage(messageReaction.messageId).delete()
                        } else if (messageReaction.emoji.name == "✅") {
                            clientStore.channels[clientStore.discord.createDM(
                                CreateDM(
                                    channelClient.getMessage(
                                        messageReaction.messageId
                                    ).usersMentioned[0].id
                                )
                            ).id].sendMessage("An exco has allowed your access to the AppVenture server as a guest.")
                            channelClient.sendMessage(
                                "<@!${messageReaction.userId}> has allowed ${
                                    guildClient.getMember(
                                        channelClient.getMessage(messageReaction.messageId).usersMentioned[0].id
                                    ).nickname
                                }(<@!${channelClient.getMessage(messageReaction.messageId).usersMentioned[0].id}>) to access the server as a guest."
                            )
                            guildClient.addMemberRole(
                                channelClient.getMessage(messageReaction.messageId).usersMentioned[0].id,
                                config.guestRole
                            )
                            channelClient.getMessage(messageReaction.messageId).delete()
                        }
                } catch (e: Exception) {
                }
            }
        }
    }
}
