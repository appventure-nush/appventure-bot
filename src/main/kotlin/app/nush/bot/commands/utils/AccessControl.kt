package app.nush.bot.commands.utils

import app.nush.bot.Config.Companion.config
import app.nush.bot.commands.Github.bot
import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.util.authorId

suspend fun isExco(discordId: String, bot: Bot): Boolean {
    val roles =
        bot.clientStore.guilds[config.guildId].getMember(discordId).roleIds
    return config.excoRole in roles || config.dev
}

fun CommandSet.excoCommand(
    command: String,
    allowBots: Boolean = false,
    action: suspend Message.() -> Unit
) {
    command(command, allowBots) {
        if (!isExco(authorId, bot)) {
            with(bot) {
                reply("You are not authorized")
            }
            return@command
        }
        action()
    }
}
