package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.ChecklistItem
import com.example.data.Folder
import com.example.data.Note
import com.example.data.Tag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Simple State Navigation Screens
sealed class Screen {
    object Splash : Screen()
    object Main : Screen()
    data class ViewNote(val noteId: Int) : Screen()
    data class EditNote(val noteId: Int?) : Screen()
}

@Composable
fun AppNavigation(viewModel: NoteViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDarkTheme) MaterialTheme.colorScheme else MaterialTheme.colorScheme
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Splash -> SplashScreen(
                        language = language,
                        onSplashFinished = { currentScreen = Screen.Main }
                    )
                    is Screen.Main -> MainScreen(
                        viewModel = viewModel,
                        onNavigateToView = { id -> currentScreen = Screen.ViewNote(id) },
                        onNavigateToEdit = { id -> currentScreen = Screen.EditNote(id) }
                    )
                    is Screen.ViewNote -> ViewNoteScreen(
                        viewModel = viewModel,
                        noteId = screen.noteId,
                        onBack = { currentScreen = Screen.Main },
                        onNavigateToEdit = { id -> currentScreen = Screen.EditNote(id) }
                    )
                    is Screen.EditNote -> EditNoteScreen(
                        viewModel = viewModel,
                        noteId = screen.noteId,
                        onBack = { currentScreen = Screen.Main }
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(language: String, onSplashFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1600)
        onSplashFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scalePulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F111A),
                        Color(0xFF1B1D2A),
                        Color(0xFF13151F)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Radiant Glowing Logo container
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(scale)
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFB19DFF),
                                Color(0xFF4FC3F7)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Logo Pen",
                    tint = Color(0xFF101116),
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "ZenNote",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Localization.get("splash_subtitle", language),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF90A4AE),
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(64.dp))

            CircularProgressIndicator(
                color = Color(0xFFB19DFF),
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// ==========================================
// 2. MAIN SCREEN
// ==========================================
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
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Bottom Navigation Bar selection state
    var bottomTabSelection by remember { mutableStateOf("all") } // "all", "pinned", "checklists", "sync"

    // Custom filtering based on bottom tab selection
    val displayedNotes = remember(notes, bottomTabSelection) {
        when (bottomTabSelection) {
            "pinned" -> notes.filter { it.isPinned }
            "checklists" -> notes.filter { it.checklistJson.isNotEmpty() && it.checklistJson != "[]" }
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
                    items(folders) { folder ->
                        val isSelected = selectedFolder == folder.name
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Folder, contentDescription = "Folder") },
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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
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
                    items(tags) { tag ->
                        val isSelected = selectedTag == tag.name
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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
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
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Notes, contentDescription = "All Notes") },
                        label = { Text(Localization.get("main_tab_all", language)) },
                        selected = bottomTabSelection == "all",
                        onClick = { bottomTabSelection = "all" }
                    )
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

// ==========================================
// 3. NOTE CARD COMPONENT
// ==========================================
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
            if (!note.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = note.imageUrl,
                    contentDescription = Localization.get("note_image_desc", language),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Preview Text content
            Text(
                text = Localization.getNoteContent(note.content, language),
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

// ==========================================
// 4. VIEW NOTE SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewNoteScreen(
    viewModel: NoteViewModel,
    noteId: Int,
    onBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val noteFlow = remember(noteId) { viewModel.getNoteByIdFlow(noteId) }
    val note by noteFlow.collectAsStateWithLifecycle(initialValue = null)
    val language by viewModel.language.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Localization.get("view_title", language), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Localization.get("edit_back", language))
                    }
                },
                actions = {
                    note?.let { n ->
                        IconButton(onClick = { viewModel.togglePin(n) }) {
                            Icon(
                                imageVector = if (n.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                                contentDescription = Localization.get("note_pinned", language),
                                tint = if (n.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            Toast.makeText(context, Localization.get("view_link_copied", language), Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Share, contentDescription = Localization.get("view_share", language))
                        }
                        IconButton(onClick = {
                            viewModel.deleteNote(n)
                            onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = Localization.get("view_delete", language))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEdit(noteId) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Edit, contentDescription = Localization.get("view_edit", language))
            }
        }
    ) { innerPadding ->
        note?.let { n ->
            val dateStr = remember(n.timestamp, language) {
                val locale = if (language == "en") Locale.ENGLISH else Locale.TRADITIONAL_CHINESE
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)
                sdf.format(Date(n.timestamp))
            }
            val checklist = remember(n.checklistJson) {
                com.example.data.ChecklistSerializer.fromJson(n.checklistJson)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header details
                Text(
                    text = Localization.getNoteTitle(n.title, language),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = "Folder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.get("view_folder", language).format(Localization.getFolderName(n.folder, language)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tag lists
                if (n.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        n.tags.split(",").forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = Localization.getTagName(tag.trim(), language),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Image Banner if exists
                if (!n.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = n.imageUrl,
                        contentDescription = Localization.get("view_hero_image", language),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Note content body text
                Text(
                    text = Localization.getNoteContent(n.content, language),
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Checklist Section (Fully interactive checklist toggles)
                if (checklist.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = Localization.get("view_todo_title", language),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            checklist.forEachIndexed { idx, item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleChecklistItem(n, idx) }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = "Toggle Checklist Item",
                                        tint = if (item.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = Localization.getChecklistItemText(item.text, language),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            textDecoration = if (item.checked) TextDecoration.LineThrough else null
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
}

// Simple FlowRow helper for tags list
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = horizontalArrangement,
        modifier = modifier,
        content = { content() }
    )
}

// ==========================================
// 5. EDIT NOTE SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteScreen(
    viewModel: NoteViewModel,
    noteId: Int?,
    onBack: () -> Unit
) {
    LaunchedEffect(noteId) {
        viewModel.startEditing(noteId)
    }

    val editingTitle by viewModel.editingTitle.collectAsStateWithLifecycle()
    val editingContent by viewModel.editingContent.collectAsStateWithLifecycle()
    val editingFolder by viewModel.editingFolder.collectAsStateWithLifecycle()
    val editingTags by viewModel.editingTags.collectAsStateWithLifecycle()
    val editingChecklist by viewModel.editingChecklist.collectAsStateWithLifecycle()
    val editingImageUrl by viewModel.editingImageUrl.collectAsStateWithLifecycle()

    val folders by viewModel.allFolders.collectAsStateWithLifecycle()
    val tags by viewModel.allTags.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    var showFolderMenu by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var newChecklistText by remember { mutableStateOf("") }
    var isRecordingWave by remember { mutableStateOf(false) } // Interactive Voice recording simulation

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) Localization.get("edit_create_title", language) else Localization.get("edit_edit_title", language), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Localization.get("edit_back", language))
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveCurrentNote(onComplete = onBack)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_note_button")
                    ) {
                        Text(Localization.get("edit_save", language))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Note Title input (Unstyled no border look)
            OutlinedTextField(
                value = editingTitle,
                onValueChange = { viewModel.editingTitle.value = it },
                placeholder = { Text(Localization.get("edit_title_placeholder", language), fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Folder selector bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { showFolderMenu = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Folder, contentDescription = "Folder", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                val localizedFolderName = Localization.getFolderName(editingFolder, language)
                Text(
                    text = Localization.get("edit_select_folder", language).format(localizedFolderName),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")

                DropdownMenu(
                    expanded = showFolderMenu,
                    onDismissRequest = { showFolderMenu = false }
                ) {
                    folders.forEach { f ->
                        val label = Localization.getFolderName(f.name, language)
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setFolderInDraft(f.name)
                                showFolderMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tags edit row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { showTagDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Label, contentDescription = "Tags", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Localization.get("edit_add_edit_tags", language), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.width(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(editingTags) { tag ->
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = Localization.getTagName(tag, language),
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = Localization.get("edit_remove_tag", language),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { viewModel.toggleTagInDraft(tag) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Attached image Banner with delete option
            if (editingImageUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = editingImageUrl,
                        contentDescription = Localization.get("edit_draft_image", language),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { viewModel.clearAttachedImage() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = Localization.get("edit_delete_image", language), tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Checklist drafting input & list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    Localization.get("edit_todo_section", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Items list
                editingChecklist.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Toggle",
                            tint = if (item.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { viewModel.toggleChecklistItemInDraft(idx) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = if (item.checked) TextDecoration.LineThrough else null
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.removeChecklistItemFromDraft(idx) }) {
                            Icon(Icons.Default.Delete, contentDescription = Localization.get("edit_remove_todo", language), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Add checklist item field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newChecklistText,
                        onValueChange = { newChecklistText = it },
                        placeholder = { Text(Localization.get("edit_add_todo_placeholder", language)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            viewModel.addChecklistItemToDraft(newChecklistText)
                            newChecklistText = ""
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.addChecklistItemToDraft(newChecklistText)
                            newChecklistText = ""
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = Localization.get("edit_add_todo_desc", language), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text Content input field
            OutlinedTextField(
                value = editingContent,
                onValueChange = { viewModel.editingContent.value = it },
                placeholder = { Text(Localization.get("edit_content_placeholder", language), fontSize = 16.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // RICH formatting toolbars
            Text(Localization.get("edit_toolbar_title", language), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(10.dp))

            // Images / Checklists / Tags / Style formatting panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Attach visual image
                Button(
                    onClick = { viewModel.attachRandomImage() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Insert Image", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Localization.get("edit_insert_image", language), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }

                // Voice Recording simulator with cute waveform
                Button(
                    onClick = {
                        isRecordingWave = !isRecordingWave
                        if (isRecordingWave) {
                            Toast.makeText(context, Localization.get("edit_recording_toast", language), Toast.LENGTH_SHORT).show()
                            scope.launch {
                                delay(3000)
                                if (isRecordingWave) {
                                    viewModel.editingContent.value += Localization.get("edit_voice_simulation_text", language)
                                    isRecordingWave = false
                                    Toast.makeText(context, Localization.get("edit_voice_success", language), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecordingWave) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isRecordingWave) Icons.Default.MicNone else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isRecordingWave) Color.White else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isRecordingWave) Localization.get("edit_recording", language) else Localization.get("edit_voice_input", language),
                        color = if (isRecordingWave) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bold/Italic formatting tools adding markdown signs
            val formattingItems = remember(language) {
                listOf(
                    (if (language == "en") "Bold" else "粗體") to "**${if (language == "en") "Bold" else "粗體"}**",
                    (if (language == "en") "Italic" else "斜體") to "*${if (language == "en") "Italic" else "斜體"}*",
                    (if (language == "en") "Link" else "超連結") to "[${if (language == "en") "Link" else "連結標題"}](https://example.com)",
                    (if (language == "en") "List" else "清單列表") to if (language == "en") "\n- Item 1\n- Item 2" else "\n- 項目1\n- 項目2"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                formattingItems.forEach { (label, code) ->
                    TextButton(
                        onClick = {
                            viewModel.editingContent.value += code
                        },
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    // Edit Tags Multi-selector Dialog
    if (showTagDialog) {
        Dialog(onDismissRequest = { showTagDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        Localization.get("dialog_tags_title", language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(tags) { tag ->
                            val isChecked = editingTags.contains(tag.name)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleTagInDraft(tag.name) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { viewModel.toggleTagInDraft(tag.name) }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(
                                            color = Color(android.graphics.Color.parseColor(tag.colorHex)),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Localization.getTagName(tag.name, language), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showTagDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(Localization.get("done", language))
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. SYNC MANAGEMENT VIEW
// ==========================================
@Composable
fun SyncView(viewModel: NoteViewModel) {
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncedTime by viewModel.lastSyncedTime.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "syncRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "Syncing cloud",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(54.dp)
                        .drawBehind {
                            if (isSyncing) {
                                // Rotate icon
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isSyncing) Localization.get("sync_loading", language) else Localization.get("sync_success", language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val localizedSyncTime = remember(lastSyncedTime, language) {
                if (lastSyncedTime == "Not synced yet") {
                    Localization.get("sync_default_time", language)
                } else {
                    lastSyncedTime.replace("Synced:", if (language == "en") "Synced at:" else "已同步於:")
                }
            }

            Text(
                text = localizedSyncTime,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = Localization.get("sync_desc", language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.triggerSync() },
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isSyncing) Localization.get("sync_status_prefix", language) else Localization.get("sync_now_btn", language))
            }
        }
    }
}

// ==========================================
// 7. SETTINGS DIALOG
// ==========================================
@Composable
fun SettingsDialog(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    Localization.get("settings_title", language),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Theme mode selection
                Text(Localization.get("settings_theme_title", language), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val themeOptions = remember(language) {
                    listOf(
                        "dark" to Localization.get("settings_theme_dark", language),
                        "light" to Localization.get("settings_theme_light", language),
                        "system" to Localization.get("settings_theme_system", language)
                    )
                }

                themeOptions.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.themeMode.value = value }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == value,
                            onClick = { viewModel.themeMode.value = value }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Language selection
                Text(Localization.get("settings_language_title", language), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val languageOptions = remember(language) {
                    listOf(
                        "zh" to Localization.get("settings_lang_zh", language),
                        "en" to Localization.get("settings_lang_en", language)
                    )
                }

                languageOptions.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.language.value = value }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == value,
                            onClick = { viewModel.language.value = value }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Local backup option
                Text(Localization.get("settings_backup_title", language), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        Toast.makeText(context, Localization.get("settings_backup_toast", language), Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = "Backup")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Localization.get("settings_backup_btn", language))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        Toast.makeText(context, Localization.get("settings_restore_toast", language), Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Localization.get("settings_restore_btn", language), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(Localization.get("done", language))
                }
            }
        }
    }
}
