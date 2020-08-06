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
    val excoRole: String
) {
    companion object {
        val config =
            Json.parse(serializer(), File("config.json").readText())
    }
}
