package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val folder: String = "All",
    val tags: String = "", // Comma separated, e.g., "Design,Work"
    val isPinned: Boolean = false,
    val isCompleted: Boolean = false,
    val imageUrl: String? = null,
    val checklistJson: String = "" // JSON: [{"text":"Action item","checked":false}]
) {
    fun getImageUrlList(): List<String> {
        if (imageUrl.isNullOrEmpty()) return emptyList()
        return imageUrl.split("|||").filter { it.isNotBlank() }
    }
}

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey val name: String,
    val colorHex: String = "#6750A4" // Accent color
)

data class ChecklistItem(
    val text: String,
    var checked: Boolean
)
