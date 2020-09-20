package app.nush.bot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Config(
    val discordToken: String,
    val projectsCategoryId: String,
    val botPrefix: String = "!",
    val guildId: String,
    val memberRole: String,
    val excoRole: String,
    val dev: Boolean = true,
    val alumniRole: String,
    val guestRole: String,
    val excoChannelId: String,
    val githubToken: String
) {
    companion object {

        private val _config =
            Json.parse(serializer(), File("config.json").readText())
        val config =
            _config.copy(botPrefix = if (_config.dev) "dev" + _config.botPrefix else _config.botPrefix)

    }
}
