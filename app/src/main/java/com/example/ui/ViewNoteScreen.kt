package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// VIEW NOTE SCREEN
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
    var showFullImageViewer by remember { mutableStateOf(false) }

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
                                imageVector = Icons.Default.PushPin,
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
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showFullImageViewer = true },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (showFullImageViewer) {
                        FullScreenImageViewer(
                            imageUrl = n.imageUrl,
                            onDismiss = { showFullImageViewer = false }
                        )
                    }
                }

                // Note content body text
                val contentText = remember(n.content, language) {
                    Localization.getNoteContent(n.content, language)
                }
                val annotatedContent = remember(contentText) {
                    htmlToAnnotatedString(contentText)
                }
                Text(
                    text = annotatedContent,
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
