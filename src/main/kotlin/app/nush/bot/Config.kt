package app.nush.bot
import com.bettercloud.vault.Vault
import com.bettercloud.vault.VaultConfig
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Config(
    val discordToken: String = "",
    val projectsCategoryId: String = "",
    val botPrefix: String = "!",
    val guildId: String = "",
    val memberRole: String = "",
    val excoRole: String = "",
    val dev: Boolean = true,
    val alumniRole: String = "",
    val guestRole: String = "",
    val excoChannelId: String = "",
    val githubToken: String = "",
    val githubClientId: String = "",
    val githubClientSecret: String = ""
) {
    companion object {
        val config: Config by lazy {
            val configFile = File("config.json")
            val config = if (configFile.exists()) {
                Json.parse(serializer(), configFile.readText())
            } else {
                val token = getToken()
                val vault = Vault(
                    VaultConfig()
                        .engineVersion(2)
                        .address("https://vault.nush.app")
                        .token(token)
                        .build()
                )
                val secrets = vault
                    .logical()
                    .read("apps/appventure-bot").data
                ObjectMapper().convertValue(secrets, Config::class.java)
            }
            config.copy(botPrefix = if (config.dev) "dev" + config.botPrefix else config.botPrefix)
        }

        private fun getToken(): String {
            val vault = Vault(
                VaultConfig()
                    .engineVersion(2)
                    .address("https://vault.nush.app")
                    .build()
            )
            val password = System.getenv("VAULT_PASSWORD")
            val response = vault.auth().loginByUserPass(
                "appventure-bot",
                password
            )
            return response.authClientToken
        }

    }
}
