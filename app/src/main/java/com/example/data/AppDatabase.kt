package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Note::class, Folder::class, Tag::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zennote_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.noteDao())
                }
            }
        }

        private suspend fun populateDatabase(noteDao: NoteDao) {
            // Default folders
            noteDao.insertFolder(Folder("All"))
            noteDao.insertFolder(Folder("Work"))
            noteDao.insertFolder(Folder("Personal"))
            noteDao.insertFolder(Folder("Ideas"))
            noteDao.insertFolder(Folder("Inspiration"))

            // Default tags
            noteDao.insertTag(Tag("Work", "#6750A4"))
            noteDao.insertTag(Tag("Design", "#03A9F4"))
            noteDao.insertTag(Tag("Personal", "#E91E63"))
            noteDao.insertTag(Tag("Inspiration", "#4CAF50"))

            // Sample notes
            noteDao.insertNote(
                Note(
                    title = "Meeting Notes",
                    content = "This meeting note outlines our key product goals, action items, and design iterations. It utilizes a clean dark layout with rich images and high-fidelity Material You aesthetic pairings.\n\nOur immediate goal is to establish a robust, offline-first notes system that users can enjoy with absolute fluid motion and zero lag.",
                    folder = "Work",
                    tags = "Work,Design",
                    isPinned = true,
                    imageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=800&auto=format&fit=crop",
                    checklistJson = "[{\"text\":\"Define product requirements doc\",\"checked\":true},{\"text\":\"Confirm color palette (Zen Slate theme)\",\"checked\":true},{\"text\":\"Build interactive Room persistence database\",\"checked\":false}]"
                )
            )

            noteDao.insertNote(
                Note(
                    title = "Product Design Trends",
                    content = "Exploring Material Design 3 guidelines and responsive web/mobile paradigms:\n\n1. Adaptive Spacing & Layout: Generous negative margins, clean typographic paired hierarchies.\n2. Atmospheric Accents: Gentle blue/indigo gradients, subtle card borders, and rounded floating action targets.\n3. Content over complexity: Keeping design pristine, avoiding standard AI slop defaults.",
                    folder = "Inspiration",
                    tags = "Design,Inspiration",
                    isPinned = true,
                    imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=800&auto=format&fit=crop",
                    checklistJson = "[{\"text\":\"Research Material You schemes\",\"checked\":true},{\"text\":\"Prototype side-panel transitions for tablet screens\",\"checked\":false}]"
                )
            )

            noteDao.insertNote(
                Note(
                    title = "Weekly Personal Goals",
                    content = "A clean todo note list to keep track of everyday errands, habits, and quick thoughts.",
                    folder = "Personal",
                    tags = "Personal",
                    isPinned = false,
                    checklistJson = "[{\"text\":\"Morning jog at the park (5K)\",\"checked\":true},{\"text\":\"Pick up sourdough bread & organic apples\",\"checked\":true},{\"text\":\"Read two chapters of 'Flow' by Mihaly Csikszentmihalyi\",\"checked\":false}]"
                )
            )
        }
    }
}
