package app.nush.bot.commands

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet

interface Command {
    fun init(bot: Bot, prefix: CommandSet)
}
