package com.yenaly.han1meviewer.util.offline

import androidx.core.content.edit
import com.yenaly.han1meviewer.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

data class OfflineGroup(val id: Int, val name: String)

data class OfflineGroupsState(
    val groups: List<OfflineGroup> = emptyList(),
    val assignments: Map<String, Int> = emptyMap(),
) {
    fun groupOf(key: String): Int = assignments[key]?.takeIf { id -> groups.any { it.id == id } } ?: OfflineGroups.DEFAULT_ID
}

object OfflineGroups {

    const val DEFAULT_ID = 1

    private val flows = HashMap<OfflineKind, MutableStateFlow<OfflineGroupsState>>()

    private fun prefKey(kind: OfflineKind) = "offline_groups_${kind.name.lowercase()}"

    @Synchronized
    private fun flow(kind: OfflineKind): MutableStateFlow<OfflineGroupsState> =
        flows.getOrPut(kind) { MutableStateFlow(load(kind)) }

    fun state(kind: OfflineKind): StateFlow<OfflineGroupsState> = flow(kind).asStateFlow()

    private fun load(kind: OfflineKind): OfflineGroupsState {
        val raw = Preferences.preferenceSp.getString(prefKey(kind), null)
        val groups = ArrayList<OfflineGroup>()
        val assign = HashMap<String, Int>()
        runCatching {
            if (raw != null) {
                val o = Json.parseToJsonElement(raw).jsonObject
                o["groups"]?.jsonArray?.forEach { e ->
                    val g = e.jsonObject
                    val id = (g["id"] as? JsonPrimitive)?.intOrNull
                    val name = (g["name"] as? JsonPrimitive)?.contentOrNull
                    if (id != null && !name.isNullOrBlank() && id != DEFAULT_ID) groups += OfflineGroup(id, name)
                }
                o["assign"]?.jsonObject?.forEach { (k, v) ->
                    (v as? JsonPrimitive)?.intOrNull?.let { assign[k] = it }
                }
            }
        }

        return OfflineGroupsState(listOf(OfflineGroup(DEFAULT_ID, "")) + groups, assign)
    }

    private fun save(kind: OfflineKind, state: OfflineGroupsState) {
        val text = buildJsonObject {
            put("groups", buildJsonArray {
                state.groups.filter { it.id != DEFAULT_ID }.forEach { g ->
                    add(buildJsonObject {
                        put("id", g.id)
                        put("name", g.name)
                    })
                }
            })
            put("assign", JsonObject(state.assignments.mapValues { JsonPrimitive(it.value) }))
        }.toString()
        Preferences.preferenceSp.edit { putString(prefKey(kind), text) }
    }

    @Synchronized
    private fun update(kind: OfflineKind, block: (OfflineGroupsState) -> OfflineGroupsState) {
        val f = flow(kind)
        val next = block(f.value)
        f.value = next
        save(kind, next)
    }

    fun create(kind: OfflineKind, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        update(kind) { s ->
            val nextId = (s.groups.maxOfOrNull { it.id } ?: DEFAULT_ID) + 1
            s.copy(groups = s.groups + OfflineGroup(nextId, clean))
        }
    }

    fun rename(kind: OfflineKind, id: Int, name: String) {
        val clean = name.trim()
        if (clean.isEmpty() || id == DEFAULT_ID) return
        update(kind) { s -> s.copy(groups = s.groups.map { if (it.id == id) it.copy(name = clean) else it }) }
    }

    fun delete(kind: OfflineKind, id: Int) {
        if (id == DEFAULT_ID) return
        update(kind) { s ->
            s.copy(
                groups = s.groups.filterNot { it.id == id },
                assignments = s.assignments.filterValues { it != id },
            )
        }
    }

    fun move(kind: OfflineKind, keys: Collection<String>, groupId: Int) {
        update(kind) { s ->
            val next = s.assignments.toMutableMap()
            keys.forEach { if (groupId == DEFAULT_ID) next.remove(it) else next[it] = groupId }
            s.copy(assignments = next)
        }
    }

    fun forget(kind: OfflineKind, keys: Collection<String>) {
        if (keys.isEmpty()) return
        update(kind) { s -> s.copy(assignments = s.assignments - keys.toSet()) }
    }

    fun rekey(kind: OfflineKind, oldKey: String, newKey: String) {
        update(kind) { s ->
            val id = s.assignments[oldKey] ?: return@update s
            s.copy(assignments = (s.assignments - oldKey) + (newKey to id))
        }
    }
}
