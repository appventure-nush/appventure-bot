package app.nush.bot.commands

import app.nush.bot.Config.Companion.config
import app.nush.bot.DB
import app.nush.bot.commands.utils.dmUser
import app.nush.bot.email
import app.nush.bot.server.PendingVerify
import app.nush.bot.server.pendingGithubRequests
import com.jessecorbett.diskord.api.rest.CreateDM
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.sendMessage
import org.kohsuke.github.GHUser

object GithubVerify : Command {
    lateinit var bot: Bot
    override fun init(bot: Bot, prefix: CommandSet) {
        this.bot = bot
        with(bot) {
            with(prefix) {
                command("verify") {
                    val channel =
                        clientStore.discord.createDM(CreateDM(authorId))
                    val dbUser = DB.getMemberByDiscordId(authorId.toLong())
                    println(dbUser)
                    if (dbUser == null) {
                        clientStore.channels[channel.id].sendMessage("Please verify your Office 365 account first by typing `!sendverify`")
                        return@command
                    }
                    val request = PendingVerify(
                        discordUserId = authorId,
                        timestamp = System.currentTimeMillis()
                    )
                    pendingGithubRequests += request
                    val url =
                        "https://github.com/login/oauth/authorize?client_id=${config.githubClientId}&state=${request.id}"
                    clientStore.channels[channel.id].sendMessage("") {
                        title = "GitHub Verification"
                        description = """
                            Please click [here]($url) to sign into and verify your GitHub account
                        """.trimIndent()
                        color = Colors.GREEN
                    }
                }
            }
        }
    }

    suspend fun userVerified(user: GHUser, discordId: String) {
        bot.dmUser(
            discordId,
            "Your GitHub account ${user.login} has been linked."
        )
        val dbUser = DB.getMemberByDiscordId(discordId.toLong()) ?: return
        DB.setGithub(dbUser.email, user.login)
    }
}
