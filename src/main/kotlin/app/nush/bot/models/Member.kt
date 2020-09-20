package app.nush.bot.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Member(val id: String, val email: String, val name: String, val mentorGroup: String) {
    companion object {
        val members: List<Member> = Json.parse(serializer().list, File("data/members.json").readText())
    }
}
