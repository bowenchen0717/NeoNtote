package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RestoreResult(
    val success: Boolean,
    val notesCount: Int = 0,
    val foldersCount: Int = 0,
    val tagsCount: Int = 0,
    val message: String = ""
)

object BackupManager {

    private fun String.toSqlLiteral(): String {
        return "'" + this.replace("'", "''") + "'"
    }

    fun generateBackupSqlContent(
        context: Context,
        notes: List<Note>,
        folders: List<Folder>,
        tags: List<Tag>
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.appendLine("-- ========================================================")
        sb.appendLine("-- ZenNote (唯一筆記) SQL Backup File")
        sb.appendLine("-- Export Date: $dateStr")
        sb.appendLine("-- Total Notes: ${notes.size}, Folders: ${folders.size}, Tags: ${tags.size}")
        sb.appendLine("-- ========================================================")
        sb.appendLine()

        // Folders SQL
        sb.appendLine("-- Table: folders")
        folders.forEach { f ->
            sb.appendLine("INSERT OR REPLACE INTO folders (name, createdAt) VALUES (${f.name.toSqlLiteral()}, ${f.createdAt});")
        }
        sb.appendLine()

        // Tags SQL
        sb.appendLine("-- Table: tags")
        tags.forEach { t ->
            sb.appendLine("INSERT OR REPLACE INTO tags (name, colorHex) VALUES (${t.name.toSqlLiteral()}, ${t.colorHex.toSqlLiteral()});")
        }
        sb.appendLine()

        // Notes SQL
        sb.appendLine("-- Table: notes")
        notes.forEach { n ->
            val exportImgUrl = ImageBackupUtils.processImageUrlForExport(context, n.imageUrl)
            val img = (exportImgUrl ?: "").toSqlLiteral()
            val chk = n.checklistJson.toSqlLiteral()
            val isPin = if (n.isPinned) 1 else 0
            val isComp = if (n.isCompleted) 1 else 0

            sb.appendLine(
                "INSERT OR REPLACE INTO notes (id, title, content, timestamp, createdAt, folder, tags, isPinned, isCompleted, imageUrl, checklistJson) " +
                "VALUES (${n.id}, ${n.title.toSqlLiteral()}, ${n.content.toSqlLiteral()}, ${n.timestamp}, ${n.createdAt}, ${n.folder.toSqlLiteral()}, ${n.tags.toSqlLiteral()}, $isPin, $isComp, $img, $chk);"
            )
        }
        sb.appendLine()

        // Embedded JSON structure for loss-less parsing
        val jsonObj = JSONObject().apply {
            put("app", "ZenNote")
            put("version", 1)
            put("timestamp", System.currentTimeMillis())

            val foldersArr = JSONArray()
            folders.forEach { f ->
                foldersArr.put(JSONObject().apply {
                    put("name", f.name)
                    put("createdAt", f.createdAt)
                })
            }
            put("folders", foldersArr)

            val tagsArr = JSONArray()
            tags.forEach { t ->
                tagsArr.put(JSONObject().apply {
                    put("name", t.name)
                    put("colorHex", t.colorHex)
                })
            }
            put("tags", tagsArr)

            val notesArr = JSONArray()
            notes.forEach { n ->
                val exportImgUrl = ImageBackupUtils.processImageUrlForExport(context, n.imageUrl)
                notesArr.put(JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("content", n.content)
                    put("timestamp", n.timestamp)
                    put("createdAt", n.createdAt)
                    put("folder", n.folder)
                    put("tags", n.tags)
                    put("isPinned", n.isPinned)
                    put("isCompleted", n.isCompleted)
                    put("imageUrl", exportImgUrl ?: "")
                    put("checklistJson", n.checklistJson)
                })
            }
            put("notes", notesArr)
        }

        sb.appendLine("-- ZENNOTE_JSON_DATA_START")
        sb.appendLine(jsonObj.toString(2))
        sb.appendLine("-- ZENNOTE_JSON_DATA_END")

        return sb.toString()
    }

    fun exportToPublicDownloads(context: Context, backupContent: String): String? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "zennote_backup_$timeStamp.sql"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/sql")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(backupContent.toByteArray(Charsets.UTF_8))
                    }
                    "Downloads/$fileName"
                } else null
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                file.writeText(backupContent, Charsets.UTF_8)
                "Downloads/$fileName"
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Failed to save to downloads", e)
            null
        }
    }

    fun writeToUri(context: Context, uri: Uri, backupContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(backupContent.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "Failed to write URI", e)
            false
        }
    }

    suspend fun restoreFromBackupContent(
        context: Context,
        repository: NoteRepository,
        content: String
    ): RestoreResult {
        return try {
            var restoredNotes = 0
            var restoredFolders = 0
            var restoredTags = 0

            val cleanContent = content.removePrefix("\uFEFF").trim()

            // 1. Try parsing JSON block or raw JSON
            val jsonString = if (cleanContent.contains("-- ZENNOTE_JSON_DATA_START")) {
                val start = cleanContent.indexOf("-- ZENNOTE_JSON_DATA_START") + "-- ZENNOTE_JSON_DATA_START".length
                val end = cleanContent.indexOf("-- ZENNOTE_JSON_DATA_END", start)
                if (start in 0 until end && end > start) {
                    cleanContent.substring(start, end).trim()
                } else null
            } else if (cleanContent.startsWith("{")) {
                cleanContent
            } else null

            if (jsonString != null) {
                try {
                    val json = JSONObject(jsonString)

                    if (json.has("folders")) {
                        val foldersArr = json.getJSONArray("folders")
                        for (i in 0 until foldersArr.length()) {
                            val item = foldersArr.getJSONObject(i)
                            val name = item.getString("name")
                            val createdAt = item.optLong("createdAt", System.currentTimeMillis())
                            repository.insertFolder(Folder(name = name, createdAt = createdAt))
                            restoredFolders++
                        }
                    }

                    if (json.has("tags")) {
                        val tagsArr = json.getJSONArray("tags")
                        for (i in 0 until tagsArr.length()) {
                            val item = tagsArr.getJSONObject(i)
                            val name = item.getString("name")
                            val colorHex = item.optString("colorHex", "#6750A4")
                            repository.insertTag(Tag(name = name, colorHex = colorHex))
                            restoredTags++
                        }
                    }

                    if (json.has("notes")) {
                        val notesArr = json.getJSONArray("notes")
                        for (i in 0 until notesArr.length()) {
                            val item = notesArr.getJSONObject(i)
                            val rawImgUrl = item.optString("imageUrl", null).takeIf { !it.isNull_or_blank() }
                            val restoredImgUrl = ImageBackupUtils.processImageUrlForImport(context, rawImgUrl)

                            val note = Note(
                                id = item.optInt("id", 0),
                                title = item.optString("title", ""),
                                content = item.optString("content", ""),
                                timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                                folder = item.optString("folder", "All"),
                                tags = item.optString("tags", ""),
                                isPinned = item.optBoolean("isPinned", false),
                                isCompleted = item.optBoolean("isCompleted", false),
                                imageUrl = restoredImgUrl,
                                checklistJson = item.optString("checklistJson", "")
                            )
                            repository.insertNote(note)
                            restoredNotes++
                        }
                    }

                    if (restoredNotes > 0 || restoredFolders > 0 || restoredTags > 0) {
                        return RestoreResult(
                            success = true,
                            notesCount = restoredNotes,
                            foldersCount = restoredFolders,
                            tagsCount = restoredTags,
                            message = "成功還原 $restoredNotes 則筆記、$restoredFolders 個資料夾與 $restoredTags 個標籤！"
                        )
                    }
                } catch (e: Exception) {
                    Log.w("BackupManager", "JSON restore failed, falling back to SQL parser", e)
                }
            }

            // 2. Fallback to parsing SQL INSERT statements line by line
            val lines = cleanContent.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("INSERT", ignoreCase = true)) {
                    val valuesStart = trimmed.indexOf("VALUES", ignoreCase = true)
                    if (valuesStart != -1) {
                        val valuesTuple = trimmed.substring(valuesStart + 6).trim()
                        val parts = parseSqlValuesTuple(valuesTuple)

                        if (trimmed.contains("INTO folders", ignoreCase = true) && parts.size >= 2) {
                            val name = parts[0]
                            val createdAt = parts[1].toLongOrNull() ?: System.currentTimeMillis()
                            repository.insertFolder(Folder(name = name, createdAt = createdAt))
                            restoredFolders++
                        } else if (trimmed.contains("INTO tags", ignoreCase = true) && parts.size >= 2) {
                            val name = parts[0]
                            val colorHex = parts[1]
                            repository.insertTag(Tag(name = name, colorHex = colorHex))
                            restoredTags++
                        } else if (trimmed.contains("INTO notes", ignoreCase = true) && parts.size >= 11) {
                            val rawImgUrl = parts[9].takeIf { !it.isNull_or_blank() }
                            val restoredImgUrl = ImageBackupUtils.processImageUrlForImport(context, rawImgUrl)

                            val note = Note(
                                id = parts[0].toIntOrNull() ?: 0,
                                title = parts[1],
                                content = parts[2],
                                timestamp = parts[3].toLongOrNull() ?: System.currentTimeMillis(),
                                createdAt = parts[4].toLongOrNull() ?: System.currentTimeMillis(),
                                folder = parts[5],
                                tags = parts[6],
                                isPinned = parts[7] == "1" || parts[7].equals("true", true),
                                isCompleted = parts[8] == "1" || parts[8].equals("true", true),
                                imageUrl = restoredImgUrl,
                                checklistJson = parts[10]
                            )
                            repository.insertNote(note)
                            restoredNotes++
                        }
                    }
                }
            }

            if (restoredNotes > 0 || restoredFolders > 0 || restoredTags > 0) {
                RestoreResult(
                    success = true,
                    notesCount = restoredNotes,
                    foldersCount = restoredFolders,
                    tagsCount = restoredTags,
                    message = "成功還原 $restoredNotes 則筆記、$restoredFolders 個資料夾與 $restoredTags 個標籤！"
                )
            } else {
                RestoreResult(
                    success = false,
                    message = "還原失敗：未找到任何可讀取的筆記資料"
                )
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Restore failed", e)
            RestoreResult(
                success = false,
                message = "還原失敗：${e.localizedMessage ?: "檔案格式不符"}"
            )
        }
    }

    private fun parseSqlValuesTuple(tupleStr: String): List<String> {
        val result = mutableListOf<String>()
        val s = tupleStr.trim().removePrefix("(").removeSuffix(")").removeSuffix(";").trim()
        var i = 0
        val sb = StringBuilder()
        var inQuotes = false

        while (i < s.length) {
            val c = s[i]
            if (inQuotes) {
                if (c == '\'') {
                    if (i + 1 < s.length && s[i + 1] == '\'') {
                        sb.append('\'')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = false
                    }
                } else {
                    sb.append(c)
                }
            } else {
                if (c == '\'') {
                    inQuotes = true
                } else if (c == ',') {
                    result.add(sb.toString().trim())
                    sb.clear()
                } else {
                    sb.append(c)
                }
            }
            i++
        }
        if (sb.isNotEmpty() || result.isNotEmpty()) {
            result.add(sb.toString().trim())
        }
        return result
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty() || this == "null"
    }
}
