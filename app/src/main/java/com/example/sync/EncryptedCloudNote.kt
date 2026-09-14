package com.example.sync

import com.example.crypto.CryptoEngine
import com.example.crypto.EncryptedPayload
import com.example.data.NoteEntity
import org.json.JSONObject
import javax.crypto.SecretKey

data class EncryptedCloudNote(
    val id: String,
    val ciphertext: String,
    val iv: String,
    val salt: String,
    val checksum: String,
    val updatedAt: Long,
    val version: Int,
    val isDeleted: Boolean = false
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("ciphertext", ciphertext)
        obj.put("iv", iv)
        obj.put("salt", salt)
        obj.put("checksum", checksum)
        obj.put("updatedAt", updatedAt)
        obj.put("version", version)
        obj.put("isDeleted", isDeleted)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): EncryptedCloudNote {
            return EncryptedCloudNote(
                id = obj.getString("id"),
                ciphertext = obj.getString("ciphertext"),
                iv = obj.getString("iv"),
                salt = obj.getString("salt"),
                checksum = obj.optString("checksum", ""),
                updatedAt = obj.getLong("updatedAt"),
                version = obj.optInt("version", 1),
                isDeleted = obj.optBoolean("isDeleted", false)
            )
        }

        fun encryptNote(note: NoteEntity, key: SecretKey, salt: ByteArray): EncryptedCloudNote {
            val innerJson = JSONObject().apply {
                put("title", note.title)
                put("content", note.content)
                put("category", note.category)
                put("tags", note.tags)
                put("colorHex", note.colorHex)
                put("isPinned", note.isPinned)
                put("isLocked", note.isLocked)
                put("isArchived", note.isArchived)
                put("checklistJson", note.checklistJson)
                put("createdAt", note.createdAt)
            }

            val payload = CryptoEngine.encrypt(innerJson.toString(), key, salt)
            return EncryptedCloudNote(
                id = note.id,
                ciphertext = payload.ciphertext,
                iv = payload.iv,
                salt = payload.salt,
                checksum = payload.checksum,
                updatedAt = note.updatedAt,
                version = note.version,
                isDeleted = note.isTrashed
            )
        }

        fun decryptNote(encrypted: EncryptedCloudNote, key: SecretKey): NoteEntity {
            val payload = EncryptedPayload(
                ciphertext = encrypted.ciphertext,
                iv = encrypted.iv,
                salt = encrypted.salt,
                checksum = encrypted.checksum
            )
            val decryptedPlaintext = CryptoEngine.decrypt(payload, key)
            val json = JSONObject(decryptedPlaintext)

            return NoteEntity(
                id = encrypted.id,
                title = json.optString("title", ""),
                content = json.optString("content", ""),
                category = json.optString("category", "General"),
                tags = json.optString("tags", ""),
                colorHex = json.optString("colorHex", "#1E293B"),
                isPinned = json.optBoolean("isPinned", false),
                isLocked = json.optBoolean("isLocked", false),
                isArchived = json.optBoolean("isArchived", false),
                isTrashed = encrypted.isDeleted,
                checklistJson = json.optString("checklistJson", ""),
                createdAt = json.optLong("createdAt", encrypted.updatedAt),
                updatedAt = encrypted.updatedAt,
                syncStatus = "SYNCED",
                lastSyncedAt = System.currentTimeMillis(),
                version = encrypted.version
            )
        }
    }
}
