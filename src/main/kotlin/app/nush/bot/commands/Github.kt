package app.nush.bot.commands

import app.nush.bot.Config.Companion.config
import app.nush.bot.DB
import app.nush.bot.commands.utils.dmUser
import app.nush.bot.commands.utils.isExco
import app.nush.bot.email
import app.nush.bot.githubUsername
import app.nush.bot.name
import app.nush.bot.server.PendingVerify
import app.nush.bot.server.pendingGithubRequests
import com.jessecorbett.diskord.api.rest.CreateDM
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.sendMessage
import com.jessecorbett.diskord.util.words
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHUser
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

object Github : Command {
    val github: GitHub =
        GitHubBuilder().withOAuthToken(config.githubToken)
            .build()
    val org: GHOrganization = github
        .getOrganization("appventure-nush")


    lateinit var bot: Bot
    override fun init(bot: Bot, prefix: CommandSet) {
        this.bot = bot
        with(bot) {
            with(prefix) {
                command("verify") {
                    val channel =
                        clientStore.discord.createDM(CreateDM(authorId))
                    val dbUser = DB.getMemberByDiscordId(authorId.toLong())
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
                command("share") {
                    val guildId = guildId ?: return@command
                    if (!isExco(authorId, guildId, bot)) {
                        reply("You are not authorized")
                        return@command
                    }
                    if (words.size < 4) {
                        reply("Specify repo name and user")
                        return@command
                    }
                    if (usersMentioned.isEmpty()) {
                        reply("You must mention at least one member.")
                        return@command
                    }
                    val members = usersMentioned.mapNotNull {
                        DB.getMemberByDiscordId(it.id.toLong()) ?: run {
                            reply("Member \"${it.username}\" could not be found")
                            null
                        }
                    }
                    if (members.size != usersMentioned.size) {
                        return@command
                    }
                    val usernames = members.mapNotNull {
                        it.githubUsername ?: run {
                            reply("\"${it.name}\" has not registered their GitHub account.")
                            null
                        }
                    }
                    if (usernames.size != usersMentioned.size) {
                        return@command
                    }
                    val repoName = words[2]
                    val repo = org.getRepository(repoName) ?: run {
                        reply("Repo $repoName could not be found")
                        return@command
                    }
                    val users = usernames.mapNotNull {
                        github.getUser(it) ?: run {
                            reply("$it is not a GitHub user")
                            null
                        }
                    }
                    if (users.size != usernames.size) {
                        return@command
                    }
                    repo.addCollaborators(users,
                        GHOrganization.Permission.MAINTAIN)
                    reply("Added \"${usernames.joinToString(";")}\" to $repoName")
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
