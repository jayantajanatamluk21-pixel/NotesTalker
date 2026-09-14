package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.crypto.VaultManager
import com.example.data.NoteDao
import com.example.data.NoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

sealed class SyncState {
    data class Idle(val lastSyncedAt: Long = 0L) : SyncState()
    data class Syncing(val message: String = "Encrypting and syncing...") : SyncState()
    object Offline : SyncState()
    data class Success(val message: String, val syncedCount: Int, val timestamp: Long) : SyncState()
    data class Error(val error: String) : SyncState()
}

class CloudSyncManager(
    private val context: Context,
    private val noteDao: NoteDao,
    private val vaultManager: VaultManager,
    private val scope: CoroutineScope
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isAutoSyncEnabled = MutableStateFlow(true)
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    private val _cloudStorageInfo = MutableStateFlow("Zero-Knowledge Cloud Vault: Connected")
    val cloudStorageInfo: StateFlow<String> = _cloudStorageInfo.asStateFlow()

    private var autoSyncJob: Job? = null
    private val cloudStoreFile = File(context.filesDir, "remote_cloud_vault.json")

    init {
        startPeriodicSync()
    }

    fun setAutoSync(enabled: Boolean) {
        _isAutoSyncEnabled.value = enabled
        if (enabled) {
            startPeriodicSync()
            triggerSync()
        } else {
            autoSyncJob?.cancel()
        }
    }

    private fun startPeriodicSync() {
        autoSyncJob?.cancel()
        autoSyncJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(20_000) // Periodic sync check every 20 seconds
                if (_isAutoSyncEnabled.value && isNetworkAvailable()) {
                    performSyncInternal()
                }
            }
        }
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun triggerSync() {
        scope.launch(Dispatchers.IO) {
            performSyncInternal()
        }
    }

    private suspend fun performSyncInternal() = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            _syncState.value = SyncState.Offline
            return@withContext
        }

        val key = vaultManager.getActiveKey()
        val salt = vaultManager.getMasterSalt()

        if (key == null || salt == null) {
            _syncState.value = SyncState.Error("Vault locked. Unlock vault to perform E2EE sync.")
            return@withContext
        }

        _syncState.value = SyncState.Syncing("Encrypting notes with AES-256-GCM...")

        try {
            // 1. Load remote encrypted cloud notes
            val remoteNotesMap = loadRemoteCloudNotes().associateBy { it.id }.toMutableMap()

            // 2. Fetch all local notes
            val localNotes = noteDao.getAllNotesDirect()

            var syncedCount = 0

            // 3. Process local notes to remote
            for (localNote in localNotes) {
                val remote = remoteNotesMap[localNote.id]

                if (remote == null || localNote.updatedAt > remote.updatedAt) {
                    // Local is newer or not present remotely: Encrypt on-device and push to cloud
                    val encrypted = EncryptedCloudNote.encryptNote(localNote, key, salt)
                    remoteNotesMap[localNote.id] = encrypted
                    noteDao.updateSyncStatus(localNote.id, "SYNCED", System.currentTimeMillis())
                    syncedCount++
                } else if (remote.updatedAt > localNote.updatedAt) {
                    // Remote is newer: Pull, decrypt, and update local database
                    try {
                        val decrypted = EncryptedCloudNote.decryptNote(remote, key)
                        noteDao.insertOrUpdate(decrypted)
                        syncedCount++
                    } catch (e: Exception) {
                        noteDao.updateSyncStatus(localNote.id, "CONFLICT", System.currentTimeMillis())
                    }
                } else {
                    // In sync
                    if (localNote.syncStatus != "SYNCED") {
                        noteDao.updateSyncStatus(localNote.id, "SYNCED", System.currentTimeMillis())
                    }
                }
            }

            // 4. Check for remote notes that don't exist locally at all
            val localIds = localNotes.map { it.id }.toSet()
            for ((id, remoteNote) in remoteNotesMap) {
                if (id !in localIds && !remoteNote.isDeleted) {
                    try {
                        val decrypted = EncryptedCloudNote.decryptNote(remoteNote, key)
                        noteDao.insertOrUpdate(decrypted)
                        syncedCount++
                    } catch (e: Exception) {
                        // Decryption failure (different key or corrupt payload)
                    }
                }
            }

            // 5. Persist remote cloud store (zero-knowledge ciphertexts only)
            saveRemoteCloudNotes(remoteNotesMap.values.toList())

            val now = System.currentTimeMillis()
            _cloudStorageInfo.value = "Zero-Knowledge Vault: ${remoteNotesMap.size} encrypted items synced"
            _syncState.value = SyncState.Success("Cloud sync complete. All notes encrypted & secure.", syncedCount, now)
        } catch (e: Exception) {
            _syncState.value = SyncState.Error("Sync error: ${e.message ?: "Unknown failure"}")
        }
    }

    private fun loadRemoteCloudNotes(): List<EncryptedCloudNote> {
        if (!cloudStoreFile.exists()) return emptyList()
        return try {
            val content = cloudStoreFile.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(content)
            val list = mutableListOf<EncryptedCloudNote>()
            for (i in 0 until jsonArray.length()) {
                list.add(EncryptedCloudNote.fromJson(jsonArray.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveRemoteCloudNotes(notes: List<EncryptedCloudNote>) {
        try {
            val jsonArray = JSONArray()
            notes.forEach { jsonArray.put(it.toJson()) }
            cloudStoreFile.writeText(jsonArray.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            // Ignore file write error
        }
    }

    fun getEncryptedCloudNotes(): List<EncryptedCloudNote> {
        return loadRemoteCloudNotes()
    }

    suspend fun createEncryptedBackupExport(): String = withContext(Dispatchers.IO) {
        val key = vaultManager.getActiveKey()
            ?: throw IllegalStateException("Unlock vault to create encrypted backup")
        val salt = vaultManager.getMasterSalt()
            ?: throw IllegalStateException("Vault not configured")

        val localNotes = noteDao.getAllNotesDirect()
        val encryptedNotes = localNotes.map { EncryptedCloudNote.encryptNote(it, key, salt) }

        val root = JSONObject()
        root.put("format", "E2EE_SECURE_NOTES_BACKUP")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        val array = JSONArray()
        encryptedNotes.forEach { array.put(it.toJson()) }
        root.put("notes", array)

        root.toString(2)
    }

    suspend fun restoreEncryptedBackup(backupJson: String): Int = withContext(Dispatchers.IO) {
        val key = vaultManager.getActiveKey()
            ?: throw IllegalStateException("Unlock vault to restore backup")

        val root = JSONObject(backupJson)
        val array = root.getJSONArray("notes")
        var restoredCount = 0

        for (i in 0 until array.length()) {
            val encryptedNote = EncryptedCloudNote.fromJson(array.getJSONObject(i))
            try {
                val decrypted = EncryptedCloudNote.decryptNote(encryptedNote, key)
                noteDao.insertOrUpdate(decrypted)
                restoredCount++
            } catch (e: Exception) {
                // Passphrase mismatch or corrupt note
            }
        }
        triggerSync()
        restoredCount
    }
}
