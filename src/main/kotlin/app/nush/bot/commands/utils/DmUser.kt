package app.nush.bot.commands.utils

import com.jessecorbett.diskord.api.rest.CreateDM
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.util.sendMessage

suspend fun Bot.dmUser(userId: String, message: String) {
    val channel = this.clientStore.discord.createDM(
        CreateDM(
            userId
        )
    )
    this.clientStore.channels[channel.id].sendMessage(message)
}
