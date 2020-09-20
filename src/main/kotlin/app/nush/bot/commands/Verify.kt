package app.nush.bot.commands

import app.nush.bot.Config.Companion.config
import app.nush.bot.botId
import app.nush.bot.commands.utils.Emojis.cross
import app.nush.bot.commands.utils.Emojis.hatEmote
import app.nush.bot.commands.utils.Emojis.tick
import app.nush.bot.commands.utils.dmUser
import app.nush.bot.models.Member.Companion.members
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
                userVerified(
                    User("LAM JUN RONG", "h171004@nushigh.edu.sg", 0),
                    "585449672584331265"
                )
            }
        }
        with(bot) {
            reactionAdded { messageReaction ->
                if (messageReaction.userId == botId) {
                    return@reactionAdded
                }
                if (messageReaction.emoji.name != hatEmote && messageReaction.emoji.name != cross && messageReaction.emoji.name != tick) {
                    return@reactionAdded
                }
                val guildClient = clientStore.guilds[config.guildId]
                val channelClient = clientStore.channels[config.excoChannelId]
                val accessRequestMessage =
                    channelClient.getMessage(messageReaction.messageId)
                if ("is requesting to access the server" !in accessRequestMessage.content) {
                    return@reactionAdded
                }
                if (accessRequestMessage.authorId != botId) return@reactionAdded
                when (messageReaction.emoji.name) {
                    hatEmote -> {
                        bot.dmUser(
                            accessRequestMessage.usersMentioned[0].id,
                            "An exco has allowed your access to the AppVenture server as an alumni."
                        )
                        channelClient.sendMessage(
                            "<@!${messageReaction.userId}> has allowed ${
                                guildClient.getMember(
                                    accessRequestMessage.usersMentioned[0].id
                                ).nickname
                            }(<@!${accessRequestMessage.usersMentioned[0].id}>) to access the server as an alumni."
                        )
                        guildClient.addMemberRole(
                            accessRequestMessage.usersMentioned[0].id,
                            config.alumniRole
                        )
                        accessRequestMessage.delete()
                    }
                    cross -> {
                        bot.dmUser(
                            accessRequestMessage.usersMentioned[0].id,
                            "You were kicked from the AppVenture server because an exco denied your access."
                        )
                        channelClient.sendMessage(
                            "<@!${messageReaction.userId}> has denied ${
                                guildClient.getMember(
                                    accessRequestMessage.usersMentioned[0].id
                                ).nickname
                            }(<@!${accessRequestMessage.usersMentioned[0].id}>) to access the server."
                        )
                        guildClient.removeMember(accessRequestMessage.usersMentioned[0].id)
                        accessRequestMessage.delete()
                    }
                    tick -> {
                        bot.dmUser(
                            accessRequestMessage.usersMentioned[0].id,
                            "An exco has allowed your access to the AppVenture server as a guest."
                        )
                        channelClient.sendMessage(
                            "<@!${messageReaction.userId}> has allowed ${
                                guildClient.getMember(
                                    accessRequestMessage.usersMentioned[0].id
                                ).nickname
                            }(<@!${accessRequestMessage.usersMentioned[0].id}>) to access the server as a guest."
                        )
                        guildClient.addMemberRole(
                            accessRequestMessage.usersMentioned[0].id,
                            config.guestRole
                        )
                        accessRequestMessage.delete()
                    }
                }
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
            var userIsAppVentureMember = false
            val channel = clientStore.discord.createDM(CreateDM(discordUserId))
            val guildClient = clientStore.guilds[config.guildId]
            val channelClient = clientStore.channels[config.excoChannelId]
            if (members.any { it.email == user.email }) {
                userIsAppVentureMember = true
                guildClient.addMemberRole(
                    discordUserId,
                    config.memberRole
                )
            }
            if (!userIsAppVentureMember) {
                bot.dmUser(
                    discordUserId,
                    "As you are not a present appventure member, your join request has been forwarded to the exco"
                )
                val accessMessage =
                    channelClient.sendMessage("$name(<@!$discordUserId>) is requesting to access the server, react with $hatEmote if you want to allow them to access the server as an alumni, $tick if you want to allow them to access the server as a guest, $cross if you do not want to allow them to access the server")
                accessMessage.react(hatEmote)
                accessMessage.react(tick)
                accessMessage.react(cross)
            } else {
                clientStore.channels[channel.id].sendMessage("Welcome, $name")
            }
            guildClient.changeNickname(
                discordUserId,
                name
            )
        }
    }
}
