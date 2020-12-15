package app.nush.bot.commands.utils

import app.nush.bot.Config.Companion.config
import com.jessecorbett.diskord.dsl.Bot

suspend fun isExco(discordId: String, guildId: String, bot: Bot): Boolean {
    val roles =
        bot.clientStore.guilds[guildId].getMember(discordId).roleIds
    return config.excoRole !in roles && !config.dev
}
