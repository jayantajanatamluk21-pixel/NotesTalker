package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.VaultManager
import com.example.data.AppDatabase
import com.example.data.ChecklistItem
import com.example.data.NoteEntity
import com.example.data.NoteRepository
import com.example.sync.CloudSyncManager
import com.example.sync.EncryptedCloudNote
import com.example.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class NotesTab {
    NOTES, ARCHIVE, TRASH
}

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val vaultManager = VaultManager(application)
    val cloudSyncManager = CloudSyncManager(
        context = application,
        noteDao = database.noteDao(),
        vaultManager = vaultManager,
        scope = viewModelScope
    )
    val repository = NoteRepository(database.noteDao(), cloudSyncManager)

    val isVaultUnlocked: StateFlow<Boolean> = vaultManager.isUnlocked
    val isPassphraseConfigured = MutableStateFlow(vaultManager.isPassphraseConfigured())

    val syncState: StateFlow<SyncState> = cloudSyncManager.syncState
    val isAutoSyncEnabled: StateFlow<Boolean> = cloudSyncManager.isAutoSyncEnabled
    val cloudStorageInfo: StateFlow<String> = cloudSyncManager.cloudStorageInfo

    val currentTab = MutableStateFlow(NotesTab.NOTES)
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val selectedTag = MutableStateFlow<String?>(null)

    // Currently selected note for editor pane
    val editingNote = MutableStateFlow<NoteEntity?>(null)

    // UI Dialogs
    val showVaultDialog = MutableStateFlow(false)
    val showCloudSyncDialog = MutableStateFlow(false)
    val showSecurityInfoDialog = MutableStateFlow(false)
    val userNotification = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.seedInitialNotesIfEmpty()
            // Set up a default passphrase if none exists so encryption works immediately
            if (!vaultManager.isPassphraseConfigured()) {
                vaultManager.setupPassphrase("MasterPass2026!")
                isPassphraseConfigured.value = true
            }
        }
    }

    val activeNotes: StateFlow<List<NoteEntity>> = combine(
        repository.activeNotes,
        searchQuery,
        selectedCategory,
        selectedTag
    ) { notes, query, category, tag ->
        notes.filter { note ->
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true) ||
                    note.tags.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" || note.category.equals(category, ignoreCase = true)
            val matchesTag = tag == null || note.getTagList().any { it.equals(tag, ignoreCase = true) }

            matchesQuery && matchesCategory && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteEntity>> = repository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<NoteEntity>> = repository.trashedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createNewNote() {
        val newNote = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = "",
            content = "",
            category = if (selectedCategory.value != "All") selectedCategory.value else "General",
            colorHex = "#1E293B",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        editingNote.value = newNote
    }

    fun openNoteForEditing(note: NoteEntity) {
        editingNote.value = note
    }

    fun closeEditor() {
        editingNote.value = null
    }

    fun saveEditingNote(note: NoteEntity) {
        editingNote.value = note
        viewModelScope.launch {
            repository.saveNote(note)
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.togglePin(note)
            if (editingNote.value?.id == note.id) {
                editingNote.value = note.copy(isPinned = !note.isPinned)
            }
        }
    }

    fun toggleLock(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleLock(note)
            if (editingNote.value?.id == note.id) {
                editingNote.value = note.copy(isLocked = !note.isLocked)
            }
        }
    }

    fun archiveNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.archiveNote(note)
            if (editingNote.value?.id == note.id) {
                editingNote.value = null
            }
            userNotification.value = "Note moved to archive"
        }
    }

    fun unarchiveNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.unarchiveNote(note)
            userNotification.value = "Note restored to active notes"
        }
    }

    fun trashNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.trashNote(note)
            if (editingNote.value?.id == note.id) {
                editingNote.value = null
            }
            userNotification.value = "Note moved to trash"
        }
    }

    fun restoreNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.restoreNote(note)
            userNotification.value = "Note restored from trash"
        }
    }

    fun deletePermanently(note: NoteEntity) {
        viewModelScope.launch {
            repository.deletePermanently(note.id)
            if (editingNote.value?.id == note.id) {
                editingNote.value = null
            }
            userNotification.value = "Note deleted permanently"
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            userNotification.value = "Trash emptied"
        }
    }

    fun unlockVault(passphrase: String): Boolean {
        val success = vaultManager.unlock(passphrase)
        if (success) {
            userNotification.value = "Vault unlocked successfully"
            cloudSyncManager.triggerSync()
        }
        return success
    }

    fun setupNewVaultPassphrase(passphrase: String): Boolean {
        val success = vaultManager.setupPassphrase(passphrase)
        if (success) {
            isPassphraseConfigured.value = true
            userNotification.value = "Encryption passphrase saved"
            cloudSyncManager.triggerSync()
        }
        return success
    }

    fun lockVault() {
        vaultManager.lock()
        userNotification.value = "Vault locked. Sensitive notes encrypted."
    }

    fun triggerCloudSync() {
        cloudSyncManager.triggerSync()
    }

    fun toggleAutoSync(enabled: Boolean) {
        cloudSyncManager.setAutoSync(enabled)
    }

    fun getEncryptedCloudPayloads(): List<EncryptedCloudNote> {
        return cloudSyncManager.getEncryptedCloudNotes()
    }

    fun clearNotification() {
        userNotification.value = null
    }
}
