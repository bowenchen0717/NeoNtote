package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = NoteRepository(database.noteDao())

    // UI filters
    val searchQuery = MutableStateFlow("")
    val selectedFolder = MutableStateFlow("All")
    val selectedTag = MutableStateFlow<String?>(null)

    // Data sources
    val allNotes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<Folder>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<Tag>> = repository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    val themeMode = MutableStateFlow("dark") // "dark", "light", "system"
    val language = MutableStateFlow("zh") // "zh" for Traditional Chinese, "en" for English
    val isSyncing = MutableStateFlow(false)
    val lastSyncedTime = MutableStateFlow("Not synced yet")

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

    // Interactive simulated features
    fun triggerSync() {
        viewModelScope.launch {
            isSyncing.value = true
            kotlinx.coroutines.delay(2000) // Simulated sync wait
            isSyncing.value = false
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            lastSyncedTime.value = "Synced: " + sdf.format(Date())
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
