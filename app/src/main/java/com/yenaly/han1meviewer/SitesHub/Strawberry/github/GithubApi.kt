package com.yenaly.han1meviewer.SitesHub.Strawberry.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

object GithubApi {

    private const val API = "https://api.github.com"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    class GithubException(message: String) : IOException(message)

    private fun req(url: String, token: String, accept: String = "application/vnd.github+json") =
        Request.Builder().url(url)
            .header("Accept", accept)
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "HANiEX-GithubDownloader")
            .apply { if (token.isNotBlank()) header("Authorization", "Bearer $token") }

    private fun explain(code: Int, body: String): String {
        val msg = runCatching { JSONObject(body).optString("message") }.getOrDefault("")
        val hint = when (code) {
            401 -> "token invalid or expired"
            403 -> "token lacks permission (needs Contents: read & write) or rate limited"
            404 -> "repo/branch not found, or token cannot see this repo"
            409 -> "repo is empty"
            422 -> "GitHub rejected the request"
            else -> ""
        }
        return "HTTP $code" + (if (hint.isNotEmpty()) " - $hint" else "") +
                (if (msg.isNotBlank()) " ($msg)" else "")
    }

    private fun call(request: Request): String =
        client.newCall(request).execute().use { r ->
            val body = r.body.string()
            if (!r.isSuccessful) throw GithubException(explain(r.code, body))
            body
        }

    suspend fun defaultBranch(owner: String, repo: String, token: String): String =
        withContext(Dispatchers.IO) {
            JSONObject(call(req("$API/repos/$owner/$repo", token).build()))
                .optString("default_branch", "main")
        }

    suspend fun downloadRepo(
        owner: String, repo: String, branch: String, token: String, dest: File,
    ): Int = withContext(Dispatchers.IO) {
        val request = req("$API/repos/$owner/$repo/zipball/$branch", token).build()
        var count = 0
        client.newCall(request).execute().use { r ->
            if (!r.isSuccessful) throw GithubException(explain(r.code, r.body.string()))
            if (dest.exists()) dest.deleteRecursively()
            dest.mkdirs()
            val canonicalDest = dest.canonicalPath
            ZipInputStream(r.body.byteStream()).use { zin ->
                var e = zin.nextEntry
                while (e != null) {
                    val rel = e.name.substringAfter('/', "")
                    if (rel.isNotEmpty()) {
                        val out = File(dest, rel)
                        if (!out.canonicalPath.startsWith(canonicalDest)) {
                            throw GithubException("Unsafe path in archive")
                        }
                        if (e.isDirectory) out.mkdirs() else {
                            out.parentFile?.mkdirs()
                            out.outputStream().use { zin.copyTo(it) }
                            count++
                        }
                    }
                    zin.closeEntry()
                    e = zin.nextEntry
                }
            }
        }
        count
    }

    suspend fun fetchText(
        owner: String, repo: String, branch: String, token: String, path: String,
    ): String? = withContext(Dispatchers.IO) {
        val url = "$API/repos/$owner/$repo/contents/$path?ref=$branch"
        client.newCall(req(url, token, "application/vnd.github.raw+json").build()).execute().use { r ->
            val body = r.body.string()
            when {
                r.code == 404 -> null
                !r.isSuccessful -> throw GithubException(explain(r.code, body))
                else -> body
            }
        }
    }

    private fun post(url: String, token: String, json: JSONObject, method: String = "POST"): JSONObject {
        val b = json.toString().toRequestBody(JSON)
        val builder = req(url, token)
        val request = if (method == "PATCH") builder.patch(b).build() else builder.post(b).build()
        return JSONObject(call(request))
    }

    suspend fun push(
        owner: String, repo: String, branch: String, token: String,
        files: Map<String, String>, message: String,
        authorName: String, authorEmail: String,
    ): String = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "Token required" }
        val ref = JSONObject(call(req("$API/repos/$owner/$repo/git/ref/heads/$branch", token).build()))
        val headSha = ref.getJSONObject("object").getString("sha")
        val headCommit = JSONObject(call(req("$API/repos/$owner/$repo/git/commits/$headSha", token).build()))
        val baseTree = headCommit.getJSONObject("tree").getString("sha")

        val tree = JSONArray()
        for ((path, content) in files) {
            val blob = post(
                "$API/repos/$owner/$repo/git/blobs", token,
                JSONObject().put("content", content).put("encoding", "utf-8"),
            )
            tree.put(
                JSONObject().put("path", path).put("mode", "100644")
                    .put("type", "blob").put("sha", blob.getString("sha"))
            )
        }
        val newTree = post(
            "$API/repos/$owner/$repo/git/trees", token,
            JSONObject().put("base_tree", baseTree).put("tree", tree),
        )
        val commitBody = JSONObject()
            .put("message", message)
            .put("tree", newTree.getString("sha"))
            .put("parents", JSONArray().put(headSha))
        if (authorName.isNotBlank() && authorEmail.isNotBlank()) {
            commitBody.put(
                "author",
                JSONObject().put("name", authorName).put("email", authorEmail),
            )
        }
        val commit = post("$API/repos/$owner/$repo/git/commits", token, commitBody)
        post(
            "$API/repos/$owner/$repo/git/refs/heads/$branch", token,
            JSONObject().put("sha", commit.getString("sha")), method = "PATCH",
        )
        commit.optString("html_url").ifBlank { "https://github.com/$owner/$repo/commit/${commit.getString("sha")}" }
    }
}
