package com.yenaly.han1meviewer.SitesHub.OnAir.github

import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object GithubDownloaderRepo {

    private fun target(): Triple<String, String, String> {
        val (owner, repo) = GithubDownloaderStore.parseRepo(GithubDownloaderStore.repoUrl)
            ?: throw GithubApi.GithubException("Invalid repo link")
        val branch = GithubDownloaderStore.branch.ifBlank { "main" }
        return Triple(owner, repo, branch)
    }

    suspend fun setUp(): Int {
        val (owner, repo) = GithubDownloaderStore.parseRepo(GithubDownloaderStore.repoUrl)
            ?: throw GithubApi.GithubException("Invalid repo link")
        val token = GithubDownloaderStore.token
        val branch = GithubDownloaderStore.branch.ifBlank {
            GithubApi.defaultBranch(owner, repo, token).also { GithubDownloaderStore.branch = it }
        }
        val count = GithubApi.downloadRepo(owner, repo, branch, token, GithubDownloaderStore.repoDir())
        GithubDownloaderStore.repoReady = true
        GithubDownloaderStore.lastSync = System.currentTimeMillis()
        GithubDownloaderStore.reload()
        return count
    }

    suspend fun refresh(): Int = setUp()

    data class PushResult(val commitUrl: String, val links: Int, val cookies: Int, val newDoneIds: Int)

    suspend fun push(note: String): PushResult {
        val (owner, repo, branch) = target()
        val token = GithubDownloaderStore.token
        if (token.isBlank()) throw GithubApi.GithubException("A GitHub token is required to push")

        val cookies = GithubDownloaderStore.cookiesAsJson()
            ?: throw GithubApi.GithubException("No OnAir cookies in the Cookies section - sign in first")
        val cookieCount = runCatching { org.json.JSONArray(cookies).length() }.getOrDefault(0)

        val remoteLinks = GithubApi.fetchText(owner, repo, branch, token, GithubDownloaderStore.LINKS_FILE)
            .orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
        val remoteDoneLines = GithubApi.fetchText(owner, repo, branch, token, GithubDownloaderStore.DOWNLOADED_IDS_FILE)
            .orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
        val remoteDone = remoteDoneLines.toSet()

        val appDone = OnAirDownloadedIds.collect(applicationContext)
        val newDone = appDone.filter { it !in remoteDone }.sortedBy { it.toLongOrNull() ?: Long.MAX_VALUE }
        val allDone = remoteDone + newDone

        val merged = LinkedHashMap<String, String>()
        (remoteLinks + GithubDownloaderStore.readLinks()).forEach { line ->
            val id = GithubDownloaderStore.extractId(line) ?: return@forEach
            if (id !in allDone) merged.putIfAbsent(id, line)
        }
        if (merged.isEmpty()) {
            throw GithubApi.GithubException("Nothing to download - every queued video is already downloaded")
        }

        val files = LinkedHashMap<String, String>()
        if (newDone.isNotEmpty()) {
            files[GithubDownloaderStore.DOWNLOADED_IDS_FILE] =
                (remoteDoneLines + newDone).joinToString("\n") + "\n"
        }
        files[GithubDownloaderStore.LINKS_FILE] = merged.values.joinToString("\n") + "\n"
        files[GithubDownloaderStore.COOKIES_FILE] = cookies + "\n"

        val message = buildString {
            append(GithubDownloaderStore.COMMIT_TAG).append(' ')
            append(note.trim().ifBlank { "download ${merged.size} video(s)" })
        }
        val url = GithubApi.push(
            owner, repo, branch, token, files, message,
            GithubDownloaderStore.authorName, GithubDownloaderStore.authorEmail,
        )

        withContext(Dispatchers.IO) {
            files.forEach { (path, text) ->
                val f = File(GithubDownloaderStore.repoDir(), path)
                f.parentFile?.mkdirs()
                f.writeText(text)
            }
        }
        GithubDownloaderStore.writeLinks(emptyList())
        return PushResult(url, merged.size, cookieCount, newDone.size)
    }
}
