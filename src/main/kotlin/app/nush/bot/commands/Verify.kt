package app.nush.bot.commands

import app.nush.bot.Config.Companion.config
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.changeNickname
import com.jessecorbett.diskord.util.words
import com.junron.pyrobase.msauth.Either
import com.junron.pyrobase.msauth.User
import com.junron.pyrobase.msauth.verify

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
                        user.name
                    )
                    reply("Welcome, ${user.name}")
                }
            }
        }
    }

}
