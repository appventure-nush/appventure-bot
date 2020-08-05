package app.nush.bot


import app.nush.bot.Config.Companion.config
import app.nush.bot.commands.Projects
import app.nush.bot.commands.Verify
import com.jessecorbett.diskord.api.rest.CreateDM
import com.jessecorbett.diskord.dsl.bot
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.commands
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.sendMessage
import kotlinx.serialization.UnstableDefault
import java.lang.Math.random

val helpText = """
    **Commands**
    **`${config.botPrefix}ping`**
    Check if server is alive
    
    **`${config.botPrefix}help`**
    Displays this message
    
    **`${config.botPrefix}projects create`**
    Creates new project (admin only)
    
""".trimIndent()

const val url =
    "http://login.microsoftonline.com/d72a7172-d5f8-4889-9a85-d7424751592a/oauth2/authorize?client_id=9f1a352a-8217-4a32-a4d3-d3c06d7b8581&redirect_uri=https://demo.chatbox2.ml/&response_type=id_token&nonce="
@ExperimentalStdlibApi
@UnstableDefault
suspend fun main() {
    bot(config.discordToken) {
        commands(config.botPrefix) {
            command("help") {
                reply(helpText)
            }
            command("ping") {
                reply("pong")
            }
            Verify.init(this@bot, this)
        }
        commands("${config.botPrefix}projects ") {
            Projects.init(this@bot, this)
        }

        userJoinedGuild {
            val user = it.user ?: return@userJoinedGuild
            val channel = clientStore.discord.createDM(CreateDM(user.id))
            val desc = """
                        Welcome to the AppVenture Discord!
                        
                        To complete verification, click [this link]($url${random()}) then send the result here.
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
