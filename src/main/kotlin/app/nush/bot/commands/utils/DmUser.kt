package app.nush.bot.commands.utils

import com.jessecorbett.diskord.api.rest.CreateDM
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.util.sendMessage

suspend fun dmUser(bot: Bot, userId: String, message: String) {
    val channel = bot.clientStore.discord.createDM(
        CreateDM(
            userId
        )
    )
    bot.clientStore.channels[channel.id].sendMessage(message)
}
