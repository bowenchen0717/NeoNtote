package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("zennote_prefs", android.content.Context.MODE_PRIVATE)

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = NoteRepository(database.noteDao())

    // UI filters
    val searchQuery = MutableStateFlow("")
    val selectedFolder = MutableStateFlow("All")
    val selectedTag = MutableStateFlow<String?>(null)

    // Order states
    val folderOrder = MutableStateFlow<List<String>>(
        prefs.getString("folder_order", null)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    )
    val tagOrder = MutableStateFlow<List<String>>(
        prefs.getString("tag_order", null)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    )

    fun updateFolderOrder(newOrder: List<String>) {
        folderOrder.value = newOrder
        prefs.edit().putString("folder_order", newOrder.joinToString(",")).apply()
    }

    fun updateTagOrder(newOrder: List<String>) {
        tagOrder.value = newOrder
        prefs.edit().putString("tag_order", newOrder.joinToString(",")).apply()
    }

    // Data sources
    val allNotes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<Folder>> = combine(repository.allFolders, folderOrder) { folders, order ->
        if (order.isEmpty()) {
            folders
        } else {
            val orderMap = order.withIndex().associate { it.value to it.index }
            folders.sortedBy { orderMap[it.name] ?: Int.MAX_VALUE }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<Tag>> = combine(repository.allTags, tagOrder) { tags, order ->
        if (order.isEmpty()) {
            tags
        } else {
            val orderMap = order.withIndex().associate { it.value to it.index }
            tags.sortedBy { orderMap[it.name] ?: Int.MAX_VALUE }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getNoteByIdFlow(id: Int): Flow<Note?> = repository.getNoteByIdFlow(id)

    // Combined filtered notes list
    val filteredNotes: StateFlow<List<Note>> = combine(
        allNotes,
        searchQuery,
        selectedFolder,
        selectedTag
    ) { notes, query, folder, tag ->
        notes.filter { note ->
            val matchesFolder = folder == "All" || note.folder.equals(folder, ignoreCase = true)
            val matchesTag = tag == null || note.tags.split(",").map { it.trim() }.contains(tag)
            val matchesQuery = query.isEmpty() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)

            matchesFolder && matchesTag && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Editing States
    val currentEditingNoteId = MutableStateFlow<Int?>(null)
    val editingTitle = MutableStateFlow("")
    val editingContent = MutableStateFlow("")
    val editingFolder = MutableStateFlow("All")
    val editingTags = MutableStateFlow<List<String>>(emptyList())
    val editingChecklist = MutableStateFlow<List<ChecklistItem>>(emptyList())
    val editingImageUrl = MutableStateFlow<String?>(null)

    // Settings & Sync State

    val themeMode = MutableStateFlow(prefs.getString("theme_mode", "dark") ?: "dark")
    val language = MutableStateFlow(prefs.getString("language", "zh") ?: "zh")
    val isSyncing = MutableStateFlow(false)
    val lastSyncedTime = MutableStateFlow(prefs.getString("last_synced_time", "Not synced yet") ?: "Not synced yet")
    val googleAccessToken = MutableStateFlow(prefs.getString("google_access_token", null))
    val showGoogleLogin = MutableStateFlow(false)

    val showFolderOption = MutableStateFlow(prefs.getBoolean("show_folder_option", true))
    val showTagOption = MutableStateFlow(prefs.getBoolean("show_tag_option", true))
    val showChecklistOption = MutableStateFlow(prefs.getBoolean("show_checklist_option", true))

    init {
        viewModelScope.launch {
            themeMode.collect { mode ->
                prefs.edit().putString("theme_mode", mode).apply()
            }
        }
        viewModelScope.launch {
            language.collect { lang ->
                prefs.edit().putString("language", lang).apply()
            }
        }
        viewModelScope.launch {
            showFolderOption.collect { show ->
                prefs.edit().putBoolean("show_folder_option", show).apply()
            }
        }
        viewModelScope.launch {
            showTagOption.collect { show ->
                prefs.edit().putBoolean("show_tag_option", show).apply()
            }
        }
        viewModelScope.launch {
            showChecklistOption.collect { show ->
                prefs.edit().putBoolean("show_checklist_option", show).apply()
            }
        }
    }

    // Dynamic Tags/Folders during creation
    val tempNewTagName = MutableStateFlow("")
    val tempNewFolderName = MutableStateFlow("")

    fun selectFolder(folder: String) {
        selectedFolder.value = folder
        selectedTag.value = null // Clear tag filter when switching folder
    }

    fun selectTag(tag: String?) {
        selectedTag.value = tag
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned, timestamp = System.currentTimeMillis()))
        }
    }

    fun toggleChecklistItem(note: Note, index: Int) {
        viewModelScope.launch {
            val items = ChecklistSerializer.fromJson(note.checklistJson).toMutableList()
            if (index in items.indices) {
                items[index] = items[index].copy(checked = !items[index].checked)
                val updatedJson = ChecklistSerializer.toJson(items)
                repository.updateNote(note.copy(checklistJson = updatedJson, timestamp = System.currentTimeMillis()))
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // Set up states when entering Create/Edit Mode
    fun startEditing(noteId: Int?) {
        if (noteId == null) {
            // New Note
            currentEditingNoteId.value = null
            editingTitle.value = ""
            editingContent.value = ""
            editingFolder.value = "All"
            editingTags.value = emptyList()
            editingChecklist.value = emptyList()
            editingImageUrl.value = null
        } else {
            // Existing Note
            viewModelScope.launch {
                val note = repository.getNoteById(noteId)
                if (note != null) {
                    currentEditingNoteId.value = note.id
                    editingTitle.value = note.title
                    editingContent.value = note.content
                    editingFolder.value = note.folder
                    editingTags.value = if (note.tags.isEmpty()) emptyList() else note.tags.split(",").map { it.trim() }
                    editingChecklist.value = ChecklistSerializer.fromJson(note.checklistJson)
                    editingImageUrl.value = note.imageUrl
                }
            }
        }
    }

    fun saveCurrentNote(onComplete: () -> Unit) {
        viewModelScope.launch {
            val title = editingTitle.value.ifEmpty { "Untitled Note" }
            val content = editingContent.value
            val folder = editingFolder.value
            val tagsString = editingTags.value.joinToString(",")
            val checklistJsonString = ChecklistSerializer.toJson(editingChecklist.value)
            val imageUrl = editingImageUrl.value

            val noteId = currentEditingNoteId.value
            if (noteId == null) {
                // Insert
                val newNote = Note(
                    title = title,
                    content = content,
                    folder = folder,
                    tags = tagsString,
                    checklistJson = checklistJsonString,
                    imageUrl = imageUrl,
                    timestamp = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                repository.insertNote(newNote)
            } else {
                // Update
                val existing = repository.getNoteById(noteId)
                if (existing != null) {
                    val updatedNote = existing.copy(
                        title = title,
                        content = content,
                        folder = folder,
                        tags = tagsString,
                        checklistJson = checklistJsonString,
                        imageUrl = imageUrl,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.updateNote(updatedNote)
                }
            }
            onComplete()
        }
    }

    // Editing checklists helper
    fun addChecklistItemToDraft(text: String) {
        if (text.isBlank()) return
        val current = editingChecklist.value.toMutableList()
        current.add(ChecklistItem(text, false))
        editingChecklist.value = current
    }

    fun removeChecklistItemFromDraft(index: Int) {
        val current = editingChecklist.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            editingChecklist.value = current
        }
    }

    fun toggleChecklistItemInDraft(index: Int) {
        val current = editingChecklist.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(checked = !current[index].checked)
            editingChecklist.value = current
        }
    }

    fun toggleTagInDraft(tagName: String) {
        val current = editingTags.value.toMutableList()
        if (current.contains(tagName)) {
            current.remove(tagName)
        } else {
            current.add(tagName)
        }
        editingTags.value = current
    }

    fun setFolderInDraft(folderName: String) {
        editingFolder.value = folderName
    }

    fun addCustomTag(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val cleanName = name.trim()
            repository.insertTag(Tag(cleanName, getRandomColorHex()))
            tempNewTagName.value = ""
        }
    }

    fun addCustomFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val cleanName = name.trim()
            repository.insertFolder(Folder(cleanName))
            tempNewFolderName.value = ""
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            if (folder.name != "All") {
                repository.deleteFolder(folder)
                if (selectedFolder.value == folder.name) {
                    selectedFolder.value = "All"
                }
            }
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            repository.deleteTag(tag)
            if (selectedTag.value == tag.name) {
                selectedTag.value = null
            }
        }
    }

    // Google OAuth & Sync Helpers
    fun saveAccessToken(token: String) {
        googleAccessToken.value = token
        prefs.edit().putString("google_access_token", token).apply()
    }

    fun clearAccessToken() {
        googleAccessToken.value = null
        prefs.edit().remove("google_access_token").apply()
        lastSyncedTime.value = "Not synced yet"
        prefs.edit().remove("last_synced_time").apply()
    }

    private fun parseIso8601(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(dateStr)?.time ?: 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }

    private fun serializeBackupData(notes: List<Note>, folders: List<Folder>, tags: List<Tag>): String {
        val root = JSONObject()
        val notesArray = JSONArray()
        for (note in notes) {
            val noteObj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("timestamp", note.timestamp)
                put("createdAt", note.createdAt)
                put("folder", note.folder)
                put("tags", note.tags)
                put("isPinned", note.isPinned)
                put("isCompleted", note.isCompleted)
                put("imageUrl", note.imageUrl ?: JSONObject.NULL)
                put("checklistJson", note.checklistJson)
            }
            notesArray.put(noteObj)
        }
        root.put("notes", notesArray)

        val foldersArray = JSONArray()
        for (folder in folders) {
            val folderObj = JSONObject().apply {
                put("name", folder.name)
                put("createdAt", folder.createdAt)
            }
            foldersArray.put(folderObj)
        }
        root.put("folders", foldersArray)

        val tagsArray = JSONArray()
        for (tag in tags) {
            val tagObj = JSONObject().apply {
                put("name", tag.name)
                put("colorHex", tag.colorHex)
            }
            tagsArray.put(tagObj)
        }
        root.put("tags", tagsArray)

        return root.toString()
    }

    private fun parseBackupData(jsonStr: String): Triple<List<Note>, List<Folder>, List<Tag>> {
        val notes = mutableListOf<Note>()
        val folders = mutableListOf<Folder>()
        val tags = mutableListOf<Tag>()
        try {
            val root = JSONObject(jsonStr)
            if (root.has("notes")) {
                val notesArray = root.getJSONArray("notes")
                for (i in 0 until notesArray.length()) {
                    val noteObj = notesArray.getJSONObject(i)
                    val note = Note(
                        id = 0,
                        title = noteObj.optString("title", ""),
                        content = noteObj.optString("content", ""),
                        timestamp = noteObj.optLong("timestamp", System.currentTimeMillis()),
                        createdAt = noteObj.optLong("createdAt", System.currentTimeMillis()),
                        folder = noteObj.optString("folder", "All"),
                        tags = noteObj.optString("tags", ""),
                        isPinned = noteObj.optBoolean("isPinned", false),
                        isCompleted = noteObj.optBoolean("isCompleted", false),
                        imageUrl = if (noteObj.isNull("imageUrl")) null else noteObj.optString("imageUrl").takeIf { it.isNotEmpty() },
                        checklistJson = noteObj.optString("checklistJson", "")
                    )
                    notes.add(note)
                }
            }
            if (root.has("folders")) {
                val foldersArray = root.getJSONArray("folders")
                for (i in 0 until foldersArray.length()) {
                    val folderObj = foldersArray.getJSONObject(i)
                    folders.add(Folder(
                        name = folderObj.optString("name", ""),
                        createdAt = folderObj.optLong("createdAt", System.currentTimeMillis())
                    ))
                }
            }
            if (root.has("tags")) {
                val tagsArray = root.getJSONArray("tags")
                for (i in 0 until tagsArray.length()) {
                    val tagObj = tagsArray.getJSONObject(i)
                    tags.add(Tag(
                        name = tagObj.optString("name", ""),
                        colorHex = tagObj.optString("colorHex", "#6750A4")
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Triple(notes, folders, tags)
    }

    private suspend fun performRealSync(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

            // 1. Search for existing zennote_backup.json
            val searchUrl = "https://www.googleapis.com/drive/v3/files?q=name='zennote_backup.json'+and+trashed=false&fields=files(id,name,modifiedTime)"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("Authorization", "Bearer $token")
                .build()

            var fileId: String? = null
            var remoteModifiedTime: Long = 0L

            client.newCall(searchRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 401) {
                        clearAccessToken()
                    }
                    return@withContext false
                }
                val bodyStr = response.body?.string() ?: return@withContext false
                val filesArray = JSONObject(bodyStr).getJSONArray("files")
                if (filesArray.length() > 0) {
                    val fileObj = filesArray.getJSONObject(0)
                    fileId = fileObj.getString("id")
                    val modTimeStr = fileObj.getString("modifiedTime")
                    remoteModifiedTime = parseIso8601(modTimeStr)
                }
            }

            // 2. Fetch local data using first() on flows
            val localNotes = repository.allNotes.first()
            val localFolders = repository.allFolders.first()
            val localTags = repository.allTags.first()

            var remoteNotes = emptyList<Note>()
            var remoteFolders = emptyList<Folder>()
            var remoteTags = emptyList<Tag>()

            // 3. Download existing backup from Google Drive if exists
            if (fileId != null) {
                val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
                val downloadRequest = Request.Builder()
                    .url(downloadUrl)
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(downloadRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string()
                        if (!bodyStr.isNullOrEmpty()) {
                            val parsed = parseBackupData(bodyStr)
                            remoteNotes = parsed.first
                            remoteFolders = parsed.second
                            remoteTags = parsed.third
                        }
                    }
                }
            }

            // 4. Merge Folders: Insert any missing folders
            for (folder in remoteFolders) {
                val exists = localFolders.any { it.name.equals(folder.name, ignoreCase = true) }
                if (!exists && folder.name.isNotBlank()) {
                    repository.insertFolder(folder)
                }
            }

            // 5. Merge Tags: Insert any missing tags
            for (tag in remoteTags) {
                val exists = localTags.any { it.name.equals(tag.name, ignoreCase = true) }
                if (!exists && tag.name.isNotBlank()) {
                    repository.insertTag(tag)
                }
            }

            // 6. Merge Notes:
            for (remoteNote in remoteNotes) {
                val matchedLocalNote = localNotes.find { it.createdAt == remoteNote.createdAt }
                if (matchedLocalNote != null) {
                    if (remoteNote.timestamp > matchedLocalNote.timestamp) {
                        val updatedNote = matchedLocalNote.copy(
                            title = remoteNote.title,
                            content = remoteNote.content,
                            folder = remoteNote.folder,
                            tags = remoteNote.tags,
                            isPinned = remoteNote.isPinned,
                            isCompleted = remoteNote.isCompleted,
                            imageUrl = remoteNote.imageUrl,
                            checklistJson = remoteNote.checklistJson,
                            timestamp = remoteNote.timestamp
                        )
                        repository.updateNote(updatedNote)
                    }
                } else {
                    repository.insertNote(remoteNote)
                }
            }

            // 7. Fetch final updated local data for backup
            val finalNotes = repository.allNotes.first()
            val finalFolders = repository.allFolders.first()
            val finalTags = repository.allTags.first()

            val finalJsonData = serializeBackupData(finalNotes, finalFolders, finalTags)

            // 8. Upload merged data back to Google Drive
            if (fileId == null) {
                val createUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                val metadataJson = JSONObject().apply {
                    put("name", "zennote_backup.json")
                    put("mimeType", "application/json")
                }.toString()

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addPart(
                        Headers.Builder().add("Content-Type", "application/json; charset=UTF-8").build(),
                        metadataJson.toRequestBody("application/json; charset=utf-8".toMediaType())
                    )
                    .addPart(
                        Headers.Builder().add("Content-Type", "application/json").build(),
                        finalJsonData.toRequestBody("application/json; charset=utf-8".toMediaType())
                    )
                    .build()

                val createRequest = Request.Builder()
                    .url(createUrl)
                    .header("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                client.newCall(createRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext false
                    }
                }
            } else {
                val updateUrl = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
                val updateRequest = Request.Builder()
                    .url(updateUrl)
                    .header("Authorization", "Bearer $token")
                    .patch(finalJsonData.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                client.newCall(updateRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext false
                    }
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    fun triggerSync() {
        val token = googleAccessToken.value
        if (token.isNullOrEmpty()) {
            showGoogleLogin.value = true
            return
        }

        viewModelScope.launch {
            isSyncing.value = true
            val success = performRealSync(token)
            isSyncing.value = false
            
            if (success) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val syncTime = "Synced: " + sdf.format(Date())
                lastSyncedTime.value = syncTime
                prefs.edit().putString("last_synced_time", syncTime).apply()
                Toast.makeText(getApplication(), "同步成功 / Sync Completed!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), "同步失敗，請重新登入 / Sync Failed, Please log in again.", Toast.LENGTH_LONG).show()
                showGoogleLogin.value = true
            }
        }
    }

    fun attachRandomImage() {
        val urls = listOf(
            "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?q=80&w=800&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?q=80&w=800&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1472214222541-d510753a49fa?q=80&w=800&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1518495973542-4542c06a5843?q=80&w=800&auto=format&fit=crop"
        )
        editingImageUrl.value = urls.random()
    }

    fun clearAttachedImage() {
        editingImageUrl.value = null
    }

    private fun getRandomColorHex(): String {
        val colors = listOf("#6750A4", "#03A9F4", "#E91E63", "#4CAF50", "#FF9800", "#9C27B0", "#00BCD4")
        return colors.random()
    }
}
