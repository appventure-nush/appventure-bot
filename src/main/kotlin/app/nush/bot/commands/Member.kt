package app.nush.bot.commands

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Member(val ID: String, val Email: String, val Name: String, val MentorGroup: String) {
    companion object {
        val members: List<Member> = Json.parse(serializer().list, File("data/members.json").readText())
    }
}