package com.nexadev.app.fs

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Gestionnaire de projets : persistance, historique de sessions, reprise. */
class ProjectStore(private val ctx: Context) {
    private val dir = File(ctx.filesDir, "projects").apply { mkdirs() }

    data class Project(val id: String, val name: String, val path: String, val lastOpened: Long)

    fun create(name: String): Project {
        val id = System.currentTimeMillis().toString()
        val pdir = File(dir, "${name.replace(Regex("[^A-Za-z0-9_-]"), "_")}_$id").apply { mkdirs() }
        File(pdir, "README.md").writeText("# $name\n\nProjet créé avec NexaDev.\n")
        val p = Project(id, name, pdir.absolutePath, System.currentTimeMillis())
        save(p); return p
    }

    fun list(): List<Project> {
        val f = File(ctx.filesDir, "projects.json")
        if (!f.exists()) return emptyList()
        val arr = JSONArray(f.readText())
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Project(o.getString("id"), o.getString("name"), o.getString("path"), o.getLong("lastOpened"))
        }.sortedByDescending { it.lastOpened }
    }

    fun save(p: Project) {
        val all = list().filter { it.id != p.id }.toMutableList()
        all.add(p)
        val arr = JSONArray()
        all.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("path", it.path).put("lastOpened", it.lastOpened)) }
        File(ctx.filesDir, "projects.json").writeText(arr.toString())
    }

    /** Historique d'une session (JSONL), utilisé pour la reprise et la recherche. */
    fun appendHistory(projectId: String, entry: JSONObject) {
        val f = File(dir, "history_$projectId.jsonl")
        entry.put("ts", System.currentTimeMillis())
        f.appendText(entry.toString() + "\n")
    }

    fun loadHistory(projectId: String): List<JSONObject> {
        val f = File(dir, "history_$projectId.jsonl")
        if (!f.exists()) return emptyList()
        return f.readLines().filter { it.isNotBlank() }.map { JSONObject(it) }
    }
}
