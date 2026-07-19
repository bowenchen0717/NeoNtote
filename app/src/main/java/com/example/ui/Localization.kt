package com.example.ui

object Localization {
    private val translations = mapOf(
        "app_name" to Pair("ZenNote", "ZenNote"),
        "splash_subtitle" to Pair("優雅 · 高效 · 專注", "Elegant · Fast · Mindful"),
        "drawer_title" to Pair("ZenNote 專業版", "ZenNote Pro"),
        "drawer_sync_enabled" to Pair("雲端備份同步已啟用", "Cloud Sync Enabled"),
        "drawer_folders" to Pair("我的資料夾", "My Folders"),
        "drawer_tags" to Pair("我的標籤", "My Tags"),
        "drawer_settings" to Pair("App 設定與備份", "App Settings & Backup"),
        "main_title" to Pair("我的筆記", "My Notes"),
        "main_filter" to Pair("篩選: ", "Filter: "),
        "main_clear_filter" to Pair("清除篩選", "Clear Filter"),
        "main_search_placeholder" to Pair("搜尋內容...", "Search notes..."),
        "main_tab_all" to Pair("全部", "All"),
        "main_tab_pinned" to Pair("置頂", "Pinned"),
        "main_tab_todo" to Pair("待辦", "Todo"),
        "main_tab_sync" to Pair("同步", "Sync"),
        "main_empty_title" to Pair("沒有找到任何筆記", "No notes found"),
        "main_empty_subtitle" to Pair("點擊右下角的 + 按鈕，立刻開始記錄生活！", "Click the + button at bottom-right to start capturing your life!"),
        "note_more_tasks" to Pair("+ 還有 %d 項待辦...", "+ %d more tasks..."),
        "note_pinned" to Pair("置頂", "Pinned"),
        "note_delete" to Pair("刪除筆記", "Delete Note"),
        "note_image_desc" to Pair("筆記附加圖片", "Note attached visual"),
        "view_title" to Pair("檢視筆記", "View Note"),
        "view_share" to Pair("分享", "Share"),
        "view_delete" to Pair("刪除", "Delete"),
        "view_edit" to Pair("編輯筆記", "Edit Note"),
        "view_folder" to Pair("資料夾: %s", "Folder: %s"),
        "view_hero_image" to Pair("主視覺圖片", "Hero Image"),
        "view_todo_title" to Pair("待辦清單", "Todo List"),
        "view_link_copied" to Pair("連結已複製，可直接分享！", "Link copied, ready to share!"),
        "edit_create_title" to Pair("建立新筆記", "Create Note"),
        "edit_edit_title" to Pair("編輯筆記", "Edit Note"),
        "edit_save" to Pair("儲存", "Save"),
        "edit_back" to Pair("返回", "Back"),
        "edit_title_placeholder" to Pair("筆記標題...", "Note Title..."),
        "edit_select_folder" to Pair("選擇資料夾: %s", "Select Folder: %s"),
        "edit_add_edit_tags" to Pair("新增/編輯標籤", "Add/Edit Tags"),
        "edit_remove_tag" to Pair("移除標籤", "Remove tag"),
        "edit_draft_image" to Pair("草稿附加圖片", "Draft Attached Image"),
        "edit_delete_image" to Pair("刪除圖片", "Delete image"),
        "edit_todo_section" to Pair("待辦與項目清單", "Todo & Checklists"),
        "edit_add_todo_placeholder" to Pair("新增待辦...", "Add item..."),
        "edit_add_todo_desc" to Pair("新增項目", "Add Item"),
        "edit_remove_todo" to Pair("移除項目", "Remove item"),
        "edit_content_placeholder" to Pair("在這裡輸入筆記內容...", "Type note content here..."),
        "edit_toolbar_title" to Pair("快速編輯工具列", "Formatting Toolbar"),
        "edit_insert_image" to Pair("插入圖片", "Insert Image"),
        "edit_voice_input" to Pair("語音輸入", "Voice Input"),
        "edit_recording" to Pair("正在錄音...", "Recording..."),
        "edit_recording_toast" to Pair("正在錄音語音辨識中... 點擊停止", "Recording speech recognition... click to stop"),
        "edit_voice_success" to Pair("已成功將語音轉為文字！", "Voice converted to text successfully!"),
        "edit_voice_simulation_text" to Pair(" [語音輸入：今天天氣晴朗，適合去戶外走走，放鬆心情。]", " [Voice Input: Today is sunny, perfect for outdoor walking and relaxing.]"),
        "dialog_tags_title" to Pair("選擇適用標籤", "Select Applicable Tags"),
        "dialog_add_folder_title" to Pair("新增資料夾", "Add Folder"),
        "dialog_add_folder_label" to Pair("資料夾名稱", "Folder Name"),
        "dialog_confirm" to Pair("確定", "Confirm"),
        "dialog_cancel" to Pair("取消", "Cancel"),
        "dialog_add_tag_title" to Pair("新增標籤", "Add Tag"),
        "dialog_add_tag_label" to Pair("標籤名稱", "Tag Name"),
        "sync_loading" to Pair("雲端資料庫備份同步中...", "Cloud Database Syncing..."),
        "sync_success" to Pair("資料已備份至雲端", "Data backed up to cloud"),
        "sync_desc" to Pair(
            "我們已為你整合 Google Drive / OneDrive 雲端空間，所有筆記與待辦清單均會自動在背景存檔，確保多裝置資訊即時同步。",
            "We have integrated Google Drive / OneDrive cloud storage for you. All notes and todo lists are automatically backed up in the background to ensure real-time multi-device synchronization."
        ),
        "sync_status_prefix" to Pair("備份同步中...", "Syncing..."),
        "sync_now_btn" to Pair("立即強制備份同步", "Force Sync Now"),
        "sync_default_time" to Pair("尚未同步", "Not synced yet"),
        "settings_title" to Pair("ZenNote 設定選項", "ZenNote Settings"),
        "settings_theme_title" to Pair("App 主題設定", "App Theme"),
        "settings_theme_dark" to Pair("深色模式 (預設)", "Dark Mode (Default)"),
        "settings_theme_light" to Pair("亮色模式", "Light Mode"),
        "settings_theme_system" to Pair("系統預設", "System Default"),
        "settings_backup_title" to Pair("安全與備份", "Security & Backup"),
        "settings_backup_btn" to Pair("立即產生本地 SQL 備份檔", "Export Local SQL Backup"),
        "settings_restore_btn" to Pair("載入本機備份還原", "Load Local Backup Restore"),
        "settings_backup_toast" to Pair("安全備份檔已匯出至手機 Downloads 資料夾！", "Backup file exported to Downloads folder!"),
        "settings_restore_toast" to Pair("已成功載入備份，所有筆記均已更新還原！", "Backup loaded successfully, all notes updated!"),
        "settings_language_title" to Pair("App 語言設定", "App Language"),
        "settings_lang_zh" to Pair("繁體中文", "繁體中文"),
        "settings_lang_en" to Pair("English", "English"),
        "done" to Pair("完成", "Done")
    )

    fun get(key: String, lang: String): String {
        val pair = translations[key] ?: return key
        return if (lang == "en") pair.second else pair.first
    }

    fun getFolderName(folderName: String, lang: String): String {
        if (lang == "en") return folderName
        return when (folderName) {
            "All" -> "全部"
            "Work" -> "工作"
            "Personal" -> "個人"
            "Ideas" -> "靈感點子"
            "Inspiration" -> "心靈啟發"
            else -> folderName
        }
    }

    fun getTagName(tagName: String, lang: String): String {
        if (lang == "en") return tagName
        return when (tagName) {
            "Work" -> "工作"
            "Design" -> "設計"
            "Personal" -> "個人"
            "Inspiration" -> "心靈啟發"
            else -> tagName
        }
    }

    fun getNoteTitle(title: String, lang: String): String {
        if (lang == "en") return title
        return when (title) {
            "Meeting Notes" -> "會議記錄"
            "Product Design Trends" -> "產品設計趨勢"
            "Weekly Personal Goals" -> "每週個人目標"
            else -> title
        }
    }

    fun getNoteContent(content: String, lang: String): String {
        if (lang == "en") return content
        return when {
            content.startsWith("This meeting note outlines our key product goals") -> {
                "此會議記錄概述了我們的主要產品目標、待辦事項和設計迭代。它採用了具有豐富影像和高保真 Material You 美學搭配的簡潔深色版面。\n\n我們的當前目標是建立一個強大、離線優先的筆記系統，讓使用者能夠享受到絕對流暢、零延遲的體驗。"
            }
            content.startsWith("Exploring Material Design 3 guidelines") -> {
                "探索 Material Design 3 指南與響應式網頁/行動裝置範式：\n\n1. 自適應間距與版面：寬敞的負空間、乾淨的排版搭配層級。\n2. 氛圍感視覺點綴：溫柔的藍色/靛藍色漸層、細緻的卡片邊框，以及圓潤的浮動操作按鈕目標。\n3. 內容重於複雜度：保持設計純淨，避免千篇一律的 AI 範本感視覺。"
            }
            content.startsWith("A clean todo note list to keep track of everyday errands") -> {
                "一個乾淨的待辦筆記清單，用於追蹤日常雜務、習慣養成和隨手記下的想法。"
            }
            else -> content
        }
    }

    fun getChecklistItemText(text: String, lang: String): String {
        if (lang == "en") return text
        return when (text) {
            "Define product requirements doc" -> "定義產品需求文件 (PRD)"
            "Confirm color palette (Zen Slate theme)" -> "確認調色盤 (Zen Slate 主題)"
            "Build interactive Room persistence database" -> "建置互動式 Room 本地資料庫"
            "Research Material You schemes" -> "研究 Material You 設計配色方案"
            "Prototype side-panel transitions for tablet screens" -> "為平板螢幕設計側邊欄過渡動畫原型"
            "Morning jog at the park (5K)" -> "早上在公園慢跑 (5公里)"
            "Pick up sourdough bread & organic apples" -> "去買酸種麵包和有機蘋果"
            "Read two chapters of 'Flow' by Mihaly Csikszentmihalyi" -> "閱讀米哈里·契克森米哈伊《心流》中的兩個章節"
            else -> text
        }
    }
}
