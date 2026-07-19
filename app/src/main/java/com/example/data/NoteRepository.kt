package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    val allFolders: Flow<List<Folder>> = noteDao.getAllFolders()
    val allTags: Flow<List<Tag>> = noteDao.getAllTags()

    fun getNoteByIdFlow(id: Int): Flow<Note?> = noteDao.getNoteByIdFlow(id)

    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun deleteNoteById(id: Int) = noteDao.deleteNoteById(id)

    fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes("%$query%")
    }

    suspend fun insertFolder(folder: Folder) = noteDao.insertFolder(folder)

    suspend fun deleteFolder(folder: Folder) = noteDao.deleteFolder(folder)

    suspend fun insertTag(tag: Tag) = noteDao.insertTag(tag)

    suspend fun deleteTag(tag: Tag) = noteDao.deleteTag(tag)
}
