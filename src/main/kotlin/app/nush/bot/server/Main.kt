package app.nush.bot.server

import app.nush.bot.Config.Companion.config
import app.nush.bot.commands.GithubVerify
import app.nush.bot.commands.Verify
import com.junron.pyrobase.msauth.Either
import com.junron.pyrobase.msauth.User
import com.junron.pyrobase.msauth.verify
import com.junron.pyrobase.msauth.verifyRaw
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.http.Parameters
import io.ktor.http.parseUrlEncodedParameters
import io.ktor.request.receiveParameters
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.kohsuke.github.GitHubBuilder

fun startServer() {
    embeddedServer(Netty, 3000, module = Application::module).start(wait = true)
}

val client = HttpClient(CIO)

fun Application.module() {
    routing {
        post("/") {
            val params = call.receiveParameters()
            val token = params["id_token"] ?: return@post call.respondText {
                "No token specified"
            }
            val user = when (val result = verify(token)) {
                is Either.Left<User> -> {
                    result.value
                }
                is Either.Right<String> -> {
                    call.respondText { "Error: ${result.value}" }
                    null
                }
            } ?: return@post

            if (user.exp < System.currentTimeMillis() / 1000) {
                call.respondText { "Error: Token expired" }
                return@post
            }
            val fullToken = (verifyRaw(token) as Either.Left).value
            val requestId = fullToken["nonce"] ?: return@post call.respondText {
                "Nonce not found"
            }
            val request =
                pendingRequests.find { it.id == requestId }
                    ?: return@post call.respondText {
                        "Request not found"
                    }
            pendingRequests -= request.id
            if (System.currentTimeMillis() - request.timestamp > 3.6e6) {
                return@post call.respondText {
                    "Request expired"
                }
            }
            Verify.userVerified(user, request.discordUserId)
            call.respondText {
                "You have been verified. You may close this page now."
            }
        }
        get("/github") {
            val code = call.request.queryParameters["code"]
                ?: return@get call.respondText {
                    "No code provided"
                }
            val state = call.request.queryParameters["state"]
                ?: return@get call.respondText {
                    "No state provided"
                }
            val request = pendingGithubRequests.find { it.id == state }
                ?: return@get call.respondText {
                    "Request could not be found"
                }
            val response =
                client.post<String>("https://github.com/login/oauth/access_token") {
                    body = FormDataContent(
                        Parameters.build {
                            append("client_id", config.githubClientId)
                            append(
                                "client_secret", config.githubClientSecret
                            )
                            append("code", code)
                            append("state", state)
                        }
                    )
                }.parseUrlEncodedParameters()
            pendingGithubRequests -= request.id
            if ("error" in response) {
                call.respondText {
                    "Error: ${
                        response["error_description"].toString()
                            .replace("+", " ")
                    }"
                }
            } else {
                val token =
                    response["access_token"] ?: return@get call.respondText {
                        "Error: malformed response"
                    }
                val user = GitHubBuilder().withOAuthToken(token).build().myself
                GithubVerify.userVerified(user, request.discordUserId)
                call.respondText {
                    "Welcome, ${user.login}! You may close this page now."
                }
            }
        }
    }
}
