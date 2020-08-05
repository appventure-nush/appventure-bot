package app.nush.bot.commands

import app.nush.bot.Config.Companion.config
import app.nush.bot.url
import com.jessecorbett.diskord.api.rest.CreateDM
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.*
import com.junron.pyrobase.msauth.Either
import com.junron.pyrobase.msauth.User
import com.junron.pyrobase.msauth.verify
fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { it.capitalize() }

object Verify : Command {
    override fun init(bot: Bot, prefix: CommandSet) {
        with(bot) {
            with(prefix) {
                command("verify") {
                    if (guildId != null) {
                        replyAndDelete("Please DM me")
                        return@command
                    }
                    val token = words.getOrNull(1) ?: return@command run {
                        reply("Please specify a token")
                    }
                    val user = when (val result = verify(token)) {
                        is Either.Left<User> -> {
                            result.value
                        }
                        is Either.Right<String> -> {
                            reply("Error: ${result.value}")
                            null
                        }
                    } ?: return@command
                    val name = user.name.toLowerCase().capitalizeWords()

                    if (user.exp < System.currentTimeMillis() / 1000) {
                        reply("Error: Token expired")
                        return@command
                    }
                    clientStore.guilds[config.guildId].addMemberRole(
                        authorId,
                        config.memberRole
                    )
                    clientStore.guilds[config.guildId].changeNickname(
                        authorId,
                        name
                    )
                    reply("Welcome, $name")
                }

                command("sendverify"){
                    sendVerifyMessage(bot, authorId)
                }
            }
        }
    }

    suspend fun sendVerifyMessage(bot: Bot, userId: String) {
        with(bot) {
            val channel = clientStore.discord.createDM(CreateDM(userId))
            val desc = """
                        Welcome to the AppVenture Discord!
                        
                        To complete verification, click [this link]($url${Math.random()}) then send the result here.
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

}
