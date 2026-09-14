package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import com.example.sync.SyncState
import com.example.ui.components.Categories
import com.example.ui.components.CloudSyncDialog
import com.example.ui.components.NoteCard
import com.example.ui.components.NoteEditor
import com.example.ui.components.VaultDialog

@Composable
fun MainNotesScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val activeNotes by viewModel.activeNotes.collectAsState()
    val archivedNotes by viewModel.archivedNotes.collectAsState()
    val trashedNotes by viewModel.trashedNotes.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val showVaultDialog by viewModel.showVaultDialog.collectAsState()
    val showCloudSyncDialog by viewModel.showCloudSyncDialog.collectAsState()
    val notification by viewModel.userNotification.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearNotification()
        }
    }

    // Dialogs
    if (showVaultDialog) {
        VaultDialog(
            viewModel = viewModel,
            isUnlocked = isVaultUnlocked,
            onDismiss = { viewModel.showVaultDialog.value = false }
        )
    }

    if (showCloudSyncDialog) {
        CloudSyncDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showCloudSyncDialog.value = false }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            // Tablet / Foldable Expanded: Dual-Pane Canonical Layout
            Row(modifier = Modifier.fillMaxSize()) {
                // Navigation Rail
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Secure Notes",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    NavigationRailItem(
                        selected = currentTab == NotesTab.NOTES,
                        onClick = { viewModel.currentTab.value = NotesTab.NOTES },
                        icon = { Icon(Icons.Default.Notes, contentDescription = "Notes") },
                        label = { Text("Notes") }
                    )
                    NavigationRailItem(
                        selected = currentTab == NotesTab.ARCHIVE,
                        onClick = { viewModel.currentTab.value = NotesTab.ARCHIVE },
                        icon = { Icon(Icons.Default.Archive, contentDescription = "Archive") },
                        label = { Text("Archive") }
                    )
                    NavigationRailItem(
                        selected = currentTab == NotesTab.TRASH,
                        onClick = { viewModel.currentTab.value = NotesTab.TRASH },
                        icon = { Icon(Icons.Default.Delete, contentDescription = "Trash") },
                        label = { Text("Trash") }
                    )
                }

                // Left Pane: Notes List
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                ) {
                    NotesListPane(
                        viewModel = viewModel,
                        currentTab = currentTab,
                        notes = when (currentTab) {
                            NotesTab.NOTES -> activeNotes
                            NotesTab.ARCHIVE -> archivedNotes
                            NotesTab.TRASH -> trashedNotes
                        },
                        isVaultUnlocked = isVaultUnlocked,
                        syncState = syncState,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        onNoteClick = { viewModel.openNoteForEditing(it) },
                        onCreateNote = { viewModel.createNewNote() }
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )

                // Right Pane: Editor or Empty Selection
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (editingNote != null) {
                        NoteEditor(
                            note = editingNote!!,
                            isVaultUnlocked = isVaultUnlocked,
                            onSave = { viewModel.saveEditingNote(it) },
                            onClose = { viewModel.closeEditor() },
                            onArchive = { viewModel.archiveNote(editingNote!!) },
                            onTrash = { viewModel.trashNote(editingNote!!) }
                        )
                    } else {
                        // Empty Selection placeholder
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Select a note to view or edit",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.createNewNote() },
                                    modifier = Modifier.testTag("create_note_detail_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("New Note")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Handheld Compact: Single-Pane Navigation
            if (editingNote != null) {
                NoteEditor(
                    note = editingNote!!,
                    isVaultUnlocked = isVaultUnlocked,
                    onSave = { viewModel.saveEditingNote(it) },
                    onClose = { viewModel.closeEditor() },
                    onArchive = { viewModel.archiveNote(editingNote!!) },
                    onTrash = { viewModel.trashNote(editingNote!!) }
                )
            } else {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.navigationBarsPadding(),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBarItem(
                                selected = currentTab == NotesTab.NOTES,
                                onClick = { viewModel.currentTab.value = NotesTab.NOTES },
                                icon = { Icon(Icons.Default.Notes, contentDescription = "Notes") },
                                label = { Text("Notes") }
                            )
                            NavigationBarItem(
                                selected = currentTab == NotesTab.ARCHIVE,
                                onClick = { viewModel.currentTab.value = NotesTab.ARCHIVE },
                                icon = { Icon(Icons.Default.Archive, contentDescription = "Archive") },
                                label = { Text("Archive") }
                            )
                            NavigationBarItem(
                                selected = currentTab == NotesTab.TRASH,
                                onClick = { viewModel.currentTab.value = NotesTab.TRASH },
                                icon = { Icon(Icons.Default.Delete, contentDescription = "Trash") },
                                label = { Text("Trash") }
                            )
                        }
                    },
                    floatingActionButton = {
                        if (currentTab == NotesTab.NOTES) {
                            FloatingActionButton(
                                onClick = { viewModel.createNewNote() },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                modifier = Modifier.testTag("new_note_fab")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Create Note")
                            }
                        }
                    }
                ) { innerPadding ->
                    NotesListPane(
                        viewModel = viewModel,
                        currentTab = currentTab,
                        notes = when (currentTab) {
                            NotesTab.NOTES -> activeNotes
                            NotesTab.ARCHIVE -> archivedNotes
                            NotesTab.TRASH -> trashedNotes
                        },
                        isVaultUnlocked = isVaultUnlocked,
                        syncState = syncState,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        onNoteClick = { viewModel.openNoteForEditing(it) },
                        onCreateNote = { viewModel.createNewNote() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListPane(
    viewModel: NotesViewModel,
    currentTab: NotesTab,
    notes: List<NoteEntity>,
    isVaultUnlocked: Boolean,
    syncState: SyncState,
    searchQuery: String,
    selectedCategory: String,
    onNoteClick: (NoteEntity) -> Unit,
    onCreateNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App TopBar with Security & Sync Affordances
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (currentTab) {
                            NotesTab.NOTES -> "Secure Notes"
                            NotesTab.ARCHIVE -> "Archived"
                            NotesTab.TRASH -> "Trash"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            actions = {
                // Cloud Sync Status Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clickable { viewModel.showCloudSyncDialog.value = true }
                        .testTag("cloud_sync_status_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (syncState) {
                            is SyncState.Syncing -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Syncing", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            is SyncState.Offline -> {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Offline", fontSize = 11.sp, color = Color(0xFFF59E0B))
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Synced", fontSize = 11.sp, color = Color(0xFF10B981))
                            }
                        }
                    }
                }

                // Vault Lock/Unlock Button
                IconButton(
                    onClick = { viewModel.showVaultDialog.value = true },
                    modifier = Modifier.testTag("vault_shield_button")
                ) {
                    Icon(
                        imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = if (isVaultUnlocked) "Vault Unlocked" else "Vault Locked",
                        tint = if (isVaultUnlocked) Color(0xFF10B981) else Color(0xFFF43F5E)
                    )
                }

                // Search toggle
                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier.testTag("search_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Notes"
                    )
                }

                // Trash Action: Empty Trash
                if (currentTab == NotesTab.TRASH && notes.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.emptyTrash() },
                        modifier = Modifier.testTag("empty_trash_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Empty Trash",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Expandable Search Bar
        AnimatedVisibility(visible = isSearchExpanded) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search title, content, or tags...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        // Category Filter Chips Row (only on active notes)
        if (currentTab == NotesTab.NOTES) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterCategories = listOf("All") + Categories
                filterCategories.forEach { cat ->
                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedCategory.value = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Notes List / Empty State
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when (currentTab) {
                            NotesTab.NOTES -> Icons.Default.NoteAdd
                            NotesTab.ARCHIVE -> Icons.Default.Archive
                            NotesTab.TRASH -> Icons.Default.Delete
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when (currentTab) {
                            NotesTab.NOTES -> if (searchQuery.isNotBlank()) "No notes match \"$searchQuery\"" else "No notes in this category yet"
                            NotesTab.ARCHIVE -> "No archived notes"
                            NotesTab.TRASH -> "Trash is empty"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (currentTab == NotesTab.NOTES && searchQuery.isBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onCreateNote,
                            modifier = Modifier.testTag("empty_create_note_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Note")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                items(notes, key = { it.id }) { note ->
                    if (currentTab == NotesTab.TRASH) {
                        // Trashed Note Card with Restore & Delete Permanently actions
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (note.title.isNotBlank()) note.title else "Untitled Note",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = note.content.take(80),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    TextButton(onClick = { viewModel.restoreNote(note) }) {
                                        Text("Restore")
                                    }
                                    IconButton(onClick = { viewModel.deletePermanently(note) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    } else if (currentTab == NotesTab.ARCHIVE) {
                        // Archived Note Card with Unarchive action
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onNoteClick(note) }
                                ) {
                                    Text(
                                        text = if (note.title.isNotBlank()) note.title else "Untitled Note",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = note.content.take(80),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { viewModel.unarchiveNote(note) }) {
                                    Text("Unarchive")
                                }
                            }
                        }
                    } else {
                        // Active Note Card
                        NoteCard(
                            note = note,
                            isVaultUnlocked = isVaultUnlocked,
                            onClick = { onNoteClick(note) },
                            onTogglePin = { viewModel.togglePin(note) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
