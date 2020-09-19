package app.nush.bot.commands

import app.nush.bot.Config.Companion.config
import app.nush.bot.botId
import app.nush.bot.commands.utils.Emojis.cross
import app.nush.bot.commands.utils.Emojis.tick
import app.nush.bot.commands.utils.dmUser
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.changeNickname
import com.jessecorbett.diskord.util.sendMessage
import com.jessecorbett.diskord.util.words

object Nick : Command {
    lateinit var bot: Bot
    override fun init(bot: Bot, prefix: CommandSet) {
        this.bot = bot
        with(prefix) {
            command("nick") {
                val newName: String = words.drop(1).joinToString(separator = " ")
                println(newName)
                if (newName != "") {
                    request(
                        bot,
                        authorId,
                        newName
                    )
                } else {
                    dmUser(
                        bot,
                        authorId,
                        "You may not request an empty nickname!"
                    )
                }
            }
        }
        with(bot) {
            reactionAdded { messageReaction ->
                if (messageReaction.emoji.name != cross && messageReaction.emoji.name != tick) {
                    return@reactionAdded
                }
                val guildClient = clientStore.guilds[config.guildId]
                val channelClient = clientStore.channels[config.excoChannelId]
                if (messageReaction.userId == botId) {
                    return@reactionAdded
                }
                val nickRequestMessage = channelClient.getMessage(messageReaction.messageId)
                if ("has requested to be renamed to" !in nickRequestMessage.content) {
                    return@reactionAdded
                }
                if (nickRequestMessage.authorId != botId) return@reactionAdded
                val newName: String =
                    nickRequestMessage.content.subSequence(
                        nickRequestMessage.content.indexOf("\"") + 1,
                        nickRequestMessage.content.indexOf(
                            "\"",
                            nickRequestMessage.content.indexOf("\"") + 1
                        )
                    ).toString()
                if (messageReaction.emoji.name == cross) {
                    dmUser(
                        bot,
                        nickRequestMessage.usersMentioned[0].id,
                        "Your rename request to \"$newName\" was rejected by an exco member."
                    )
                    channelClient.sendMessage(
                        "<@!${messageReaction.userId}> has denied ${
                            guildClient.getMember(
                                nickRequestMessage.usersMentioned[0].id
                            ).nickname
                        }(<@!${nickRequestMessage.usersMentioned[0].id}>)'s request to change name to \"$newName\""
                    )
                    nickRequestMessage.delete()
                } else {
                    dmUser(
                        bot,
                        nickRequestMessage.usersMentioned[0].id,
                        "Your rename request to \"$newName\" was accepted by an exco member."
                    )
                    channelClient.sendMessage(
                        "<@!${messageReaction.userId}> has accepted ${
                            guildClient.getMember(
                                nickRequestMessage.usersMentioned[0].id
                            ).nickname
                        }(<@!${nickRequestMessage.usersMentioned[0].id}>)'s request to change name to \"$newName\""
                    )
                    guildClient.changeNickname(
                        nickRequestMessage.usersMentioned[0].id,
                        newName
                    )
                    nickRequestMessage.delete()
                }
            }
        }
    }

    suspend fun request(bot: Bot, discordUserId: String, newName: String) {
        with(bot) {
            val guildClient = clientStore.guilds[config.guildId]
            val channelClient = clientStore.channels[config.excoChannelId]
            dmUser(
                Verify.bot,
                discordUserId,
                "Your rename request has been sent to the exco, it will be processed as soon as possible."
            )
            val renameMessage =
                channelClient.sendMessage(
                    "${guildClient.getMember(discordUserId).nickname}(<@!$discordUserId>) has requested to be renamed to \"$newName\". $tick if you want to accept the rename request, $cross if you do not want to accept the rename request"
                )
            renameMessage.react(tick)
            renameMessage.react(cross)
        }
    }
}
