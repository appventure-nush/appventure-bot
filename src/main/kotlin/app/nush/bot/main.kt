package app.nush.bot


import app.nush.bot.Config.Companion.config
import app.nush.bot.commands.Projects
import com.jessecorbett.diskord.dsl.bot
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.commands
import kotlinx.serialization.UnstableDefault

val helpText = """
  **Commands**
""".trimIndent()

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
        }
        commands("${config.botPrefix}projects ") {
            Projects.init(this@bot, this)
        }
    }
}
