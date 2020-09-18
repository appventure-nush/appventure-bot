package app.nush.bot.commands

import app.nush.bot.Config.Companion.config
import app.nush.bot.botId
import com.jessecorbett.diskord.api.rest.CreateDM
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
                if (newName != "") {
                    request(
                        bot,
                        authorId,
                        newName
                    )
                } else {
                    with(bot) {
                        clientStore.channels[this@command.id].sendMessage("You may not request an empty nickname!")
                    }
                }
            }
        }
        with(bot) {
            reactionAdded { messageReaction ->
                try {
                    val tick = "✅"
                    val cross = "❎"
                    if (messageReaction.emoji.name != cross && messageReaction.emoji.name != tick) {
                        return@reactionAdded
                    }
                    val guildClient = clientStore.guilds[config.guildId]
                    val channelClient = clientStore.channels[config.excoChannelId]
                    if (!guildClient.getMember(messageReaction.userId).user?.isBot!!) {
                        val nickRequestMessage = channelClient.getMessage(messageReaction.messageId)
                        if (nickRequestMessage.content.substring(nickRequestMessage.content.length - 96) != "\". ✅ if you want to accept the rename request, ❎ if you do not want to accept the rename request") {
                            return@reactionAdded
                        }
                        if (nickRequestMessage.authorId != botId) return@reactionAdded
                        val newName: String = nickRequestMessage.content.subSequence(
                            nickRequestMessage.content.indexOf("\"") + 1,
                            nickRequestMessage.content.indexOf(
                                "\"",
                                nickRequestMessage.content.indexOf("\"") + 1
                            )
                        ).toString()
                        if (messageReaction.emoji.name == cross) {
                            clientStore.channels[clientStore.discord.createDM(
                                CreateDM(
                                    nickRequestMessage.usersMentioned[0].id
                                )
                            ).id].sendMessage("Your rename request to \"$newName\" was rejected by an exco member.")
                            channelClient.sendMessage(
                                "<@!${messageReaction.userId}> has denied ${
                                    guildClient.getMember(
                                        nickRequestMessage.usersMentioned[0].id
                                    ).nickname
                                }(<@!${nickRequestMessage.usersMentioned[0].id}>)'s request to change name to \"$newName\""
                            )
                            nickRequestMessage.delete()
                        } else if (messageReaction.emoji.name == tick) {
                            clientStore.channels[clientStore.discord.createDM(
                                CreateDM(
                                    nickRequestMessage.usersMentioned[0].id
                                )
                            ).id].sendMessage("Your rename request to \"$newName\" was accepted by an exco member.")
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
                } catch (e: Exception) {
                }
            }
        }
    }

    suspend fun request(bot: Bot, userId: String, newName: String) {
        with(bot) {
            val guildClient = clientStore.guilds[config.guildId]
            val channelClient = clientStore.channels[config.excoChannelId]
            clientStore.channels[clientStore.discord.createDM(
                CreateDM(
                    userId
                )
            ).id].sendMessage("Your rename request has been sent to the exco, it will be processed as soon as possible.")
            val renameMessage =
                channelClient.sendMessage("${guildClient.getMember(userId).nickname}(<@!$userId>) has requested to be renamed to \"$newName\". ✅ if you want to accept the rename request, ❎ if you do not want to accept the rename request")
            renameMessage.react("✅")
            renameMessage.react("❎")
        }
    }
}