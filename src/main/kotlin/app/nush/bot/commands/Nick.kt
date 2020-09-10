package app.nush.bot.commands

import app.nush.bot.Config.Companion.config
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
                request(
                    bot,
                    authorId,
                    words.subList(1.coerceAtMost(words.size - 1), words.size - 1).joinToString(separator = " ")
                )
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
            reactionAdded { messageReaction ->
                try {
                    if (!guildClient.getMember(messageReaction.userId).user?.isBot!!) {
                        val newName: String = channelClient.getMessage(messageReaction.messageId).content.subSequence(
                            channelClient.getMessage(messageReaction.messageId).content.indexOf("\"") + 1,
                            channelClient.getMessage(messageReaction.messageId).content.indexOf(
                                "\"",
                                channelClient.getMessage(messageReaction.messageId).content.indexOf("\"") + 1
                            )
                        ).toString()
                        if (messageReaction.emoji.name == "❎") {
                            clientStore.channels[clientStore.discord.createDM(
                                CreateDM(
                                    channelClient.getMessage(
                                        messageReaction.messageId
                                    ).usersMentioned[0].id
                                )
                            ).id].sendMessage("Your rename request to \"$newName\" was rejected by an exco member.")
                            channelClient.sendMessage(
                                "<@!${messageReaction.userId}> has denied ${
                                    guildClient.getMember(
                                        channelClient.getMessage(messageReaction.messageId).usersMentioned[0].id
                                    ).nickname
                                }(<@!${channelClient.getMessage(messageReaction.messageId).usersMentioned[0].id}>)'s request to change name to \"$newName\""
                            )
                            channelClient.getMessage(messageReaction.messageId).delete()
                        } else if (messageReaction.emoji.name == "✅") {
                            clientStore.channels[clientStore.discord.createDM(
                                CreateDM(
                                    channelClient.getMessage(
                                        messageReaction.messageId
                                    ).usersMentioned[0].id
                                )
                            ).id].sendMessage("Your rename request to \"$newName\" was accepted by an exco member.")
                            channelClient.sendMessage(
                                "<@!${messageReaction.userId}> has accepted ${
                                    guildClient.getMember(
                                        channelClient.getMessage(messageReaction.messageId).usersMentioned[0].id
                                    ).nickname
                                }(<@!${channelClient.getMessage(messageReaction.messageId).usersMentioned[0].id}>)'s request to change name to \"$newName\""
                            )
                            guildClient.changeNickname(
                                channelClient.getMessage(
                                    messageReaction.messageId
                                ).usersMentioned[0].id,
                                newName
                            )
                            channelClient.getMessage(messageReaction.messageId).delete()
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }
    }
}