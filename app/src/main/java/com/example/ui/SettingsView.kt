package com.example.ui

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ==========================================
// SYNC MANAGEMENT VIEW
// ==========================================
@Composable
fun SyncView(viewModel: NoteViewModel) {
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncedTime by viewModel.lastSyncedTime.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val showGoogleLogin by viewModel.showGoogleLogin.collectAsStateWithLifecycle()
    val googleAccessToken by viewModel.googleAccessToken.collectAsStateWithLifecycle()

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

    if (showGoogleLogin) {
        Dialog(onDismissRequest = { viewModel.showGoogleLogin.value = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "en") "Google Drive Login" else "登入 Google 雲端硬碟",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { viewModel.showGoogleLogin.value = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (language == "en") "🛠️ AI Studio Sandbox Mode" else "🛠️ AI Studio 測試沙盒模式",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == "en") {
                                    "This prototype runs in a shared development sandbox (jigsaw-puzzle-9d9f1). In the production release, ZenNote will use its own Google Cloud project and native Android Google Sign-In, displaying only ZenNote's brand name and official icon."
                                } else {
                                    "本原型目前運行於開發共享沙盒專案中。在正式發布（Production）版本中，將會配置您專屬的 Google Cloud 專案與 Android 原生登入，屆時將只會顯示您自訂的「唯一筆記」名稱與官方圖標，請安心進行雲端備份功能測試。"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                            "client_id=1058266822028-4phi7fkqia9evug5so12u9thj857ti3j.apps.googleusercontent.com" +
                            "&redirect_uri=https://jigsaw-puzzle-9d9f1.firebaseapp.com/__/auth/handler" +
                            "&response_type=token" +
                            "&scope=https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.appdata"
                    
                    AndroidView(
                        factory = { ctx ->
                            android.webkit.WebView(ctx).apply {
                                webClientOf(viewModel, authUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

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
                        .graphicsLayer {
                            if (isSyncing) {
                                rotationZ = rotationAngle
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

            if (googleAccessToken != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == "en") "Connected to Google Drive" else "已連接 Google 雲端硬碟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { viewModel.clearAccessToken() }) {
                    Text(
                        text = if (language == "en") "Disconnect Google Drive" else "斷開 Google 雲端硬碟連接",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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

private fun android.webkit.WebView.webClientOf(viewModel: NoteViewModel, authUrl: String) {
    webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            url?.let {
                if (it.contains("access_token=")) {
                    val fragment = try {
                        val hashIndex = it.indexOf('#')
                        if (hashIndex != -1) it.substring(hashIndex + 1) else it
                    } catch (e: Exception) {
                        ""
                    }
                    if (fragment.contains("access_token=")) {
                        val params = fragment.split("&").associate { param ->
                            val parts = param.split("=")
                            if (parts.size >= 2) parts[0] to parts[1] else parts[0] to ""
                        }
                        val token = params["access_token"]
                        if (!token.isNullOrEmpty()) {
                            viewModel.saveAccessToken(token)
                            viewModel.showGoogleLogin.value = false
                            viewModel.triggerSync()
                        }
                    }
                }
            }
        }
    }
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    loadUrl(authUrl)
}

// ==========================================
// SETTINGS DIALOG
// ==========================================
@Composable
fun SettingsDialog(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingBackupContent by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    // Launcher for saving backup file to user-chosen path
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/sql")
    ) { uri: Uri? ->
        uri?.let { saveUri ->
            val content = pendingBackupContent ?: viewModel.cachedBackupSqlContent ?: ""
            viewModel.saveBackupToSelectedUri(context, saveUri, content) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Launcher for selecting local backup file to restore
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { openUri ->
            isRestoring = true
            viewModel.restoreLocalBackupFromUri(context, openUri) { success, msg ->
                isRestoring = false
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

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
                        if (!isExporting) {
                            isExporting = true
                            viewModel.prepareBackupSql { sqlContent ->
                                isExporting = false
                                if (sqlContent.isNotBlank()) {
                                    pendingBackupContent = sqlContent
                                    try {
                                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                        createDocumentLauncher.launch("zennote_backup_$timeStamp.sql")
                                    } catch (e: Exception) {
                                        Log.e("SettingsDialog", "Create document launcher error", e)
                                        Toast.makeText(context, "無法開啟檔案儲存選擇器：${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "產生備份檔失敗！", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在產生 SQL 備份...")
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Backup")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.get("settings_backup_btn", language))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (!isRestoring) {
                            try {
                                openDocumentLauncher.launch(arrayOf("*/*"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "無法開啟檔案選擇器：${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isRestoring,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在還原資料庫...")
                    } else {
                        Icon(Icons.Default.Backup, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.get("settings_restore_btn", language), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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

// Helper extension function to move items in a list
fun <T> List<T>.move(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in indices) return this
    val list = toMutableList()
    val item = list.removeAt(fromIndex)
    list.add(toIndex, item)
    return list
}

@Composable
fun ReorderGrip(
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    val tint = if (isDragging) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(12.dp, 18.dp)) {
            val dotRadius = 1.5.dp.toPx()
            val spacingX = 6.dp.toPx()
            val spacingY = 5.dp.toPx()

            // Draw 2x3 dots grid
            for (col in 0..1) {
                for (row in 0..2) {
                    drawCircle(
                        color = tint,
                        radius = dotRadius,
                        center = androidx.compose.ui.geometry.Offset(
                            x = col * spacingX + 3.dp.toPx(),
                            y = row * spacingY + 4.dp.toPx()
                        )
                    )
                }
            }
        }
    }
}
