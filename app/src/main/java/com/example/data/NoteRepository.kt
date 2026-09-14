package com.example.data

import com.example.sync.CloudSyncManager
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val cloudSyncManager: CloudSyncManager
) {
    val activeNotes: Flow<List<NoteEntity>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val trashedNotes: Flow<List<NoteEntity>> = noteDao.getTrashedNotes()

    fun getNoteById(id: String): Flow<NoteEntity?> = noteDao.getNoteById(id)

    suspend fun getNoteByIdDirect(id: String): NoteEntity? = noteDao.getNoteByIdDirect(id)

    suspend fun saveNote(note: NoteEntity) {
        val updated = note.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = "PENDING_SYNC",
            version = note.version + 1
        )
        noteDao.insertOrUpdate(updated)
        cloudSyncManager.triggerSync()
    }

    suspend fun togglePin(note: NoteEntity) {
        saveNote(note.copy(isPinned = !note.isPinned))
    }

    suspend fun toggleLock(note: NoteEntity) {
        saveNote(note.copy(isLocked = !note.isLocked))
    }

    suspend fun archiveNote(note: NoteEntity) {
        saveNote(note.copy(isArchived = true, isPinned = false))
    }

    suspend fun unarchiveNote(note: NoteEntity) {
        saveNote(note.copy(isArchived = false))
    }

    suspend fun trashNote(note: NoteEntity) {
        saveNote(note.copy(isTrashed = true, isPinned = false))
    }

    suspend fun restoreNote(note: NoteEntity) {
        saveNote(note.copy(isTrashed = false))
    }

    suspend fun deletePermanently(id: String) {
        noteDao.deleteById(id)
        cloudSyncManager.triggerSync()
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
        cloudSyncManager.triggerSync()
    }

    suspend fun seedInitialNotesIfEmpty() {
        val all = noteDao.getAllNotesDirect()
        if (all.isEmpty()) {
            val checklistExample = listOf(
                ChecklistItem(text = "End-to-End Encryption (AES-256-GCM)", isChecked = true),
                ChecklistItem(text = "PBKDF2 Key Derivation with 64k rounds", isChecked = true),
                ChecklistItem(text = "Offline-first local Room storage", isChecked = true),
                ChecklistItem(text = "Automatic cloud backup sync", isChecked = true),
                ChecklistItem(text = "Responsive adaptive UI for tablets & phones", isChecked = true)
            )

            val initialNotes = listOf(
                NoteEntity(
                    title = "Welcome to Secure Notes 🔐",
                    content = "Your privacy is paramount. Everything you write here is end-to-end encrypted (E2EE) using military-grade AES-256-GCM authenticated encryption.\n\nKey features:\n• Zero-knowledge cloud synchronization\n• 100% offline access\n• Automatic background backups\n• Individual note locks & Vault security\n• Rich checklists & color tags",
                    category = "General",
                    tags = "welcome,security,guide",
                    colorHex = "#1E293B",
                    isPinned = true,
                    isLocked = false,
                    checklistJson = ChecklistItem.listToJsonString(checklistExample)
                ),
                NoteEntity(
                    title = "Private Financial Credentials 💳",
                    content = "Vault recovery token: 4892-3841-9920-ABCD\nPrimary bank routing: 021000021\nCrypto wallet cold backup mnemonic: [Protected in memory]\n\nThis note is locked behind your master encryption key. When the vault is locked, the content cannot be read without your passphrase.",
                    category = "Finance",
                    tags = "finance,confidential",
                    colorHex = "#064E3B", // Emerald
                    isPinned = true,
                    isLocked = true
                ),
                NoteEntity(
                    title = "Project Phoenix Architecture Plan 🚀",
                    content = "Objectives for Q4:\n1. Zero-knowledge cryptographic protocol review\n2. Real-time conflict resolution using vector timestamps\n3. Adaptive split-pane layout testing on foldable devices\n4. Cloud snapshot verification tests",
                    category = "Work",
                    tags = "work,architecture,phoenix",
                    colorHex = "#1E3A8A", // Indigo
                    isPinned = false,
                    isLocked = false
                ),
                NoteEntity(
                    title = "Weekend Creative Sparks 💡",
                    content = "Ideas to explore:\n• Interactive markdown live rendering in Jetpack Compose\n• Canvas-based audio waveform visualizer\n• Biometric hardware-backed keystore integration\n• Self-hosted WebDAV sync protocol adapter",
                    category = "Ideas",
                    tags = "ideas,creative,compose",
                    colorHex = "#581C87", // Purple
                    isPinned = false,
                    isLocked = false
                )
            )

            noteDao.insertOrUpdateAll(initialNotes)
        }
    }
}
