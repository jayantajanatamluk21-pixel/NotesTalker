package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("text", text)
        obj.put("isChecked", isChecked)
        return obj
    }

    companion object {
        fun fromJson(json: JSONObject): ChecklistItem {
            return ChecklistItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                text = json.optString("text", ""),
                isChecked = json.optBoolean("isChecked", false)
            )
        }

        fun listToJsonString(items: List<ChecklistItem>): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJsonString(jsonStr: String): List<ChecklistItem> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<ChecklistItem>()
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
