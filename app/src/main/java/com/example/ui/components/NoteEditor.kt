package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChecklistItem
import com.example.data.NoteEntity
import com.example.ui.theme.NoteColorOptions
import java.util.UUID

val Categories = listOf("General", "Personal", "Work", "Ideas", "Finance", "Urgent")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditor(
    note: NoteEntity,
    isVaultUnlocked: Boolean,
    onSave: (NoteEntity) -> Unit,
    onClose: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    var category by remember(note.id) { mutableStateOf(note.category) }
    var tagsInput by remember(note.id) { mutableStateOf("") }
    var tagList by remember(note.id) { mutableStateOf(note.getTagList()) }
    var colorHex by remember(note.id) { mutableStateOf(note.colorHex) }
    var isPinned by remember(note.id) { mutableStateOf(note.isPinned) }
    var isLocked by remember(note.id) { mutableStateOf(note.isLocked) }
    var checklistItems by remember(note.id) { mutableStateOf(note.getChecklistItems()) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showChecklistSection by remember(note.id) { mutableStateOf(checklistItems.isNotEmpty()) }
    var newChecklistText by remember { mutableStateOf("") }

    // Auto-save changes helper
    fun persistCurrentChanges(
        newTitle: String = title,
        newContent: String = content,
        newCategory: String = category,
        newTagList: List<String> = tagList,
        newColorHex: String = colorHex,
        newPinned: Boolean = isPinned,
        newLocked: Boolean = isLocked,
        newChecklist: List<ChecklistItem> = checklistItems
    ) {
        val updatedNote = note.copy(
            title = newTitle,
            content = newContent,
            category = newCategory,
            tags = newTagList.joinToString(","),
            colorHex = newColorHex,
            isPinned = newPinned,
            isLocked = newLocked,
            checklistJson = ChecklistItem.listToJsonString(newChecklist),
            updatedAt = System.currentTimeMillis()
        )
        onSave(updatedNote)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top action bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted Note",
                            tint = if (isVaultUnlocked) Color(0xFF10B981) else Color(0xFFF43F5E),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isVaultUnlocked) "E2EE Encrypted" else "Locked",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isVaultUnlocked) Color(0xFF10B981) else Color(0xFFF43F5E)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        persistCurrentChanges()
                        onClose()
                    },
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Notes"
                    )
                }
            },
            actions = {
                // Pin button
                IconButton(
                    onClick = {
                        isPinned = !isPinned
                        persistCurrentChanges(newPinned = isPinned)
                    },
                    modifier = Modifier.testTag("editor_pin_button")
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (isPinned) "Unpin Note" else "Pin Note",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Lock toggle button
                IconButton(
                    onClick = {
                        isLocked = !isLocked
                        persistCurrentChanges(newLocked = isLocked)
                    },
                    modifier = Modifier.testTag("editor_lock_button")
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isLocked) "Encrypted Note" else "Lock with E2EE",
                        tint = if (isLocked) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Palette button
                IconButton(
                    onClick = { showColorPicker = !showColorPicker },
                    modifier = Modifier.testTag("editor_color_palette_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Change Color Theme",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Archive button
                IconButton(
                    onClick = onArchive,
                    modifier = Modifier.testTag("editor_archive_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Archive Note"
                    )
                }

                // Delete button
                IconButton(
                    onClick = onTrash,
                    modifier = Modifier.testTag("editor_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Move to Trash",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Color Picker bar (collapsible)
        AnimatedVisibility(visible = showColorPicker) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NoteColorOptions.forEach { (hex, name) ->
                        val parsedColor = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    colorHex = hex
                                    persistCurrentChanges(newColorHex = hex)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = name,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Editor Content (Scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Categories.forEach { cat ->
                    val isSelected = category.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            category = cat
                            persistCurrentChanges(newCategory = cat)
                        },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title Input
            TextField(
                value = title,
                onValueChange = {
                    title = it
                    persistCurrentChanges(newTitle = it)
                },
                placeholder = {
                    Text(
                        "Title",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input"),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = false
            )

            // Tags display and input
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tagList.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove tag $tag",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable {
                                        val updated = tagList.filter { it != tag }
                                        tagList = updated
                                        persistCurrentChanges(newTagList = updated)
                                    },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Add Tag row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    placeholder = { Text("Add tag (e.g. security, meeting)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_tag_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val cleanTag = tagsInput.trim().replace("#", "")
                            if (cleanTag.isNotBlank() && cleanTag !in tagList) {
                                val updated = tagList + cleanTag
                                tagList = updated
                                tagsInput = ""
                                persistCurrentChanges(newTagList = updated)
                            }
                        }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val cleanTag = tagsInput.trim().replace("#", "")
                        if (cleanTag.isNotBlank() && cleanTag !in tagList) {
                            val updated = tagList + cleanTag
                            tagList = updated
                            tagsInput = ""
                            persistCurrentChanges(newTagList = updated)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add tag",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Checklist Section
            if (showChecklistSection || checklistItems.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tasks Checklist (${checklistItems.count { it.isChecked }}/${checklistItems.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { showChecklistSection = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hide Checklist",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        checklistItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { checked ->
                                        val updated = checklistItems.toMutableList()
                                        updated[index] = item.copy(isChecked = checked)
                                        checklistItems = updated
                                        persistCurrentChanges(newChecklist = updated)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val updated = checklistItems.toMutableList()
                                            updated[index] = item.copy(isChecked = !item.isChecked)
                                            checklistItems = updated
                                            persistCurrentChanges(newChecklist = updated)
                                        }
                                )
                                IconButton(
                                    onClick = {
                                        val updated = checklistItems.toMutableList()
                                        updated.removeAt(index)
                                        checklistItems = updated
                                        persistCurrentChanges(newChecklist = updated)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete item",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Add new checklist item input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newChecklistText,
                                onValueChange = { newChecklistText = it },
                                placeholder = { Text("New task...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_checklist_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newChecklistText.isNotBlank()) {
                                            val updated = checklistItems + ChecklistItem(
                                                id = UUID.randomUUID().toString(),
                                                text = newChecklistText.trim(),
                                                isChecked = false
                                            )
                                            checklistItems = updated
                                            newChecklistText = ""
                                            persistCurrentChanges(newChecklist = updated)
                                        }
                                    }
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newChecklistText.isNotBlank()) {
                                        val updated = checklistItems + ChecklistItem(
                                            id = UUID.randomUUID().toString(),
                                            text = newChecklistText.trim(),
                                            isChecked = false
                                        )
                                        checklistItems = updated
                                        newChecklistText = ""
                                        persistCurrentChanges(newChecklist = updated)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add task",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Note Body Content
            TextField(
                value = content,
                onValueChange = {
                    content = it
                    persistCurrentChanges(newContent = it)
                },
                placeholder = {
                    Text(
                        "Start typing your encrypted note...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_content_input"),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom Formatting & Utility Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Checklist Toggle
                IconButton(
                    onClick = { showChecklistSection = !showChecklistSection },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = "Toggle Checklist",
                        tint = if (showChecklistSection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bold
                IconButton(
                    onClick = {
                        content = "$content **bold text** "
                        persistCurrentChanges(newContent = content)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatBold,
                        contentDescription = "Insert Bold Markdown",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Italic
                IconButton(
                    onClick = {
                        content = "$content *italic text* "
                        persistCurrentChanges(newContent = content)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatItalic,
                        contentDescription = "Insert Italic Markdown",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bullet list
                IconButton(
                    onClick = {
                        content = if (content.endsWith("\n") || content.isEmpty()) "$content• " else "$content\n• "
                        persistCurrentChanges(newContent = content)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = "Insert Bullet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quote
                IconButton(
                    onClick = {
                        content = if (content.endsWith("\n") || content.isEmpty()) "$content> " else "$content\n> "
                        persistCurrentChanges(newContent = content)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = "Insert Quote",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Word count badge
                val totalWords = (title + " " + content).trim().let { if (it.isEmpty()) 0 else it.split("\\s+".toRegex()).size }
                Text(
                    text = "$totalWords words • ${title.length + content.length} chars",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
