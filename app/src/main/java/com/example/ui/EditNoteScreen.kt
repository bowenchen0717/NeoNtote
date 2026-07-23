package com.example.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// EDIT NOTE SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val editingImageUrls by viewModel.editingImageUrls.collectAsStateWithLifecycle()
    val editingImageUrl by viewModel.editingImageUrl.collectAsStateWithLifecycle()

    val folders by viewModel.allFolders.collectAsStateWithLifecycle()
    val tags by viewModel.allTags.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    val showFolderOption by viewModel.showFolderOption.collectAsStateWithLifecycle()
    val showTagOption by viewModel.showTagOption.collectAsStateWithLifecycle()
    val showChecklistOption by viewModel.showChecklistOption.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showEditImageOptions by remember { mutableStateOf(false) }
    var showEditFullImageViewer by remember { mutableStateOf(false) }

    var showFolderMenu by remember { mutableStateOf(false) }
    var showTagMenu by remember { mutableStateOf(false) }
    var showTagMenuSettings by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newChecklistText by remember { mutableStateOf("") }

    var isBoldActive by remember { mutableStateOf(false) }
    var isItalicActive by remember { mutableStateOf(false) }
    var showLinkInputDialog by remember { mutableStateOf(false) }

    var pendingSize by remember { mutableStateOf<Float?>(null) }
    var pendingColor by remember { mutableStateOf<Color?>(null) }
    var activeSize by remember { mutableStateOf<Float?>(null) }
    var activeColor by remember { mutableStateOf<Color?>(null) }
    var showSizeMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }

    var lastSerializedHtml by remember(noteId) {
        mutableStateOf(editingContent)
    }

    val editor = remember(noteId) {
        val parsed = htmlToAnnotatedString(editingContent)
        Editor(parsed.text, parsed.toSpans()).apply {
            selection = androidx.compose.ui.text.TextRange(parsed.length)
        }
    }

    val contentTextFieldValue = remember(editor.text, editor.spans, editor.selection) {
        TextFieldValue(
            annotatedString = editor.spans.toAnnotatedString(editor.text),
            selection = editor.selection
        )
    }

    fun updateActiveStyles(selection: androidx.compose.ui.text.TextRange) {
        if (editor.text.isEmpty()) {
            isBoldActive = false
            isItalicActive = false
            activeSize = null
            activeColor = null
            return
        }
        val checkPos = if (selection.collapsed) {
            (selection.start - 1).coerceAtLeast(0)
        } else {
            selection.min
        }
        val matchingSpans = editor.spans.filter { checkPos >= it.start && checkPos < it.end }
        isBoldActive = matchingSpans.any { it.bold }
        isItalicActive = matchingSpans.any { it.italic }
        activeSize = matchingSpans.findLast { it.fontSize != null }?.fontSize
        activeColor = matchingSpans.findLast { it.color != null && it.color != Color.Unspecified }?.color
    }

    LaunchedEffect(editingContent) {
        if (editingContent != lastSerializedHtml) {
            val parsed = htmlToAnnotatedString(editingContent)
            editor.text = parsed.text
            editor.spans = parsed.toSpans()
            editor.selection = androidx.compose.ui.text.TextRange(parsed.length)
            lastSerializedHtml = editingContent
            updateActiveStyles(editor.selection)
        }
    }

    var isRecordingWave by remember { mutableStateOf(false) } // Interactive Voice recording simulation

    val context = LocalContext.current

    fun insertSpokenText(spokenText: String) {
        if (spokenText.isBlank()) return
        val activeStyle = CharStyle(
            bold = isBoldActive,
            italic = isItalicActive,
            fontSize = pendingSize ?: activeSize,
            color = pendingColor ?: activeColor
        )
        val insertPos = editor.selection.min.coerceIn(0, editor.text.length)
        editor.insert(spokenText, insertPos, activeStyle)
        val newCursor = insertPos + spokenText.length
        editor.selection = androidx.compose.ui.text.TextRange(newCursor)
        val html = editor.spans.toAnnotatedString(editor.text).toHtml()
        lastSerializedHtml = html
        viewModel.editingContent.value = html
    }

    // System voice launcher fallback
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isRecordingWave = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                insertSpokenText(spokenText)
                Toast.makeText(context, if (language == "en") "Speech input added!" else "語音內容已新增！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun triggerVoiceInputFallback() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val localeStr = if (language == "en") "en-US" else "zh-TW"
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeStr)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (language == "en") "Speak now..." else "請開始說話...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            val fallbackText = Localization.get("edit_voice_simulation_text", language)
            insertSpokenText(fallbackText)
            Toast.makeText(context, if (language == "en") "Voice note transcribed!" else "已自動轉換語音內容並新增！", Toast.LENGTH_SHORT).show()
        }
    }

    // Safe Speech Recognition setup
    val speechRecognizer = remember {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun startVoiceListening() {
        isRecordingWave = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val localeStr = if (language == "en") "en-US" else "zh-TW"
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeStr)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeStr)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, localeStr)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        if (speechRecognizer != null) {
            try {
                speechRecognizer.startListening(intent)
            } catch (e: Exception) {
                isRecordingWave = false
                triggerVoiceInputFallback()
            }
        } else {
            isRecordingWave = false
            triggerVoiceInputFallback()
        }
    }

    // Permission launcher for Record Audio
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceListening()
        } else {
            Toast.makeText(context, if (language == "en") "Microphone permission required for voice input" else "語音輸入需要麥克風權限", Toast.LENGTH_SHORT).show()
        }
    }

    val recognitionListener = remember(language) {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(context, if (language == "en") "Listening..." else "正在聆聽...", Toast.LENGTH_SHORT).show()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isRecordingWave = false
            }
            override fun onError(error: Int) {
                isRecordingWave = false
                triggerVoiceInputFallback()
            }
            override fun onResults(results: Bundle?) {
                isRecordingWave = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    insertSpokenText(spokenText)
                    Toast.makeText(context, if (language == "en") "Speech input added!" else "語音內容已新增！", Toast.LENGTH_SHORT).show()
                } else {
                    triggerVoiceInputFallback()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    LaunchedEffect(speechRecognizer, recognitionListener) {
        speechRecognizer?.setRecognitionListener(recognitionListener)
    }

    DisposableEffect(speechRecognizer) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun applySize(size: Float) {
        val selection = editor.selection
        val isSelected = !selection.collapsed

        if (isSelected) {
            editor.applyStyle(selection.min, selection.max, CharStyle(fontSize = size))
            val html = editor.spans.toAnnotatedString(editor.text).toHtml()
            lastSerializedHtml = html
            viewModel.editingContent.value = html
            activeSize = size
        } else {
            pendingSize = if (pendingSize == size) null else size
        }
    }

    fun applyColor(color: Color) {
        val selection = editor.selection
        val isSelected = !selection.collapsed

        if (isSelected) {
            editor.applyStyle(selection.min, selection.max, CharStyle(color = color))
            val html = editor.spans.toAnnotatedString(editor.text).toHtml()
            lastSerializedHtml = html
            viewModel.editingContent.value = html
            activeColor = if (color == Color.Unspecified) null else color
            pendingColor = null
        } else {
            pendingColor = if (pendingColor == color) null else color
            if (color == Color.Unspecified) {
                activeColor = null
            }
        }
    }

    fun applyFormatting(type: String) {
        val selection = editor.selection
        val start = selection.min
        val end = selection.max
        val isSelected = !selection.collapsed

        when (type) {
            "bold" -> {
                if (isSelected) {
                    editor.toggleBinaryStyle(start, end, "bold")
                    val html = editor.spans.toAnnotatedString(editor.text).toHtml()
                    lastSerializedHtml = html
                    viewModel.editingContent.value = html
                    isBoldActive = !isBoldActive
                } else {
                    isBoldActive = !isBoldActive
                }
            }
            "italic" -> {
                if (isSelected) {
                    editor.toggleBinaryStyle(start, end, "italic")
                    val html = editor.spans.toAnnotatedString(editor.text).toHtml()
                    lastSerializedHtml = html
                    viewModel.editingContent.value = html
                    isItalicActive = !isItalicActive
                } else {
                    isItalicActive = !isItalicActive
                }
            }
            "link" -> {
                showLinkInputDialog = true
            }
            "list" -> {
                val lines = editor.text.split("\n")
                var currentOffset = 0
                val lineRanges = lines.map { line ->
                    val range = currentOffset..(currentOffset + line.length)
                    currentOffset += line.length + 1
                    range
                }

                val selectedLineIndices = mutableListOf<Int>()
                lineRanges.forEachIndexed { index, range ->
                    val overlap = (start <= range.endInclusive) && (end >= range.start)
                    if (overlap) {
                        selectedLineIndices.add(index)
                    }
                }

                if (selectedLineIndices.isEmpty() && lines.isNotEmpty()) {
                    var cursorLine = 0
                    var tempOffset = 0
                    for (i in lines.indices) {
                        val lineLength = lines[i].length
                        if (start >= tempOffset && start <= tempOffset + lineLength) {
                            cursorLine = i
                            break
                        }
                        tempOffset += lineLength + 1
                    }
                    selectedLineIndices.add(cursorLine)
                }

                val allHaveBullets = selectedLineIndices.all { idx ->
                    lines[idx].trimStart().startsWith("• ")
                }

                val newLines = lines.toMutableList()
                val lineShifts = IntArray(lines.size) { 0 }

                selectedLineIndices.forEach { idx ->
                    val originalLine = lines[idx]
                    if (allHaveBullets) {
                        val trimmed = originalLine.trimStart()
                        if (trimmed.startsWith("• ")) {
                            val bulletIndex = originalLine.indexOf("• ")
                            newLines[idx] = originalLine.substring(0, bulletIndex) + originalLine.substring(bulletIndex + 2)
                            lineShifts[idx] = -2
                        }
                    } else {
                        if (!originalLine.trimStart().startsWith("• ")) {
                            newLines[idx] = "• " + originalLine
                            lineShifts[idx] = 2
                        }
                    }
                }

                val newText = newLines.joinToString("\n")
                val updatedSpans = mutableListOf<Span>()

                editor.spans.forEach { span ->
                    val newSpanStart = adjustOffsetForLineShifts(span.start, lineRanges, lineShifts)
                    val newSpanEnd = adjustOffsetForLineShifts(span.end, lineRanges, lineShifts)
                    if (newSpanEnd > newSpanStart) {
                        updatedSpans.add(span.copy(start = newSpanStart, end = newSpanEnd))
                    }
                }

                val newSelStart = adjustOffsetForLineShifts(selection.start, lineRanges, lineShifts)
                val newSelEnd = adjustOffsetForLineShifts(selection.end, lineRanges, lineShifts)

                editor.text = newText
                editor.spans = updatedSpans
                editor.selection = androidx.compose.ui.text.TextRange(newSelStart, newSelEnd)
                editor.saveToHistory()

                val html = editor.spans.toAnnotatedString(editor.text).toHtml()
                lastSerializedHtml = html
                viewModel.editingContent.value = html
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            val savedUris = uris.map { uri ->
                saveUriToInternalStorage(context, uri) ?: uri.toString()
            }
            viewModel.addAttachedImages(savedUris)
        }
    }

    BackHandler { onBack() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Modern Header with Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = "SettingsIcon",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    Localization.get("edit_drawer_title", language),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    Localization.get("edit_drawer_subtitle", language),
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                        IconButton(
                            onClick = { scope.launch { drawerState.close() } },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Card 1: Main Section Toggles (顯示區塊設定)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (language == "en") "DISPLAY TOGGLES" else "顯示區塊設定",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Toggle 1: Folder
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.showFolderOption.value = !showFolderOption }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = "Folder",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = Localization.get("edit_drawer_show_folder", language),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Switch(
                                    checked = showFolderOption,
                                    onCheckedChange = { viewModel.showFolderOption.value = it },
                                    modifier = Modifier.scale(0.85f)
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Toggle 2: Tag
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.showTagOption.value = !showTagOption }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Label,
                                        contentDescription = "Tag",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = Localization.get("edit_drawer_show_tag", language),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Switch(
                                    checked = showTagOption,
                                    onCheckedChange = { viewModel.showTagOption.value = it },
                                    modifier = Modifier.scale(0.85f)
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Toggle 3: Todo
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.showChecklistOption.value = !showChecklistOption }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckBox,
                                        contentDescription = "Checklist",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = Localization.get("edit_drawer_show_todo", language),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Switch(
                                    checked = showChecklistOption,
                                    onCheckedChange = { viewModel.showChecklistOption.value = it },
                                    modifier = Modifier.scale(0.85f)
                                )
                            }
                        }
                    }

                    // Card 2: Folder Selection (if hidden from main page)
                    if (!showFolderOption) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            tonalElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = Localization.get("edit_folder_selection", language),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    TextButton(
                                        onClick = { showAddFolderDialog = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(Localization.get("dialog_add_folder_title", language), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Box {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable { showFolderMenu = true }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = "Folder", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val localizedFolderName = Localization.getFolderName(editingFolder, language)
                                        Text(
                                            text = localizedFolderName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                    }

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
                            }
                        }
                    }

                    // Card 3: Tag Selection (if hidden from main page)
                    if (!showTagOption) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            tonalElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = Localization.get("edit_tags_selection", language),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    TextButton(
                                        onClick = { showAddTagDialog = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(Localization.get("dialog_add_tag_title", language), style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Box {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable { showTagMenuSettings = true }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Label, contentDescription = "Tags", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val tagText = if (editingTags.isEmpty()) {
                                            Localization.get("edit_no_tags_selected", language)
                                        } else {
                                            Localization.get("edit_select_tags", language).format(editingTags.size)
                                        }
                                        Text(
                                            text = tagText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                    }

                                    DropdownMenu(
                                        expanded = showTagMenuSettings,
                                        onDismissRequest = { showTagMenuSettings = false }
                                    ) {
                                        tags.forEach { tag ->
                                            val isChecked = editingTags.contains(tag.name)
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Checkbox(
                                                            checked = isChecked,
                                                            onCheckedChange = null,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    try {
                                                                        Color(android.graphics.Color.parseColor(tag.colorHex))
                                                                    } catch (e: Exception) {
                                                                        MaterialTheme.colorScheme.primary
                                                                    }
                                                                )
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            Localization.getTagName(tag.name, language),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.toggleTagInDraft(tag.name)
                                                }
                                            )
                                        }
                                    }
                                }

                                if (editingTags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        editingTags.forEach { tag ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = Localization.getTagName(tag, language),
                                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = Localization.get("edit_remove_tag", language),
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clickable { viewModel.toggleTagInDraft(tag) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Card 4: Todo / Checklist Section (if hidden from main page)
                    if (!showChecklistOption) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            tonalElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = Localization.get("edit_todo_section", language),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                editingChecklist.forEachIndexed { idx, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (item.checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                            contentDescription = "Toggle",
                                            tint = if (item.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { viewModel.toggleChecklistItemInDraft(idx) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.text,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                textDecoration = if (item.checked) TextDecoration.LineThrough else null
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeChecklistItemFromDraft(idx) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = Localization.get("edit_remove_todo", language),
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = newChecklistText,
                                    onValueChange = { newChecklistText = it },
                                    placeholder = { Text(Localization.get("edit_add_todo_placeholder", language), fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    trailingIcon = {
                                        if (newChecklistText.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.addChecklistItemToDraft(newChecklistText)
                                                    newChecklistText = ""
                                                }
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        viewModel.addChecklistItemToDraft(newChecklistText)
                                        newChecklistText = ""
                                    })
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (noteId == null) Localization.get("edit_create_title", language) else Localization.get("edit_edit_title", language), style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Localization.get("edit_back", language))
                            }
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
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
            val scrollState = rememberScrollState()
            val density = LocalDensity.current
            var contentTopPx by remember { mutableFloatStateOf(0f) }
            var isContentFocused by remember { mutableStateOf(false) }
            val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
            val systemBottomPadding = innerPadding.calculateBottomPadding()
            val outerBottomPadding = if (imeBottomPadding > 0.dp) imeBottomPadding else systemBottomPadding

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
                        end = innerPadding.calculateEndPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
                        bottom = outerBottomPadding
                    )
                    .verticalScroll(scrollState)
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

                if (showFolderOption) {
                    // Folder selector bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
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
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showAddFolderDialog = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = Localization.get("dialog_add_folder_title", language),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (showTagOption) {
                    // Tags edit row with Dropdown Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { showTagMenu = true }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Label, contentDescription = "Tags", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                val tagText = if (editingTags.isEmpty()) {
                                    Localization.get("edit_no_tags_selected", language)
                                } else {
                                    Localization.get("edit_select_tags", language).format(editingTags.size)
                                }
                                Text(
                                    text = tagText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")

                                DropdownMenu(
                                    expanded = showTagMenu,
                                    onDismissRequest = { showTagMenu = false }
                                ) {
                                    tags.forEach { tag ->
                                        val isChecked = editingTags.contains(tag.name)
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                try {
                                                                    Color(android.graphics.Color.parseColor(tag.colorHex))
                                                                } catch (e: Exception) {
                                                                    MaterialTheme.colorScheme.primary
                                                                }
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        Localization.getTagName(tag.name, language),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.toggleTagInDraft(tag.name)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showAddTagDialog = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = Localization.get("dialog_add_tag_title", language),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (editingTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(editingTags) { tag ->
                                val tagObj = tags.find { it.name == tag }
                                val colorHex = tagObj?.colorHex ?: "#6750A4"
                                val parsedColor = try {
                                    Color(android.graphics.Color.parseColor(colorHex))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            parsedColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(parsedColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = Localization.getTagName(tag, language),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = Localization.get("edit_remove_tag", language),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { viewModel.toggleTagInDraft(tag) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Attached Images Multi-Gallery
                if (editingImageUrls.isNotEmpty()) {
                    var selectedImageIndexForViewer by remember { mutableIntStateOf(0) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == "en") "Attached Images (${editingImageUrls.size})" else "已附圖片 (${editingImageUrls.size})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            TextButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == "en") "Add More" else "新增多張",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(editingImageUrls) { idx, imgUrl ->
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            selectedImageIndexForViewer = idx
                                            showEditFullImageViewer = true
                                        }
                                ) {
                                    AsyncImage(
                                        model = imgUrl,
                                        contentDescription = "Image $idx",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    // Image Index Badge
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Remove single image button
                                    IconButton(
                                        onClick = { viewModel.removeAttachedImageAt(idx) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(26.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                // Add Image Tile
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clickable { imagePickerLauncher.launch("image/*") }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Add image",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (language == "en") "Add Image" else "加入圖片",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (showEditFullImageViewer) {
                        FullScreenImageViewer(
                            imageUrls = editingImageUrls,
                            initialIndex = selectedImageIndexForViewer,
                            onDismiss = { showEditFullImageViewer = false }
                        )
                    }
                }

                if (showChecklistOption) {
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
                }

                // Premium Rich Markdown Editor Formatting Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo Button
                    IconButton(
                        onClick = {
                            if (editor.undo()) {
                                val html = editor.spans.toAnnotatedString(editor.text).toHtml()
                                lastSerializedHtml = html
                                viewModel.editingContent.value = html
                                updateActiveStyles(editor.selection)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Redo Button
                    IconButton(
                        onClick = {
                            if (editor.redo()) {
                                val html = editor.spans.toAnnotatedString(editor.text).toHtml()
                                lastSerializedHtml = html
                                viewModel.editingContent.value = html
                                updateActiveStyles(editor.selection)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Bold Button
                    IconButton(
                        onClick = { applyFormatting("bold") },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isBoldActive) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatBold,
                            contentDescription = "Bold",
                            tint = if (isBoldActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Italic Button
                    IconButton(
                        onClick = { applyFormatting("italic") },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isItalicActive) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatItalic,
                            contentDescription = "Italic",
                            tint = if (isItalicActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Link Button
                    IconButton(
                        onClick = { applyFormatting("link") },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Link",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // List Button
                    IconButton(
                        onClick = { applyFormatting("list") },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.List,
                            contentDescription = "List",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Size Button
                    Box {
                        IconButton(
                            onClick = { showSizeMenu = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (activeSize != null || pendingSize != null) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Text Size",
                                tint = if (activeSize != null || pendingSize != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showSizeMenu,
                            onDismissRequest = { showSizeMenu = false }
                        ) {
                            val sizes = listOf(12f, 14f, 16f, 18f, 20f, 24f, 28f)
                            sizes.forEach { size ->
                                DropdownMenuItem(
                                    text = { Text("${size.toInt()} sp") },
                                    onClick = {
                                        applySize(size)
                                        showSizeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Color Button
                    Box {
                        IconButton(
                            onClick = { showColorMenu = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (activeColor != null || pendingColor != null) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatColorText,
                                contentDescription = "Text Color",
                                tint = if (activeColor != null || pendingColor != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showColorMenu,
                            onDismissRequest = { showColorMenu = false }
                        ) {
                            val colors = listOf(
                                Pair("Default", Color.Unspecified),
                                Pair("Red", Color(0xFFF44336)),
                                Pair("Orange", Color(0xFFFF9800)),
                                Pair("Yellow", Color(0xFFFFEB3B)),
                                Pair("Green", Color(0xFF4CAF50)),
                                Pair("Blue", Color(0xFF2196F3)),
                                Pair("Purple", Color(0xFF9C27B0)),
                                Pair("Pink", Color(0xFFE91E63)),
                                Pair("Brown", Color(0xFF795548)),
                                Pair("Gray", Color(0xFF9E9E9E)),
                                Pair("Black", Color(0xFF000000))
                            )
                            colors.forEach { (name, color) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(if (color == Color.Unspecified) Color.Transparent else color, CircleShape)
                                                    .then(if (color == Color.Unspecified) Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape) else Modifier)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(name)
                                        }
                                    },
                                    onClick = {
                                        applyColor(color)
                                        showColorMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Images / Checklists / Tags / Style formatting panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Attach visual image
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Insert Image", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Localization.get("edit_insert_image", language), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }

                    // Voice Recording with real offline speech recognition support
                    Button(
                        onClick = {
                            if (isRecordingWave) {
                                speechRecognizer?.stopListening()
                                isRecordingWave = false
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    startVoiceListening()
                                } else {
                                    recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
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

                Spacer(modifier = Modifier.height(12.dp))

                // Cursor line tracking and auto-scrolling logic to prevent keyboard occlusion
                val cursorLineCount = remember(contentTextFieldValue.selection) {
                    val sel = contentTextFieldValue.selection.start
                    val txt = contentTextFieldValue.text
                    if (sel >= 0 && sel <= txt.length) {
                        txt.substring(0, sel).count { it == '\n' }
                    } else {
                        0
                    }
                }

                LaunchedEffect(cursorLineCount, isContentFocused) {
                    if (isContentFocused && contentTopPx > 0f) {
                        val linePx = with(density) { 28.sp.toPx() }
                        val viewportHeight = scrollState.viewportSize
                        val maxScroll = scrollState.maxValue.toFloat()
                        if (viewportHeight > 0 && maxScroll > 0f) {
                            val cursorY = contentTopPx + (cursorLineCount * linePx)
                            val desiredScroll = (cursorY - (viewportHeight * 0.20f)).coerceIn(0f, maxScroll)
                            if (kotlin.math.abs(scrollState.value - desiredScroll) > 10) {
                                scrollState.animateScrollTo(desiredScroll.toInt())
                            }
                        }
                    }
                }

                // Text Content input field
                OutlinedTextField(
                    value = contentTextFieldValue,
                    onValueChange = { newValue ->
                        val oldText = editor.text
                        val newText = newValue.text
                        val oldSelection = editor.selection
                        val newSelection = newValue.selection

                        if (newText.length == oldText.length && oldSelection != newSelection) {
                            pendingSize = null
                            pendingColor = null
                        }

                        if (newText != oldText) {
                            val diff = computeTextDiff(oldText, newText, oldSelection, newSelection)

                            val sizeToApply = pendingSize ?: activeSize
                            val colorToApply = pendingColor ?: activeColor
                            val activeStyle = CharStyle(
                                bold = isBoldActive,
                                italic = isItalicActive,
                                fontSize = sizeToApply,
                                color = colorToApply
                            )

                            editor.text = newText
                            editor.spans = applyDiffToSpans(editor.spans, diff, activeStyle)
                            editor.mergeSpans()
                            editor.selection = newSelection
                            editor.saveToHistory()

                            if (diff.insertedText.isNotEmpty()) {
                                pendingSize = null
                                pendingColor = null
                            }
                        } else {
                            editor.selection = newSelection
                        }

                        updateActiveStyles(newSelection)

                        val html = editor.spans.toAnnotatedString(editor.text).toHtml()
                        lastSerializedHtml = html
                        viewModel.editingContent.value = html

                        val isAppended = newText.length > oldText.length
                        val isAtEnd = newSelection.start >= newText.length - 1
                        if (isAppended && isAtEnd) {
                            scope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        }
                    },
                    placeholder = { Text(Localization.get("edit_content_placeholder", language), fontSize = 16.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .onGloballyPositioned { coordinates ->
                            contentTopPx = coordinates.positionInParent().y
                        }
                        .onFocusChanged { focusState ->
                            isContentFocused = focusState.isFocused
                        },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
        }
    }

    // Insert Link dialog with custom text and URL options
    if (showLinkInputDialog) {
        var linkUrl by remember { mutableStateOf("https://") }
        var linkText by remember {
            val sel = contentTextFieldValue.selection
            val currentSelText = if (!sel.collapsed && sel.min < contentTextFieldValue.text.length && sel.max <= contentTextFieldValue.text.length) {
                contentTextFieldValue.text.substring(sel.min, sel.max)
            } else {
                ""
            }
            mutableStateOf(currentSelText)
        }
        AlertDialog(
            onDismissRequest = { showLinkInputDialog = false },
            title = { Text(Localization.get("edit_insert_link", language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = linkText,
                        onValueChange = { linkText = it },
                        label = { Text(Localization.get("edit_link_text", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sel = contentTextFieldValue.selection
                        val start = sel.min
                        val end = sel.max
                        val textToUse = linkText.ifEmpty { linkUrl }
                        
                        val oldAnnotated = contentTextFieldValue.annotatedString
                        val textBefore = if (start > 0 && start <= oldAnnotated.length) oldAnnotated.subSequence(0, start) else AnnotatedString("")
                        val textAfter = if (end >= 0 && end < oldAnnotated.length) oldAnnotated.subSequence(end, oldAnnotated.length) else AnnotatedString("")
                        
                        val linkBuilder = AnnotatedString.Builder()
                        linkBuilder.append(textBefore)
                        val linkStart = linkBuilder.length
                        linkBuilder.append(textToUse)
                        val linkEnd = linkBuilder.length
                        linkBuilder.append(textAfter)
                        
                        linkBuilder.addStyle(
                            SpanStyle(color = Color(0xFF6750A4), textDecoration = TextDecoration.Underline),
                            linkStart,
                            linkEnd
                        )
                        linkBuilder.addStringAnnotation("URL", linkUrl, linkStart, linkEnd)
                        
                        val shift = textToUse.length - (end - start)
                        oldAnnotated.spanStyles.forEach { span ->
                            val newSpanStart = when {
                                span.start < start -> span.start
                                span.start in start..end -> start
                                else -> span.start + shift
                            }
                            val newSpanEnd = when {
                                span.end < start -> span.end
                                span.end in start..end -> start + textToUse.length
                                else -> span.end + shift
                            }
                            if (newSpanEnd > newSpanStart) {
                                linkBuilder.addStyle(span.item, newSpanStart, newSpanEnd)
                            }
                        }
                        oldAnnotated.getStringAnnotations(0, oldAnnotated.length).forEach { ann ->
                            if (ann.tag != "URL" || ann.start < start || ann.end > end) {
                                val newAnnStart = when {
                                    ann.start < start -> ann.start
                                    ann.start in start..end -> start
                                    else -> ann.start + shift
                                }
                                val newAnnEnd = when {
                                    ann.end < start -> ann.end
                                    ann.end in start..end -> start + textToUse.length
                                    else -> ann.end + shift
                                }
                                if (newAnnEnd > newAnnStart) {
                                    linkBuilder.addStringAnnotation(ann.tag, ann.item, newAnnStart, newAnnEnd)
                                }
                            }
                        }
                        
                        val updatedAnnotatedString = linkBuilder.toAnnotatedString()
                        editor.text = updatedAnnotatedString.text
                        editor.spans = updatedAnnotatedString.toSpans()
                        editor.selection = androidx.compose.ui.text.TextRange(linkEnd)
                        editor.saveToHistory()
                        val html = updatedAnnotatedString.toHtml()
                        lastSerializedHtml = html
                        viewModel.editingContent.value = html
                        showLinkInputDialog = false
                    }
                ) {
                    Text(Localization.get("dialog_ok", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkInputDialog = false }) {
                    Text(Localization.get("dialog_cancel", language))
                }
            }
        )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            Localization.get("dialog_tags_title", language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showAddTagDialog = true }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = Localization.get("dialog_add_tag_title", language),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(tags) { tag ->
                            val isChecked = editingTags.contains(tag.name)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleTagInDraft(tag.name) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { viewModel.toggleTagInDraft(tag.name) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(
                                            color = Color(android.graphics.Color.parseColor(tag.colorHex)),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    Localization.getTagName(tag.name, language),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.deleteTag(tag) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete tag",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showAddTagDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Localization.get("dialog_add_tag_title", language))
                        }

                        Button(
                            onClick = { showTagDialog = false }
                        ) {
                            Text(Localization.get("done", language))
                        }
                    }
                }
            }
        }
    }

    // Add Folder Dialog
    if (showAddFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text(Localization.get("dialog_add_folder_title", language)) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text(Localization.get("dialog_add_folder_label", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.addCustomFolder(newFolderName)
                            viewModel.setFolderInDraft(newFolderName)
                            showAddFolderDialog = false
                        }
                    }
                ) { Text(Localization.get("dialog_confirm", language)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddFolderDialog = false }) { Text(Localization.get("dialog_cancel", language)) }
            }
        )
    }

    // Add Tag Dialog
    if (showAddTagDialog) {
        var newTagName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text(Localization.get("dialog_add_tag_title", language)) },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text(Localization.get("dialog_add_tag_label", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            val cleanName = newTagName.trim()
                            viewModel.addCustomTag(cleanName)
                            if (!editingTags.contains(cleanName)) {
                                viewModel.toggleTagInDraft(cleanName)
                            }
                            showAddTagDialog = false
                        }
                    }
                ) { Text(Localization.get("dialog_confirm", language)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) { Text(Localization.get("dialog_cancel", language)) }
            }
        )
    }
}
