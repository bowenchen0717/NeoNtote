package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import com.example.data.Note
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: NoteViewModel,
    onNavigateToView: (Int) -> Unit,
    onNavigateToEdit: (Int?) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val folders by viewModel.allFolders.collectAsStateWithLifecycle()
    val tags by viewModel.allTags.collectAsStateWithLifecycle()

    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    var localFolders by remember { mutableStateOf(folders) }
    var localTags by remember { mutableStateOf(tags) }

    var draggingFolderName by remember { mutableStateOf<String?>(null) }
    var folderAccumulatedDragY by remember { mutableStateOf(0f) }

    var draggingTagName by remember { mutableStateOf<String?>(null) }
    var tagAccumulatedDragY by remember { mutableStateOf(0f) }

    LaunchedEffect(folders) {
        if (draggingFolderName == null) {
            localFolders = folders
        }
    }

    LaunchedEffect(tags) {
        if (draggingTagName == null) {
            localTags = tags
        }
    }

    // Bottom Navigation Bar selection state
    var bottomTabSelection by remember { mutableStateOf("all") } // "all", "pinned", "checklists", "sync"

    // Custom filtering based on bottom tab selection
    val displayedNotes = remember(notes, bottomTabSelection) {
        when (bottomTabSelection) {
            "pinned" -> notes.filter { it.isPinned }
            "checklists" -> notes.filter { com.example.data.ChecklistSerializer.fromJson(it.checklistJson).isNotEmpty() }
            else -> notes
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(320.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFB19DFF), Color(0xFF4FC3F7))
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notes, contentDescription = "AppIcon", tint = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                Localization.get("drawer_title", language),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                Localization.get("drawer_sync_enabled", language),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Folders Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        Localization.get("drawer_folders", language),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    IconButton(
                        onClick = { showAddFolderDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Folder", modifier = Modifier.size(18.dp))
                    }
                }

                // Folder items
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    itemsIndexed(localFolders, key = { _, folder -> folder.name }) { index, folder ->
                        val isSelected = selectedFolder == folder.name
                        val isDragging = draggingFolderName == folder.name
                        NavigationDrawerItem(
                            icon = {
                                Icon(Icons.Default.Folder, contentDescription = "Folder")
                            },
                            label = { Text(Localization.getFolderName(folder.name, language)) },
                            selected = isSelected,
                            onClick = {
                                viewModel.selectFolder(folder.name)
                                scope.launch { drawerState.close() }
                            },
                            badge = {
                                if (folder.name != "All") {
                                    IconButton(onClick = { viewModel.deleteFolder(folder) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .pointerInput(folder.name) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingFolderName = folder.name
                                            folderAccumulatedDragY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            folderAccumulatedDragY += dragAmount.y
                                            val threshold = 90f // threshold in pixels for swapping
                                            val currentName = draggingFolderName
                                            if (currentName != null) {
                                                val currIdx = localFolders.indexOfFirst { it.name == currentName }
                                                if (currIdx != -1) {
                                                    if (folderAccumulatedDragY > threshold && currIdx < localFolders.size - 1) {
                                                        localFolders = localFolders.move(currIdx, currIdx + 1)
                                                        folderAccumulatedDragY -= threshold
                                                    } else if (folderAccumulatedDragY < -threshold && currIdx > 0) {
                                                        localFolders = localFolders.move(currIdx, currIdx - 1)
                                                        folderAccumulatedDragY += threshold
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggingFolderName = null
                                            folderAccumulatedDragY = 0f
                                            viewModel.updateFolderOrder(localFolders.map { it.name })
                                        },
                                        onDragCancel = {
                                            draggingFolderName = null
                                            folderAccumulatedDragY = 0f
                                            viewModel.updateFolderOrder(localFolders.map { it.name })
                                        }
                                    )
                                }
                                .graphicsLayer {
                                    scaleX = if (isDragging) 1.04f else 1.0f
                                    scaleY = if (isDragging) 1.04f else 1.0f
                                    shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                                }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Tags Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        Localization.get("drawer_tags", language),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    IconButton(
                        onClick = { showAddTagDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Tag", modifier = Modifier.size(18.dp))
                    }
                }

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    itemsIndexed(localTags, key = { _, tag -> tag.name }) { index, tag ->
                        val isSelected = selectedTag == tag.name
                        val isDragging = draggingTagName == tag.name
                        NavigationDrawerItem(
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            color = Color(android.graphics.Color.parseColor(tag.colorHex)),
                                            shape = CircleShape
                                        )
                                )
                            },
                            label = { Text(Localization.getTagName(tag.name, language)) },
                            selected = isSelected,
                            onClick = {
                                viewModel.selectTag(if (isSelected) null else tag.name)
                                scope.launch { drawerState.close() }
                            },
                            badge = {
                                IconButton(onClick = { viewModel.deleteTag(tag) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .pointerInput(tag.name) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingTagName = tag.name
                                            tagAccumulatedDragY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            tagAccumulatedDragY += dragAmount.y
                                            val threshold = 90f // threshold in pixels for swapping
                                            val currentName = draggingTagName
                                            if (currentName != null) {
                                                val currIdx = localTags.indexOfFirst { it.name == currentName }
                                                if (currIdx != -1) {
                                                    if (tagAccumulatedDragY > threshold && currIdx < localTags.size - 1) {
                                                        localTags = localTags.move(currIdx, currIdx + 1)
                                                        tagAccumulatedDragY -= threshold
                                                    } else if (tagAccumulatedDragY < -threshold && currIdx > 0) {
                                                        localTags = localTags.move(currIdx, currIdx - 1)
                                                        tagAccumulatedDragY += threshold
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggingTagName = null
                                            tagAccumulatedDragY = 0f
                                            viewModel.updateTagOrder(localTags.map { it.name })
                                        },
                                        onDragCancel = {
                                            draggingTagName = null
                                            tagAccumulatedDragY = 0f
                                            viewModel.updateTagOrder(localTags.map { it.name })
                                        }
                                    )
                                }
                                .graphicsLayer {
                                    scaleX = if (isDragging) 1.04f else 1.0f
                                    scaleY = if (isDragging) 1.04f else 1.0f
                                    shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Settings drawer item
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(Localization.get("drawer_settings", language)) },
                    selected = false,
                    onClick = {
                        showSettingsDialog = true
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    title = {
                        Column {
                            Text(
                                text = Localization.get("main_title", language),
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            if (selectedFolder != "All" || selectedTag != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val filterValue = if (selectedTag != null) {
                                        Localization.getTagName(selectedTag!!, language)
                                    } else {
                                        Localization.getFolderName(selectedFolder, language)
                                    }
                                    Text(
                                        text = Localization.get("main_filter", language) + filterValue,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = Localization.get("main_clear_filter", language),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable {
                                                viewModel.selectFolder("All")
                                                viewModel.selectTag(null)
                                            }
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Expandable elegant search field
                        if (isSearchExpanded) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.searchQuery.value = it },
                                placeholder = { Text(Localization.get("main_search_placeholder", language)) },
                                modifier = Modifier
                                    .width(180.dp)
                                    .padding(end = 4.dp),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        viewModel.searchQuery.value = ""
                                        isSearchExpanded = false
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Search")
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        } else {
                            IconButton(onClick = { isSearchExpanded = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.testTag("search_button"))
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        this@NavigationBar.NavigationBarItem(
                            icon = { Icon(Icons.Default.Folder, contentDescription = "Folders") },
                            label = {
                                val labelText = if (selectedFolder == "All") {
                                    Localization.get("main_tab_all", language)
                                } else {
                                    Localization.getFolderName(selectedFolder, language)
                                }
                                Text(
                                    text = labelText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = bottomTabSelection == "all",
                            onClick = {
                                bottomTabSelection = "all"
                                showFolderMenu = true
                            }
                        )

                        DropdownMenu(
                            expanded = showFolderMenu,
                            onDismissRequest = { showFolderMenu = false }
                        ) {
                            Text(
                                text = Localization.get("drawer_folders", language),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            HorizontalDivider()

                            folders.forEach { folder ->
                                val isSelected = selectedFolder == folder.name
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = Localization.getFolderName(folder.name, language),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (folder.name == "All") Icons.Default.AllInclusive else Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectFolder(folder.name)
                                        bottomTabSelection = "all"
                                        showFolderMenu = false
                                    }
                                )
                            }

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text(Localization.get("dialog_add_folder_title", language)) },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    showFolderMenu = false
                                    showAddFolderDialog = true
                                }
                            )
                        }
                    }

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.PushPin, contentDescription = "Pinned Notes") },
                        label = { Text(Localization.get("main_tab_pinned", language)) },
                        selected = bottomTabSelection == "pinned",
                        onClick = { bottomTabSelection = "pinned" }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Checklist, contentDescription = "Checklists") },
                        label = { Text(Localization.get("main_tab_todo", language)) },
                        selected = bottomTabSelection == "checklists",
                        onClick = { bottomTabSelection = "checklists" }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.CloudSync, contentDescription = "Sync Cloud") },
                        label = { Text(Localization.get("main_tab_sync", language)) },
                        selected = bottomTabSelection == "sync",
                        onClick = { bottomTabSelection = "sync" }
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNavigateToEdit(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("create_note_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note", modifier = Modifier.size(28.dp))
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (bottomTabSelection == "sync") {
                    // Dedicated interactive sync view
                    SyncView(viewModel)
                } else if (displayedNotes.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.StickyNote2,
                                contentDescription = "No Notes",
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                Localization.get("main_empty_title", language),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                Localization.get("main_empty_subtitle", language),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    // Standard Notes card list with pull refresh simulator and beautiful layout
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                language = language,
                                onClick = { onNavigateToView(note.id) },
                                onLongClick = { viewModel.togglePin(note) },
                                onDelete = { viewModel.deleteNote(note) },
                                onTogglePin = { viewModel.togglePin(note) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Interactive Dialogs
    if (showAddFolderDialog) {
        val folderName by viewModel.tempNewFolderName.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text(Localization.get("dialog_add_folder_title", language)) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { viewModel.tempNewFolderName.value = it },
                    label = { Text(Localization.get("dialog_add_folder_label", language)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addCustomFolder(folderName)
                    showAddFolderDialog = false
                }) { Text(Localization.get("dialog_confirm", language)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddFolderDialog = false }) { Text(Localization.get("dialog_cancel", language)) }
            }
        )
    }

    if (showAddTagDialog) {
        val tagName by viewModel.tempNewTagName.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text(Localization.get("dialog_add_tag_title", language)) },
            text = {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { viewModel.tempNewTagName.value = it },
                    label = { Text(Localization.get("dialog_add_tag_label", language)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addCustomTag(tagName)
                    showAddTagDialog = false
                }) { Text(Localization.get("dialog_confirm", language)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) { Text(Localization.get("dialog_cancel", language)) }
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(viewModel = viewModel, onDismiss = { showSettingsDialog = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    language: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit
) {
    val dateStr = remember(note.timestamp, language) {
        val locale = if (language == "en") Locale.ENGLISH else Locale.TRADITIONAL_CHINESE
        val formatPattern = if (language == "en") "MMM dd, HH:mm" else "MM月dd日 HH:mm"
        val sdf = SimpleDateFormat(formatPattern, locale)
        sdf.format(Date(note.timestamp))
    }
    val checklistItems = remember(note.checklistJson) {
        com.example.data.ChecklistSerializer.fromJson(note.checklistJson)
    }
    val hasChecklist = checklistItems.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                1.dp,
                if (note.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Note Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Localization.getNoteTitle(note.title, language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = Localization.get("note_pinned", language),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onTogglePin() }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = Localization.get("note_delete", language),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onDelete() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Attached image preview if exists
            val noteImages = remember(note.imageUrl) { note.getImageUrlList() }
            if (noteImages.isNotEmpty()) {
                if (noteImages.size == 1) {
                    AsyncImage(
                        model = noteImages.first(),
                        contentDescription = Localization.get("note_image_desc", language),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        noteImages.take(3).forEachIndexed { idx, url ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (idx == 2 && noteImages.size > 3) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${noteImages.size - 2}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Preview Text content
            val contentText = remember(note.content, language) {
                Localization.getNoteContent(note.content, language)
            }
            val annotatedContent = remember(contentText) {
                htmlToAnnotatedString(contentText)
            }
            Text(
                text = annotatedContent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Checklist preview
            if (hasChecklist) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    checklistItems.take(2).forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (item.checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (item.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Localization.getChecklistItemText(item.text, language),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = if (item.checked) TextDecoration.LineThrough else null
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (checklistItems.size > 2) {
                        Text(
                            text = Localization.get("note_more_tasks", language).format(checklistItems.size - 2),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer of card containing tags & timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                // Render Tag Chips
                if (note.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        note.tags.split(",").take(2).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = Localization.getTagName(tag.trim(), language),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
