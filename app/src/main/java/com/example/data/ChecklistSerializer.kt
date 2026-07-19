package com.example.data

import org.json.JSONArray
import org.json.JSONObject

object ChecklistSerializer {
    fun fromJson(json: String?): List<ChecklistItem> {
        if (json.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<ChecklistItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChecklistItem(
                        text = obj.optString("text", ""),
                        checked = obj.optBoolean("checked", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun toJson(items: List<ChecklistItem>): String {
        val array = JSONArray()
        try {
            for (item in items) {
                val obj = JSONObject()
                obj.put("text", item.text)
                obj.put("checked", item.checked)
                array.put(obj)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return array.toString()
    }
}
