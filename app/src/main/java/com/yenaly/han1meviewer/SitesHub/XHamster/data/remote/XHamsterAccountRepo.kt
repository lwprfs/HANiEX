package com.yenaly.han1meviewer.SitesHub.XHamster.data.remote

import com.yenaly.han1meviewer.SitesHub.XHamster.common.XHamsterConstants
import com.yenaly.han1meviewer.SitesHub.XHamster.data.parser.XHamsterParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID

object XHamsterAccountRepo {

    data class Choice(val value: String, val label: String)

    data class PrivacyField(
        val key: String,
        val title: String,
        val value: String,
        val choices: List<Choice>,
    )

    data class PrivacyForm(val raw: JsonObject, val fields: List<PrivacyField>)

    data class ProfileForm(
        val raw: JsonObject,
        val userId: Long?,
        val username: String,
        val usernameLocked: Boolean,
        val fullName: String,
        val city: String,
        val occupation: String,
        val about: String,
        val gender: String?,
        val orientation: String?,
        val relation: String?,
        val genderChoices: List<Choice>,
        val orientationChoices: List<Choice>,
        val relationChoices: List<Choice>,
        val email: String?,
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TEXT_TYPE = "text/plain;charset=UTF-8".toMediaType()

    @Volatile
    private var cachedUserId: Long? = null

    private fun base() = XHamsterConstants.BASE_URL

    private fun requireLogin() {
        if (!XHamsterNetwork.hasAuthCookies()) throw IOException("LOGIN_REQUIRED")
    }

    private fun fetchHtml(path: String): String {
        val request = Request.Builder().url(base() + path)
            .header("Accept", "text/html,application/xhtml+xml,*/*")
            .header("Referer", base() + "/")
            .get().build()
        return XHamsterNetwork.sharedOkHttpClient.newCall(request).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
            r.body.string()
        }
    }

    private fun sync(payload: String, referer: String): JsonObject? {
        val request = Request.Builder().url("${base()}/x-api")
            .header("Accept", "*/*")
            .header("Origin", XHamsterConstants.ORIGIN)
            .header("Referer", referer)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("x-csrf-token", "1")
            .post(payload.toRequestBody(TEXT_TYPE)).build()
        val body = XHamsterNetwork.sharedOkHttpClient.newCall(request).execute().use { r ->
            r.body.string()
        }
        return (runCatching { json.parseToJsonElement(body) }.getOrNull() as? JsonArray)
            ?.firstOrNull() as? JsonObject
    }

    private fun checkSync(response: JsonObject?, fallback: String) {
        val extras = response?.get("extras") as? JsonObject
        val ok = (extras?.get("result") as? JsonPrimitive)?.booleanOrNull
        if (ok == true) return
        val message = (extras?.get("errorMessage") as? JsonPrimitive)?.contentOrNull
            ?: ((extras?.get("error") as? JsonObject)?.get("message") as? JsonPrimitive)?.contentOrNull
            ?: ((response?.get("errors") as? JsonArray)?.firstOrNull() as? JsonObject)
                ?.get("message")?.let { (it as? JsonPrimitive)?.contentOrNull }
        throw IOException(message ?: fallback)
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull
            ?.takeIf { it.isNotBlank() }

    private fun JsonObject.obj(key: String) = this[key] as? JsonObject

    private fun JsonObject.arr(key: String) = this[key] as? JsonArray

    private fun humanize(key: String): String =
        key.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() }

    suspend fun currentUserId(): Long? {
        cachedUserId?.let { return it }
        if (!XHamsterNetwork.hasAuthCookies()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val root = XHamsterParser.extractInitials(fetchHtml("/my/edit/privacy"))
                (root?.get("userId") as? JsonPrimitive)?.longOrNull
            }.getOrNull()
        }?.also { cachedUserId = it }
    }

    fun clearCache() {
        cachedUserId = null
    }

    private val PRIVACY_TITLES = mapOf(
        "pm" to "Who can message me",
        "promo" to "Promotional messages",
        "performerCollaboration" to "Collaboration requests",
        "acceptProfileComments" to "Who can comment on my profile",
        "acceptContentComments" to "Who can comment on my content",
        "acceptDownloadShortVideos" to "Who can download my shorts",
    )

    suspend fun loadPrivacy(): Result<PrivacyForm> = withContext(Dispatchers.IO) {
        runCatching {
            requireLogin()
            val html = fetchHtml("/my/edit/privacy")
            val root = XHamsterParser.extractInitials(html)
                ?: throw IOException("Unable to read privacy settings")
            (root["userId"] as? JsonPrimitive)?.longOrNull?.let { cachedUserId = it }
            val settings = root.obj("userPrivacySettings")
                ?: throw IOException("Unable to read privacy settings")
            val fields = ArrayList<PrivacyField>()
            val general = settings.arr("privateSettings")
            for (key in listOf(
                "pm", "promo", "performerCollaboration",
                "acceptProfileComments", "acceptContentComments", "acceptDownloadShortVideos",
            )) {
                if (!settings.containsKey(key)) continue
                val list = settings.arr(key + "List") ?: if (key == "pm") general else null
                val choices = list.orEmpty().mapNotNull { el ->
                    val o = el as? JsonObject ?: return@mapNotNull null
                    Choice(o.str("value") ?: return@mapNotNull null, o.str("text") ?: o.str("value")!!)
                }
                if (choices.isEmpty()) continue
                val current = settings.str(key) ?: choices.first().value
                fields += PrivacyField(key, PRIVACY_TITLES[key] ?: humanize(key), current, choices)
            }
            PrivacyForm(settings, fields)
        }
    }

    suspend fun savePrivacy(form: PrivacyForm, values: Map<String, String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireLogin()
                val model = buildJsonObject {
                    put("id", JsonNull)
                    put("${'$'}id", UUID.randomUUID().toString())
                    put("modelName", "userPrivacySettingsModel")
                    put("itemState", "changed")
                    form.raw.forEach { (k, v) ->
                        put(k, if (values.containsKey(k)) JsonPrimitive(values.getValue(k)) else v)
                    }
                }
                val payload = buildJsonArray {
                    add(buildJsonObject {
                        put("name", "userPrivacySettingsModelSync")
                        put("requestData", buildJsonObject { put("model", model) })
                    })
                }.toString()
                checkSync(sync(payload, base() + "/my/edit/privacy"), "Could not save privacy settings")
            }
        }

    suspend fun changePassword(current: String, new: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireLogin()
                val payload = buildJsonArray {
                    add(buildJsonObject {
                        put("name", "userPasswordSettingsModelSync")
                        put("requestData", buildJsonObject {
                            put("model", buildJsonObject {
                                put("id", JsonNull)
                                put("${'$'}id", UUID.randomUUID().toString())
                                put("modelName", "userPasswordSettingsModel")
                                put("itemState", "created")
                                put("password", current)
                                put("newPassword", new)
                            })
                        })
                    })
                }.toString()
                checkSync(sync(payload, base() + "/my/edit/password"), "Could not change password")
            }
        }

    private fun choicesOf(el: JsonElement?): List<Choice> =
        (el as? JsonObject)?.mapNotNull { (k, v) ->
            (v as? JsonPrimitive)?.contentOrNull?.let { Choice(k, it) }
        }.orEmpty()

    suspend fun loadProfile(): Result<ProfileForm> = withContext(Dispatchers.IO) {
        runCatching {
            requireLogin()
            val root = XHamsterParser.extractInitials(fetchHtml("/my/edit/profile"))
                ?: throw IOException("Unable to read profile")
            val user = root.obj("displayUserModel") ?: throw IOException("Unable to read profile")
            val personal = user.obj("personalInfo")
            val fields = root.obj("fieldValues")
            val userId = (user["id"] as? JsonPrimitive)?.longOrNull
            cachedUserId = userId ?: cachedUserId
            val relationChoices = listOf(
                Choice("single", "Single"), Choice("taken", "Taken"), Choice("open", "Open"),
            )
            ProfileForm(
                raw = user,
                userId = userId,
                username = personal?.str("username") ?: user.str("name").orEmpty(),
                usernameLocked = (user["isUsernameChangeDisabled"] as? JsonPrimitive)?.booleanOrNull == true,
                fullName = personal?.str("fullName").orEmpty(),
                city = personal?.obj("geo")?.str("city").orEmpty(),
                occupation = personal?.str("occupation").orEmpty(),
                about = user.str("about").orEmpty(),
                gender = personal?.obj("gender")?.str("name"),
                orientation = personal?.obj("orientation")?.str("name"),
                relation = personal?.obj("relations")?.str("name"),
                genderChoices = choicesOf(fields?.get("seeking")).filter {
                    it.value in setOf("male", "female", "trans_mtf", "trans_ftm", "genderqueer",
                        "intersex", "non_binary", "couple", "male_couple", "female_couple")
                },
                orientationChoices = choicesOf(fields?.get("orientations")),
                relationChoices = relationChoices,
                email = root.obj("editEmailContainerComponent")?.str("mainEmail"),
            )
        }
    }

    suspend fun saveProfile(
        form: ProfileForm,
        username: String,
        fullName: String,
        city: String,
        occupation: String,
        gender: String?,
        orientation: String?,
        relation: String?,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireLogin()
            fun label(choices: List<Choice>, value: String?) =
                choices.firstOrNull { it.value == value }?.label ?: value.orEmpty()

            fun named(old: JsonObject?, value: String?, choices: List<Choice>): JsonElement =
                if (value == null) old ?: JsonNull
                else buildJsonObject {
                    put("label", label(choices, value))
                    put("name", value)
                }

            val oldPersonal = form.raw.obj("personalInfo") ?: JsonObject(emptyMap())
            val geo = buildJsonObject {
                (oldPersonal.obj("geo") ?: JsonObject(emptyMap())).forEach { (k, v) -> put(k, v) }
                put("city", city)
            }
            val personal = buildJsonObject {
                oldPersonal.forEach { (k, v) -> put(k, v) }
                put("username", username)
                put("fullName", fullName)
                put("occupation", occupation)
                put("geo", geo)
                put("gender", named(oldPersonal.obj("gender"), gender, form.genderChoices))
                put("orientation", named(oldPersonal.obj("orientation"), orientation, form.orientationChoices))
                put("relations", named(oldPersonal.obj("relations"), relation, form.relationChoices))
            }
            val model = buildJsonObject {
                form.raw.forEach { (k, v) -> put(k, v) }
                put("${'$'}id", UUID.randomUUID().toString())
                put("modelName", "userModel")
                put("itemState", "changed")
                put("personalInfo", personal)
            }
            val payload = buildJsonArray {
                add(buildJsonObject {
                    put("name", "userModelSync")
                    put("requestData", buildJsonObject { put("model", model) })
                })
            }.toString()
            checkSync(sync(payload, base() + "/my/edit/profile"), "Could not save profile")
        }
    }
}
