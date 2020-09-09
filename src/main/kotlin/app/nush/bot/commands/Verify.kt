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
                sendVerifyMessage(bot, authorId)
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
            var userIsAppVenture = false
            val channel = clientStore.discord.createDM(CreateDM(discordUserId))
            val guildClient = clientStore.guilds[config.guildId]
            val channelClient = clientStore.channels["738913820021358672"]
            for (member in members) {
                if (member.Email == user.email) {
                    userIsAppVenture = true
                    guildClient.addMemberRole(
                        discordUserId,
                        config.memberRole
                    )
                }
            }
            if (!userIsAppVenture) {
                channelClient.sendMessage("$name($discordUserId) is requesting to join the server!")
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
