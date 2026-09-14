package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val category: String = "General",
    val tags: String = "", // Comma-separated tags
    val colorHex: String = "#1E293B", // Modern slate default
    val isPinned: Boolean = false,
    val isLocked: Boolean = false, // E2EE locked note
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val checklistJson: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING_SYNC", // PENDING_SYNC, SYNCED, SYNCING
    val lastSyncedAt: Long = 0L,
    val version: Int = 1
) {
    fun getTagList(): List<String> {
        if (tags.isBlank()) return emptyList()
        return tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getChecklistItems(): List<ChecklistItem> {
        return ChecklistItem.listFromJsonString(checklistJson)
    }

    val wordCount: Int
        get() {
            val text = (title + " " + content).trim()
            if (text.isEmpty()) return 0
            return text.split("\\s+".toRegex()).size
        }

    val characterCount: Int
        get() = title.length + content.length
}
