package com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote

import android.util.Log
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryChatPage
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.parser.StrawberryChatParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request

object StrawberryChatRepo {

    private const val TAG = "StrawberryChatRepo"

    sealed interface ChatPostResult {
        data object Success : ChatPostResult
        data class FormError(val message: String) : ChatPostResult
        data class NetworkError(val throwable: Throwable) : ChatPostResult
        data object LoginRequired : ChatPostResult
    }

    suspend fun fetchChatPage(page: Int = 1): Result<StrawberryChatPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base = StrawberryNetwork.baseUrl.trimEnd('/')
                val url = if (page <= 1) "$base/chat" else "$base/chat?page=$page"
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "text/html")
                    .header("Referer", "$base/chat")
                    .build()
                StrawberryNetwork.sharedOkHttpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Chat load failed (HTTP ${resp.code})")
                    }
                    val html = resp.body.string().orEmpty()
                    if (StrawberryChatParser.detectLoginRequired(html)) {
                        throw IllegalStateException("Your session expired. Please sign in again.")
                    }
                    StrawberryChatParser.parseChatPage(html, base)
                        ?: throw IllegalStateException("Could not parse the chat page.")
                }
            }
        }

    suspend fun postChatMessage(body: String): ChatPostResult =
        withContext(Dispatchers.IO) {
            val trimmed = body.trim()
            if (trimmed.isBlank()) {
                return@withContext ChatPostResult.FormError("Message cannot be empty.")
            }
            try {
                val base = StrawberryNetwork.baseUrl.trimEnd('/')
                val chatUrl = "$base/chat"

                val chatPage = fetchChatPage(1).getOrElse { e ->
                    Log.e(TAG, "Failed to fetch chat page for CSRF", e)
                    return@withContext ChatPostResult.NetworkError(e)
                }

                val form = FormBody.Builder()
                    .add("csrf_token", chatPage.csrfToken)
                    .add("next", "/chat")
                    .add("body", trimmed)
                    .build()

                val request = Request.Builder()
                    .url("$base/chat/send")
                    .header("Accept", "text/html")
                    .header("Referer", chatUrl)
                    .header("Origin", base)
                    .post(form)
                    .build()

                StrawberryNetwork.sharedOkHttpClient.newCall(request).execute().use { resp ->
                    val code = resp.code
                    val finalHtml = resp.body.string().orEmpty()

                    if (code == 401 || code == 403) {
                        return@withContext ChatPostResult.LoginRequired
                    }
                    if (code == 429 || StrawberryChatParser.detectRateLimited(finalHtml)) {
                        return@withContext ChatPostResult.FormError(
                            "You are sending messages too fast. Wait a moment and try again."
                        )
                    }
                    if (code !in 200..399) {
                        return@withContext ChatPostResult.FormError(
                            "Failed to send (HTTP $code). Try again."
                        )
                    }

                    if (StrawberryChatParser.detectLoginRequired(finalHtml)) {
                        return@withContext ChatPostResult.LoginRequired
                    }

                    val parsed = runCatching {
                        StrawberryChatParser.parseChatPage(finalHtml, base)
                    }.getOrNull()

                    val serverError = parsed?.formError
                    if (!serverError.isNullOrBlank()) {
                        return@withContext ChatPostResult.FormError(serverError)
                    }

                    ChatPostResult.Success
                }
            } catch (e: Exception) {
                Log.e(TAG, "postChatMessage failed", e)
                ChatPostResult.NetworkError(e)
            }
        }
}
