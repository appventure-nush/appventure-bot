package app.nush.bot.server

import com.junron.pyrobase.jsoncache.IndexableItem
import com.junron.pyrobase.jsoncache.Storage
import kotlinx.serialization.Serializable
import java.util.*

val pendingRequests = Storage("pendingRequests", PendingVerify.serializer())
val pendingGithubRequests =
    Storage("pendingGithubRequests", PendingVerify.serializer())

@Serializable
data class PendingVerify(
    override val id: String = UUID.randomUUID().toString(),
    val discordUserId: String,
    val timestamp: Long
) : IndexableItem
