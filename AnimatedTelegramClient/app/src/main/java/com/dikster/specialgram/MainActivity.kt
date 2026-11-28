@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.dikster.specialgram

import android.os.Bundle
import android.net.Uri
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import androidx.core.content.FileProvider
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import android.widget.VideoView
import androidx.core.view.WindowCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

private data class UiAppearance(
    val background: Color, val panel: Color, val panelLight: Color, val chatBackground: Color, val accent: Color,
    val outgoing: Color, val incoming: Color, val text: Color, val muted: Color, val glass: Float, val bubbleRadius: Float
)
private val LocalAppearance = staticCompositionLocalOf {
    UiAppearance(Color(0xFF0E1621), Color(0xFF17212B), Color(0xFF202B36), Color(0xFF0E1621), Color(0xFF2AABEE),
        Color(0xFF2B5278), Color(0xFF182533), Color(0xFFEAF2F8), Color(0xFF8A9AA9), 0f, 18f)
}


class MainActivity : ComponentActivity() {
    private lateinit var td: TdLibClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.rgb(14, 22, 33)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        td = (application as SpecialgramApp).td
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF2AABEE), background = Color(0xFF0E1621), surface = Color(0xFF17212B),
                onBackground = Color(0xFFEAF2F8), onSurface = Color(0xFFEAF2F8)
            )) { TelegramApp(td) }
        }
    }
}

@Composable
private fun TelegramApp(td: TdLibClient) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("appearance", android.content.Context.MODE_PRIVATE) }
    var themeName by remember { mutableStateOf(prefs.getString("theme", "Telegram") ?: "Telegram") }
    var glass by remember { mutableFloatStateOf(prefs.getFloat("glass", 0.35f)) }
    var fontScale by remember { mutableFloatStateOf(prefs.getFloat("fontScale", 1f)) }
    var accentName by remember { mutableStateOf(prefs.getString("accent", "По теме") ?: "По теме") }
    var bubbleRadius by remember { mutableFloatStateOf(prefs.getFloat("bubbleRadius", 18f)) }
    var wallpaperName by remember { mutableStateOf(prefs.getString("wallpaper", "По теме") ?: "По теме") }
    var compactChats by remember { mutableStateOf(prefs.getBoolean("compactChats", false)) }
    var headerWallpaper by remember { mutableStateOf(prefs.getString("headerWallpaper", null)) }
    var videoNoteShape by remember { mutableStateOf(prefs.getString("videoNoteShape", "circle") ?: "circle") }
    var selectedChat by remember { mutableStateOf<TgChat?>(null) }
    val profilePhotoPicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching { MediaPreparation.prepareProfilePhoto(context, uri) }.onSuccess { td.setMyProfilePhoto(it.absolutePath) }.onFailure { td.reportOperationError("Не удалось подготовить фото профиля: ${it.message ?: "ошибка файла"}") }
    }
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            headerWallpaper = uri.toString()
        }
    }
    val chatPhotoPicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        val chatId = selectedChat?.id
        if (uri != null && chatId != null) runCatching { MediaPreparation.prepareOutgoingMedia(context, uri, "specialgram_photo", 10L * 1024L * 1024L) }
            .onSuccess { td.sendPhoto(chatId, it.absolutePath) }
            .onFailure { td.reportOperationError("Не удалось отправить фото: ${it.message ?: "ошибка файла"}") }
    }
    val chatDocumentPicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        val chatId = selectedChat?.id
        if (uri != null && chatId != null) runCatching { MediaPreparation.prepareOutgoingMedia(context, uri, "specialgram_document", 50L * 1024L * 1024L) }
            .onSuccess { td.sendDocument(chatId, it.absolutePath) }
            .onFailure { td.reportOperationError("Не удалось отправить файл: ${it.message ?: "ошибка файла"}") }
    }
    val storyImagePicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { MediaPreparation.prepareStoryPhoto(context, uri) }
                .onSuccess { td.postPhotoStory(it.absolutePath) }
                .onFailure { td.reportOperationError("Не удалось подготовить фото для истории: " + (it.message ?: "ошибка файла")) }
        }
    }
    val storyVideoPicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { MediaPreparation.prepareStoryVideo(context, uri) }
                .onSuccess { (file, duration) -> td.postVideoStory(file.absolutePath, duration) }
                .onFailure { td.reportOperationError("Видео не подходит для истории: " + (it.message ?: "ошибка файла")) }
        }
    }
    val appearance = remember(themeName, glass, accentName, bubbleRadius, wallpaperName) {
        val colors = when (themeName) {
            "Midnight" -> listOf(Color(0xFF080B16), Color(0xFF11172A), Color(0xFF1A2340), Color(0xFF8B7CFF), Color(0xFF514A91), Color(0xFF151B30))
            "Aurora" -> listOf(Color(0xFF071A20), Color(0xFF102B32), Color(0xFF193B43), Color(0xFF45D6C5), Color(0xFF236D69), Color(0xFF153039))
            "Rose" -> listOf(Color(0xFF1C101B), Color(0xFF2B1929), Color(0xFF3A2237), Color(0xFFFF78B7), Color(0xFF75405F), Color(0xFF382334))
            "Космос" -> listOf(Color(0xFF080B19), Color(0xFF17152D), Color(0xFF282047), Color(0xFF9B7BFF), Color(0xFF5142A0), Color(0xFF17162D))
            else -> listOf(Color(0xFF0E1621), Color(0xFF17212B), Color(0xFF202B36), Color(0xFF2AABEE), Color(0xFF2B5278), Color(0xFF182533))
        }
        val chatBackground = when(wallpaperName) { "Глубокая ночь" -> Color(0xFF080F1A); "Океан" -> Color(0xFF0A2028); "Сумерки" -> Color(0xFF211629); else -> colors[0] }
        val accent = when(accentName) { "Синий" -> Color(0xFF2AABEE); "Фиолетовый" -> Color(0xFF9B82FF); "Мятный" -> Color(0xFF42D6B5); "Розовый" -> Color(0xFFFF78B7); else -> colors[3] }
        UiAppearance(colors[0], colors[1], colors[2], chatBackground, accent, colors[4], colors[5],
            Color(0xFFEAF2F8), Color(0xFF8A9AA9), glass, bubbleRadius)
    }
    LaunchedEffect(themeName, glass, fontScale, accentName, bubbleRadius, wallpaperName, compactChats, headerWallpaper) {
        prefs.edit().putString("theme", themeName).putFloat("glass", glass).putFloat("fontScale", fontScale).putString("accent", accentName).putFloat("bubbleRadius", bubbleRadius).putString("wallpaper", wallpaperName).putBoolean("compactChats", compactChats).putString("headerWallpaper", headerWallpaper).apply()
    }
    LaunchedEffect(videoNoteShape) { prefs.edit().putString("videoNoteShape", videoNoteShape).apply() }
    CompositionLocalProvider(LocalAppearance provides appearance,
        androidx.compose.ui.platform.LocalDensity provides Density(androidx.compose.ui.platform.LocalDensity.current.density, fontScale)) {
    val auth by td.auth.collectAsState()
    val chats by td.chats.collectAsState()
    val stories by td.stories.collectAsState()
    val contacts by td.contacts.collectAsState()
    val messages by td.messages.collectAsState()
    val searchResults by td.searchResults.collectAsState()
    val profile by td.profile.collectAsState()
    val account by td.account.collectAsState()
    val privacyLastSeen by td.privacyLastSeen.collectAsState()
    val privacyPhone by td.privacyPhone.collectAsState()
    val privacyPhoto by td.privacyPhoto.collectAsState()
    val operationError by td.operationError.collectAsState()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var contactsQuery by remember { mutableStateOf("") }
    var sortNewest by remember { mutableStateOf(true) }
    val videoCaptureLauncher = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        val chatId = selectedChat?.id
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null && chatId != null) {
            val target = File(context.cacheDir, "specialgram_video_${System.currentTimeMillis()}.mp4")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
                    ?: error("Камера не вернула видеофайл")
            }.onSuccess { td.sendVideo(chatId, target.absolutePath, videoNoteShape) }
                .onFailure { td.reportOperationError("Не удалось получить видео: ${it.message ?: "ошибка файла"}") }
        } else if (result.resultCode == android.app.Activity.RESULT_OK) {
            td.reportOperationError("Камера не вернула ссылку на записанное видео. Попробуйте другую камеру.")
        }
    }

    BackHandler(enabled = selectedChat != null) {
        td.closeProfile()
        selectedChat = null
    }

    Surface(Modifier.fillMaxSize(), color = LocalAppearance.current.background) {
        AnimatedContent(targetState = auth, label = "main-screen") { state ->
            when (state) {
                Auth.Loading -> CenterPanel("Подключаемся к Telegram…", "Пожалуйста, подождите")
                Auth.Phone -> AuthScreen("Ваш номер телефона", "Введите номер в международном формате", phone, { phone = it }, "Продолжить", { td.phone(phone) })
                Auth.Code -> AuthScreen("Код подтверждения", "Код придёт от Telegram", code, { code = it }, "Подтвердить", { td.code(code) })
                Auth.Password -> AuthScreen("Облачный пароль", "Введите пароль двухэтапной проверки", password, { password = it }, "Войти", { td.password(password) }, true)
                Auth.Ready -> {
                    if (selectedChat == null) {
                        ChatsScreen(
                            chats = chats,
                            contacts = contacts,
                            stories = stories,
                            onOpenStory = { td.openStory(it.chatId,it.storyId) },
                            onCloseStory = { td.closeStory(it.chatId,it.storyId) },
                            onReactToStory = { td.reactToStory(it.chatId,it.storyId) },
                            onPostStory = { storyImagePicker.launch(arrayOf("image/*")) },
                            onPostVideoStory = { storyVideoPicker.launch(arrayOf("video/mp4", "video/*")) },
                            onReplyToStory = { story, text -> td.replyToStory(story.chatId, story.storyId, text) },
                            query = query,
                            onQuery = { query = it },
                            contactsQuery = contactsQuery,
                            onContactsQuery = { contactsQuery = it },
                            sortNewest = sortNewest,
                            onSort = { sortNewest = it },
                            compactChats = compactChats,
                            onCompactChats = { compactChats = it },
                            account = account, onSaveMyProfile = { first, last, bio -> td.updateMyProfile(first, last, bio) }, onPickProfilePhoto = { profilePhotoPicker.launch(arrayOf("image/*")) }, operationError = operationError, onDismissOperationError = { td.clearOperationError() }, privacyLastSeen = privacyLastSeen, onPrivacyLastSeen = { td.setLastSeenPrivacy(it) }, privacyPhone = privacyPhone, onPrivacyPhone = { td.setPhonePrivacy(it) }, privacyPhoto = privacyPhoto, onPrivacyPhoto = { td.setPhotoPrivacy(it) }, themeName = themeName, onTheme = { themeName = it },
                            glass = glass, onGlass = { glass = it }, fontScale = fontScale, onFontScale = { fontScale = it },
                            accentName = accentName, onAccent = { accentName = it }, bubbleRadius = bubbleRadius, onBubbleRadius = { bubbleRadius = it },
                            wallpaperName = wallpaperName, onWallpaper = { wallpaperName = it },
                            headerWallpaper = headerWallpaper,
                            onPickHeaderWallpaper = { imagePicker.launch(arrayOf("image/*")) },
                            onClearHeaderWallpaper = { headerWallpaper = null },
                            onSelectCosmicCover = { headerWallpaper = "preset:cosmic" },
                            onOpen = { selectedChat = it; td.openChat(it.id) },
                            onOpenContact = { contact -> td.openContact(contact.id) { selectedChat = it; td.openChat(it.id) } },
                            onArchive = { td.archiveChat(it.id, !it.archived) },
                            onTogglePinned = { chat -> td.toggleChatPinned(chat.id, !chat.isPinned, chat.archived) },
                            onToggleUnread = { chat -> td.toggleChatUnread(chat.id, !chat.isMarkedAsUnread) },
                            onToggleMute = { chat -> td.toggleChatMuted(chat.id, !chat.isMuted) }
                        )
                    } else {
                        ConversationScreen(
                            chat = selectedChat!!,
                            chats = chats,
                            messages = messages,
                            searchResults = searchResults,
                            onSearchMessages = { td.searchChatMessages(selectedChat!!.id,it) },
                            profile = profile,
                            onOpenProfile = { td.openProfile(selectedChat!!.id) },
                            onCloseProfile = { td.closeProfile() },
                            onBack = { selectedChat = null },
                            onSend = { text, animated, replyToMessageId ->
                                if (animated) td.sendAnimated(selectedChat!!.id, text, replyToMessageId)
                                else td.sendPlain(selectedChat!!.id, text, replyToMessageId)
                            },
                            onDeleteMessage = { message, revoke -> td.deleteMessage(selectedChat!!.id, message.id, revoke) },
                            onClearHistory = { revoke -> td.clearHistory(selectedChat!!.id, revoke) },
                            onEditMessage = { message, text -> td.editMessage(selectedChat!!.id, message.id, text) },
                            onReactToMessage = { message, emoji -> td.reactToMessage(selectedChat!!.id, message.id, emoji) },
                            onForwardMessage = { message, destination -> td.forwardMessage(selectedChat!!.id, destination.id, message.id) },
                            onPinMessage = { message -> td.pinMessage(selectedChat!!.id, message.id, !message.isPinned) },
                            videoNoteShape = videoNoteShape,
                            onVideoNoteShape = { videoNoteShape = it },
                            onRecordVideo = { runCatching { videoCaptureLauncher.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE).putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60)) }.onFailure { td.reportOperationError("Не удалось открыть камеру: ${it.message ?: "камера недоступна"}") } },
                            operationError = operationError,
                            onDismissOperationError = { td.clearOperationError() },
                            onLoadOlderMessages = { td.loadOlderMessages(selectedChat!!.id) },
                            onSendPhoto = { chatPhotoPicker.launch(arrayOf("image/*")) },
                            onSendDocument = { chatDocumentPicker.launch(arrayOf("*/*")) }
                        )
                    }
                }
                is Auth.Error -> CenterPanel("Не удалось подключиться", state.text)
            }
        }
    }
    }
}

@Composable
private fun ChatsScreen(chats: List<TgChat>, contacts: List<TgContact>, stories: List<TgStory>, onOpenStory: (TgStory) -> Unit, onCloseStory: (TgStory) -> Unit, onReactToStory: (TgStory) -> Unit, onPostStory: () -> Unit, onPostVideoStory: () -> Unit, onReplyToStory: (TgStory, String) -> Unit, query: String, onQuery: (String) -> Unit, contactsQuery: String, onContactsQuery: (String) -> Unit, sortNewest: Boolean, onSort: (Boolean) -> Unit, compactChats: Boolean, onCompactChats: (Boolean) -> Unit, account: TgAccount?, onSaveMyProfile: (String,String,String) -> Unit, onPickProfilePhoto: () -> Unit, operationError: String?, onDismissOperationError: () -> Unit, privacyLastSeen: String, onPrivacyLastSeen: (String) -> Unit, privacyPhone: String, onPrivacyPhone: (String) -> Unit, privacyPhoto: String, onPrivacyPhoto: (String) -> Unit, themeName: String, onTheme: (String) -> Unit, glass: Float, onGlass: (Float) -> Unit, fontScale: Float, onFontScale: (Float) -> Unit, accentName: String, onAccent: (String) -> Unit, bubbleRadius: Float, onBubbleRadius: (Float) -> Unit, wallpaperName: String, onWallpaper: (String) -> Unit, headerWallpaper: String?, onPickHeaderWallpaper: () -> Unit, onClearHeaderWallpaper: () -> Unit, onSelectCosmicCover: () -> Unit, onOpen: (TgChat) -> Unit, onOpenContact: (TgContact) -> Unit, onArchive: (TgChat) -> Unit, onTogglePinned: (TgChat) -> Unit, onToggleUnread: (TgChat) -> Unit, onToggleMute: (TgChat) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var showStoryComposer by remember { mutableStateOf(false) }
    var viewingStory by remember { mutableStateOf<Pair<Long,Int>?>(null) }
    val orderedStories = remember(stories) { stories.sortedWith(compareBy<TgStory> { it.date }.thenBy { it.chatId }.thenBy { it.storyId }) }
    val activeStoryIndex = orderedStories.indexOfFirst { viewingStory == (it.chatId to it.storyId) }
    val activeStory = orderedStories.getOrNull(activeStoryIndex)
    var selectedTab by remember { mutableStateOf(0) }
    BackHandler(enabled = selectedTab != 0) { selectedTab = 0 }
    var unreadOnly by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf("Все") }
    val filtered = remember(chats, query, sortNewest, unreadOnly, folder) {
        val found = chats.filter { chat -> chat.title.contains(query, ignoreCase = true) && (!unreadOnly || chat.unreadCount > 0) && when(folder) { "Архив" -> chat.archived; "Личные" -> !chat.archived && (chat.kind == "Личный чат" || chat.kind == "Секретный чат"); "Группы" -> !chat.archived && chat.kind == "Группа"; "Каналы" -> !chat.archived && chat.kind == "Канал"; else -> !chat.archived } }
        if (sortNewest) found.sortedWith(compareByDescending<TgChat> { it.isPinned }.thenByDescending { it.lastDate })
        else found.sortedWith(compareByDescending<TgChat> { it.isPinned }.thenBy { it.title.lowercase(Locale.getDefault()) })
    }
    Column(Modifier.fillMaxSize().background(LocalAppearance.current.background)) {
        Box(Modifier.fillMaxWidth()) {
            HeaderWallpaper(uri = if (selectedTab == 0) headerWallpaper else null, modifier = Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
                Row(Modifier.fillMaxWidth().background(Color.Transparent).padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(42.dp)) {
                        Text("☰", color = Color.White, fontSize = 23.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (selectedTab == 2) "Настройки" else if (selectedTab == 1) "Контакты" else "Specialgram", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(if (selectedTab == 0) "${chats.count { !it.archived }} диалогов" else "Specialgram · Telegram", color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Text("⌕", color = Color.White, fontSize = 25.sp) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Чаты") }, onClick = { selectedTab = 0; menuOpen = false })
                            DropdownMenuItem(text = { Text("Контакты") }, onClick = { selectedTab = 1; menuOpen = false })
                            DropdownMenuItem(text = { Text("Настройки") }, onClick = { selectedTab = 2; menuOpen = false })
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Сначала новые") }, onClick = { onSort(true); selectedTab = 0; menuOpen = false })
                            DropdownMenuItem(text = { Text("По названию: А — Я") }, onClick = { onSort(false); selectedTab = 0; menuOpen = false })
                            DropdownMenuItem(text = { Text(if (unreadOnly) "Показать все чаты" else "Только непрочитанные") }, onClick = { unreadOnly = !unreadOnly; selectedTab = 0; menuOpen = false })
                        }
                    }
                }
                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = query, onValueChange = onQuery,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
                        singleLine = true,
                        placeholder = { Text("Поиск", color = Color.White.copy(alpha = 0.66f)) },
                        leadingIcon = { Text("⌕", fontSize = 24.sp, color = Color.White.copy(alpha = 0.82f)) },
                        trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { onQuery("") }) { Text("✕", color = Color.White.copy(alpha = 0.82f), fontSize = 16.sp) } },
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.16f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.12f),
                            focusedBorderColor = Color.White.copy(alpha = 0.30f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        )
                    )

                }
            }
        }
        if (selectedTab == 2) {
            Box(Modifier.weight(1f).fillMaxWidth()) { SettingsPanel(account, onSaveMyProfile, onPickProfilePhoto, operationError, onDismissOperationError, privacyLastSeen, onPrivacyLastSeen, privacyPhone, onPrivacyPhone, privacyPhoto, onPrivacyPhoto, themeName, onTheme, glass, onGlass, fontScale, onFontScale, accentName, onAccent, bubbleRadius, onBubbleRadius, wallpaperName, onWallpaper, compactChats, onCompactChats, headerWallpaper, onPickHeaderWallpaper, onClearHeaderWallpaper, onSelectCosmicCover) }
        } else if (selectedTab == 1) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                ContactsPanel(contacts.filter { it.name.contains(contactsQuery, true) || it.username.contains(contactsQuery, true) || it.phone.contains(contactsQuery) }, contactsQuery, onContactsQuery, onOpenContact)
            }
        } else {
            if (folder != "Архив") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                    Column(Modifier.width(70.dp).clickable { showStoryComposer = true }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(62.dp).border(2.dp, LocalAppearance.current.accent, CircleShape).padding(4.dp).clip(CircleShape).background(LocalAppearance.current.panelLight), contentAlignment = Alignment.Center) {
                            Text("+", color = LocalAppearance.current.accent, fontSize = 34.sp, fontWeight = FontWeight.Light)
                        }
                        Text("Моя история", color = LocalAppearance.current.text, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    stories.distinctBy { it.chatId }.forEach { story ->
                        val owner = chats.firstOrNull { it.id == story.chatId }
                        Column(Modifier.width(70.dp).clickable {
                            val first = stories.filter { it.chatId == story.chatId }.let { items -> items.firstOrNull { !it.isViewed } ?: items.firstOrNull() }
                            if (first != null) { viewingStory = first.chatId to first.storyId; onOpenStory(first) }
                        }, horizontalAlignment = Alignment.CenterHorizontally) {
                            val groupStories = stories.filter { it.chatId == story.chatId }
                            Avatar(path = owner?.avatarPath ?: "", fallback = (owner?.title ?: story.ownerName).take(1).uppercase(), id = story.chatId, size = 62, hasStory = true, storyViewed = groupStories.all { it.isViewed })
                            Text(owner?.title ?: story.ownerName, color = LocalAppearance.current.text, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Все", "Личные", "Группы", "Каналы", "Архив").forEach { item ->
                    Column(
                        Modifier.clickable { folder = item }.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(item, color = if (folder == item) LocalAppearance.current.accent else LocalAppearance.current.muted, fontSize = 14.sp, fontWeight = if (folder == item) FontWeight.SemiBold else FontWeight.Normal)
                        Spacer(Modifier.height(7.dp))
                        Box(Modifier.width(if (folder == item) 25.dp else 8.dp).height(3.dp).clip(CircleShape).background(if (folder == item) LocalAppearance.current.accent else Color.Transparent))
                    }
                }
                    }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Все чаты", color = LocalAppearance.current.accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text(if (unreadOnly) "Непрочитанные" else if (sortNewest) "По активности" else "А — Я", color = LocalAppearance.current.muted, fontSize = 12.sp)
            }
            if (filtered.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(if (chats.isEmpty()) "Загружаем чаты…" else "Ничего не найдено", color = LocalAppearance.current.muted) }
            else LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(filtered, key = { it.id }) { chat ->
                    ChatRow(
                        chat,
                        compact = compactChats,
                        onArchive = { onArchive(chat) },
                        onTogglePinned = { onTogglePinned(chat) },
                        onToggleUnread = { onToggleUnread(chat) },
                        onToggleMute = { onToggleMute(chat) }
                    ) { onOpen(chat) }
                    HorizontalDivider(color = LocalAppearance.current.muted.copy(alpha = 0.16f), thickness = 0.6.dp, modifier = Modifier.padding(start = 82.dp))
                }
            }
        }
        if (showStoryComposer) AlertDialog(
            onDismissRequest = { showStoryComposer = false },
            title = { Text("Добавить историю", color = LocalAppearance.current.text) },
            text = { Text("Выбери фото или видео из галереи.", color = LocalAppearance.current.muted) },
            confirmButton = {
                Row {
                    TextButton(onClick = { showStoryComposer = false; onPostStory() }) { Text("Фото") }
                    TextButton(onClick = { showStoryComposer = false; onPostVideoStory() }) { Text("Видео") }
                }
            },
            dismissButton = { TextButton(onClick = { showStoryComposer = false }) { Text("Отмена") } }
        )
        if (activeStory != null) StoryViewer(
            story = activeStory,
            onDismiss = { onCloseStory(activeStory); viewingStory = null },
            onPrevious = {
                val previous = orderedStories.getOrNull(activeStoryIndex - 1)
                if (previous != null) { onCloseStory(activeStory); viewingStory = previous.chatId to previous.storyId; onOpenStory(previous) }
            },
            onNext = {
                val next = orderedStories.getOrNull(activeStoryIndex + 1)
                if (next != null) { onCloseStory(activeStory); viewingStory = next.chatId to next.storyId; onOpenStory(next) }
                else { onCloseStory(activeStory); viewingStory = null }
            },
            storyPosition = activeStoryIndex + 1,
            storyCount = orderedStories.size,
            onReact = { onReactToStory(activeStory) },
            onReply = { text -> onReplyToStory(activeStory, text) }
        )

    }
}

@Composable
private fun HeaderWallpaper(uri: String?, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap = remember(uri) {
        if (uri.isNullOrBlank()) null else runCatching {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull()
    }
    if (uri == "preset:cosmic") {
        Box(modifier.background(Brush.linearGradient(listOf(Color(0xFF080B19), Color(0xFF29204D), Color(0xFF10152C))))) {
            Canvas(Modifier.matchParentSize()) {
                val stars = listOf(0.08f to 0.22f, 0.17f to 0.71f, 0.29f to 0.36f, 0.38f to 0.82f, 0.49f to 0.18f, 0.57f to 0.56f, 0.68f to 0.28f, 0.76f to 0.76f, 0.88f to 0.39f, 0.94f to 0.15f, 0.23f to 0.12f, 0.83f to 0.9f)
                stars.forEachIndexed { index, point ->
                    drawCircle(color = if (index % 3 == 0) Color(0xFFB9A8FF).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.72f), radius = if (index % 4 == 0) 2.2.dp.toPx() else 1.1.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * point.first, size.height * point.second))
                }
            }
            Box(Modifier.matchParentSize().background(Color(0x33000000)))
        }
    } else if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = "Фон верхней панели", modifier = modifier, contentScale = ContentScale.Crop)
        Box(modifier.background(Color(0x99000000)))
    } else {
        Box(modifier.background(LocalAppearance.current.panel))
    }
}

@Composable
private fun ContactsPanel(contacts: List<TgContact>, query: String, onQuery: (String) -> Unit, onOpen: (TgContact) -> Unit) {
    Column(Modifier.fillMaxSize().background(LocalAppearance.current.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Контакты", color = LocalAppearance.current.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(contacts.size.toString(), color = LocalAppearance.current.muted, fontSize = 13.sp)
        }
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), singleLine = true,
            placeholder = { Text("Поиск контактов") },
            trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { onQuery("") }) { Text("✕", color = LocalAppearance.current.muted, fontSize = 15.sp) } },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = LocalAppearance.current.panelLight.copy(alpha = 1f - LocalAppearance.current.glass * 0.38f), unfocusedContainerColor = LocalAppearance.current.panelLight.copy(alpha = 1f - LocalAppearance.current.glass * 0.38f), focusedBorderColor = LocalAppearance.current.accent, unfocusedBorderColor = Color.Transparent, focusedTextColor = LocalAppearance.current.text, unfocusedTextColor = LocalAppearance.current.text))
        if (contacts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Контакты не найдены", color = LocalAppearance.current.muted)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(contacts, key = { it.id }) { contact ->
                    Row(Modifier.fillMaxWidth().clickable { onOpen(contact) }.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(path = "", fallback = contact.name.take(1).uppercase(), id = contact.id, size = 48, online = contact.isOnline)
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(contact.name, color = LocalAppearance.current.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (contact.isPremium) Text("  ✦", color = Color(0xFFB58CFF), fontSize = 12.sp)
                            }
                            Text(if (contact.isOnline) "в сети" else contact.status.ifBlank { contact.username.takeIf { it.isNotBlank() }?.let { "@$it" } ?: contact.phone }, color = if (contact.isOnline) Color(0xFF35C759) else LocalAppearance.current.muted, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = Color(0xFF253341), thickness = 0.6.dp, modifier = Modifier.padding(start = 76.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    account: TgAccount?, onSaveMyProfile: (String,String,String) -> Unit, onPickProfilePhoto: () -> Unit, operationError: String?, onDismissOperationError: () -> Unit, privacyLastSeen: String, onPrivacyLastSeen: (String) -> Unit, privacyPhone: String, onPrivacyPhone: (String) -> Unit, privacyPhoto: String, onPrivacyPhoto: (String) -> Unit, themeName: String, onTheme: (String) -> Unit,
    glass: Float, onGlass: (Float) -> Unit, fontScale: Float, onFontScale: (Float) -> Unit,
    accentName: String, onAccent: (String) -> Unit, bubbleRadius: Float, onBubbleRadius: (Float) -> Unit,
    wallpaperName: String, onWallpaper: (String) -> Unit, compactChats: Boolean, onCompactChats: (Boolean) -> Unit,
    headerWallpaper: String?, onPickHeaderWallpaper: () -> Unit, onClearHeaderWallpaper: () -> Unit, onSelectCosmicCover: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tdlibMediaBytes = remember { File(context.filesDir, "tdlib-files").let { root -> if (root.exists()) root.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L } }
    val cacheBytes = remember { context.cacheDir.let { root -> if (root.exists()) root.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L } }
    var page by remember { mutableStateOf("main") }
    BackHandler(enabled = page != "main") {
        page = when (page) { "cover" -> "appearance"; else -> "main" }
    }
    Column(Modifier.fillMaxSize().background(LocalAppearance.current.background)) {
        if (page != "main") {
            Row(Modifier.fillMaxWidth().background(LocalAppearance.current.panel).clickable {
                page = if (page == "cover") "appearance" else "main"
            }.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = LocalAppearance.current.accent, fontSize = 30.sp)
                Spacer(Modifier.width(12.dp))
                Text(when(page) { "cover" -> "Обложка Specialgram"; "appearance" -> "Внешний вид"; "account" -> "Аккаунт"; "notifications" -> "Уведомления"; "privacy" -> "Конфиденциальность"; "storage" -> "Данные и память"; "diagnostics" -> "Диагностика"; else -> "О приложении" }, color = LocalAppearance.current.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (page == "main") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Avatar(path = account?.avatarPath ?: "", fallback = (account?.name ?: "S").take(1).uppercase(), id = 0, size = 60)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account?.name ?: "Мой аккаунт Telegram", color = LocalAppearance.current.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (account?.username.isNullOrBlank()) "Подключён через TDLib" else "@${account?.username}", color = LocalAppearance.current.muted, fontSize = 13.sp)
                        if (account?.isPremium == true) Text("✦ Telegram Premium", color = Color(0xFFB58CFF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = LocalAppearance.current.muted.copy(alpha = 0.18f))
                SettingsNavigationRow("Внешний вид", "Темы, обложка и оформление") { page = "appearance" }
                SettingsNavigationRow("Аккаунт", "Номер телефона и профиль") { page = "account" }
                SettingsNavigationRow("Уведомления", "Звуки и оповещения") { page = "notifications" }
                SettingsNavigationRow("Конфиденциальность", "Последний визит и безопасность") { page = "privacy" }
                SettingsNavigationRow("Данные и память", "Сеть и хранилище") { page = "storage" }
                SettingsNavigationRow("Диагностика", "Журнал технических ошибок TDLib") { page = "diagnostics" }
                SettingsNavigationRow("О приложении", "Specialgram · 0.3") { page = "about" }
                Text("Авторская функция", color = LocalAppearance.current.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                Text("Постепенную отправку текста можно включить отдельно в меню каждого чата.", color = LocalAppearance.current.muted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(bottom = 18.dp))
            } else if (page == "appearance") {
                Text("SPECIALGRAM · ВНЕШНИЙ ВИД", color = LocalAppearance.current.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 17.dp, bottom = 9.dp))
                Text("Цветовая тема", color = LocalAppearance.current.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Telegram", "Midnight", "Aurora", "Rose", "Космос").forEach { name -> FilterChip(selected = themeName == name, onClick = { onTheme(name) }, label = { Text(name) }) }
                }
                Text("Акцентный цвет", color = LocalAppearance.current.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("По теме", "Синий", "Фиолетовый", "Мятный", "Розовый").forEach { name -> FilterChip(selected = accentName == name, onClick = { onAccent(name) }, label = { Text(name) }) }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Эффект стекла", color = LocalAppearance.current.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text("Прозрачность панелей и карточек", color = LocalAppearance.current.muted, fontSize = 12.sp) }
                    Text("${(glass * 100).toInt()}%", color = LocalAppearance.current.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(value = glass, onValueChange = onGlass, valueRange = 0f..1f)
                Surface(color = LocalAppearance.current.panel.copy(alpha = 1f - LocalAppearance.current.glass * 0.30f), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Предпросмотр Specialgram", color = LocalAppearance.current.text, fontWeight = FontWeight.SemiBold)
                        Text("Так будут выглядеть панели приложения", color = LocalAppearance.current.muted, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        Surface(color = LocalAppearance.current.panelLight.copy(alpha = 1f - LocalAppearance.current.glass * 0.38f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(34.dp).clip(CircleShape).background(LocalAppearance.current.accent), contentAlignment = Alignment.Center) { Text("A", color = Color.White, fontWeight = FontWeight.Bold) }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) { Text("Specialgram", color = LocalAppearance.current.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text("Пример полупрозрачной карточки", color = LocalAppearance.current.muted, fontSize = 11.sp) }
                                Text("✓✓", color = LocalAppearance.current.accent, fontSize = 12.sp)
                            }
                        }
                    }
                }
                SettingsNavigationRow("Обложка Specialgram", "Стандартная, космическая или своя фотография") { page = "cover" }
                Text("Фон переписки", color = LocalAppearance.current.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 9.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("По теме", "Глубокая ночь", "Океан", "Сумерки").forEach { name -> FilterChip(selected = wallpaperName == name, onClick = { onWallpaper(name) }, label = { Text(name) }) }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Компактный список чатов", color = LocalAppearance.current.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text("Больше диалогов на экране", color = LocalAppearance.current.muted, fontSize = 12.sp) }
                    Switch(checked = compactChats, onCheckedChange = onCompactChats)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Скругление сообщений", color = LocalAppearance.current.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text("Форма пузырей в переписке", color = LocalAppearance.current.muted, fontSize = 12.sp) }
                    Text("${bubbleRadius.toInt()} dp", color = LocalAppearance.current.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(value = bubbleRadius, onValueChange = onBubbleRadius, valueRange = 8f..28f)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Размер текста", color = LocalAppearance.current.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text("Масштаб интерфейса", color = LocalAppearance.current.muted, fontSize = 12.sp) }
                    Text("${(fontScale * 100).toInt()}%", color = LocalAppearance.current.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(value = fontScale, onValueChange = onFontScale, valueRange = 0.85f..1.25f)
            } else if (page == "cover") {
                Text("Выбери, что будет отображаться в верхней части списка чатов.", color = LocalAppearance.current.muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                CoverChoice("Стандартная", "Обычный фон Telegram", headerWallpaper == null) { onClearHeaderWallpaper() }
                CoverChoice("Космос", "Градиент со звёздами", headerWallpaper == "preset:cosmic") { onSelectCosmicCover() }
                CoverChoice("Моя фотография", "Выбрать изображение из галереи", headerWallpaper != null && headerWallpaper != "preset:cosmic") { onPickHeaderWallpaper() }
            } else if (page == "privacy") {
                Text("Кто видит время последнего посещения и статус «в сети»", color = LocalAppearance.current.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
                Text("Настройка применяется к аккаунту Telegram и синхронизируется с другими клиентами.", color = LocalAppearance.current.muted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 14.dp))
                listOf("Все", "Мои контакты", "Никто").forEach { choice ->
                    Row(Modifier.fillMaxWidth().clickable { onPrivacyLastSeen(choice) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(choice, color = LocalAppearance.current.text, fontSize = 15.sp)
                            if (privacyLastSeen == choice) Text("Текущий выбор", color = LocalAppearance.current.accent, fontSize = 11.sp)
                        }
                        RadioButton(selected = privacyLastSeen == choice, onClick = { onPrivacyLastSeen(choice) })
                    }
                    HorizontalDivider(color = LocalAppearance.current.muted.copy(alpha = 0.16f))
                }
                Text("Сейчас: $privacyLastSeen", color = LocalAppearance.current.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                HorizontalDivider(color = LocalAppearance.current.muted.copy(alpha = 0.16f), modifier = Modifier.padding(vertical = 14.dp))
                PrivacyChoiceSection("Кто видит мой номер телефона", privacyPhone, onPrivacyPhone)
                HorizontalDivider(color = LocalAppearance.current.muted.copy(alpha = 0.16f), modifier = Modifier.padding(vertical = 14.dp))
                PrivacyChoiceSection("Кто видит мою фотографию профиля", privacyPhoto, onPrivacyPhoto)
                Text("Если в Telegram настроены исключения для отдельных людей, они будут обозначены как «Особые настройки». Выбор выше заменит такие исключения общим правилом.", color = LocalAppearance.current.muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp))
            } else if (page == "account") {
                var firstName by remember(account?.firstName) { mutableStateOf(account?.firstName.orEmpty()) }
                var lastName by remember(account?.lastName) { mutableStateOf(account?.lastName.orEmpty()) }
                var bio by remember(account?.bio) { mutableStateOf(account?.bio.orEmpty()) }
                Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(path = account?.avatarPath ?: "", fallback = (account?.name ?: "Я").take(1), id = 0, size = 76)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account?.name ?: "Мой аккаунт", color = LocalAppearance.current.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (account?.username.isNullOrBlank()) "Имя пользователя не задано" else "@${account?.username}", color = LocalAppearance.current.muted, fontSize = 13.sp)
                        if (account?.isPremium == true) Text("✦ Telegram Premium", color = Color(0xFFB58CFF), fontSize = 12.sp)
                    }
                }
                OutlinedButton(onClick = onPickProfilePhoto, modifier = Modifier.fillMaxWidth()) { Text("Изменить фотографию профиля") }
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Имя") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Фамилия") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(value = bio, onValueChange = { bio = it.take(70).replace("\n", "") }, label = { Text("О себе") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 2, maxLines = 3)
                Button(onClick = { onSaveMyProfile(firstName, lastName, bio) }, enabled = firstName.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Сохранить изменения") }
                Text("Телефон: ${account?.phone?.ifBlank { "не указан" } ?: "Загружаем…"}", color = LocalAppearance.current.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
                Text("Изменения отправляются непосредственно в Telegram.", color = LocalAppearance.current.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
            } else if (page == "notifications") {
                Text("Уведомления Specialgram", color = LocalAppearance.current.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
                Text("Управление разрешением, звуком и отображением уведомлений выполняется Android. Уведомления чатов синхронизируются с Telegram.", color = LocalAppearance.current.muted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp, bottom = 14.dp))
                SettingsNavigationRow("Системные настройки уведомлений", "Разрешения, звук, экран блокировки") {
                    runCatching {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        context.startActivity(intent)
                    }
                }
                Text("Если уведомления отключены в Android, включи их для Specialgram. Отдельные настройки чатов остаются на стороне Telegram.", color = LocalAppearance.current.muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp))
            } else if (page == "diagnostics") {
                val journal = remember { DiagnosticsJournal.read(context) }
                Text("Журнал ошибок", color = LocalAppearance.current.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
                Text("Здесь сохраняются технические ошибки TDLib: название запроса, код и время. Переписки, имена, номера телефонов и идентификаторы чатов не записываются.", color = LocalAppearance.current.muted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                Text(if (journal.isBlank()) "Ошибок пока нет. Если появится Too Many Requests, запись появится здесь." else journal, color = LocalAppearance.current.text, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth().background(LocalAppearance.current.panel, RoundedCornerShape(12.dp)).padding(12.dp))
                OutlinedButton(onClick = {
                    val value = DiagnosticsJournal.read(context)
                    if (value.isBlank()) android.widget.Toast.makeText(context, "Журнал пока пуст", android.widget.Toast.LENGTH_SHORT).show()
                    else {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Specialgram diagnostics", value))
                        android.widget.Toast.makeText(context, "Журнал скопирован", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Скопировать журнал") }
                Button(onClick = {
                    val value = DiagnosticsJournal.read(context)
                    if (value.isBlank()) android.widget.Toast.makeText(context, "Журнал пока пуст", android.widget.Toast.LENGTH_SHORT).show()
                    else {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_SUBJECT, "Specialgram — журнал диагностики").putExtra(android.content.Intent.EXTRA_TEXT, value)
                        context.startActivity(android.content.Intent.createChooser(intent, "Экспортировать журнал"))
                    }
                }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Экспортировать журнал") }
                Text("Журнал хранится только в памяти приложения. Перед отправкой можешь просмотреть его содержимое.", color = LocalAppearance.current.muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 10.dp, bottom = 18.dp))
            } else if (page == "storage") {
                Text("Данные и память", color = LocalAppearance.current.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
                Text("Локальное хранилище TDLib и временные файлы приложения.", color = LocalAppearance.current.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
                SettingsLine("Медиа и файлы Telegram", formatFileSize(tdlibMediaBytes))
                SettingsLine("Временные файлы", formatFileSize(cacheBytes))
                SettingsNavigationRow("Открыть хранилище Android", "Просмотр размера и очистка данных приложения") {
                    runCatching {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
                        context.startActivity(intent)
                    }
                }
                Text("Очистка данных Android удалит локальную базу и может потребовать повторного входа. Не очищай данные приложения, если не хочешь заново авторизоваться.", color = LocalAppearance.current.muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 10.dp))
            } else {
                Text("О приложении", color = LocalAppearance.current.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
                Text("Specialgram — клиент Telegram на базе TDLib.\n\nЗащищённая диагностика: приложение отправляет технические отчёты о сбоях и ошибках TDLib. Тексты сообщений, переписки, контакты и номер телефона в отчёты не включаются.", color = LocalAppearance.current.muted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
    if (operationError != null) AlertDialog(
        onDismissRequest = onDismissOperationError,
        title = { Text("Не удалось выполнить действие", color = LocalAppearance.current.text) },
        text = { Text(operationError, color = LocalAppearance.current.muted) },
        confirmButton = { TextButton(onClick = onDismissOperationError) { Text("Понятно") } }
    )
}

@Composable
private fun PrivacyChoiceSection(title:String,current:String,onSelect:(String)->Unit) {
    Text(title, color = LocalAppearance.current.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp))
    listOf("Все", "Мои контакты", "Никто").forEach { choice ->
        Row(Modifier.fillMaxWidth().clickable { onSelect(choice) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(choice, color = LocalAppearance.current.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            RadioButton(selected = current == choice, onClick = { onSelect(choice) })
        }
    }
    Text("Текущее значение: $current", color = LocalAppearance.current.muted, fontSize = 11.sp)
}

@Composable
private fun SettingsNavigationRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = LocalAppearance.current.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = LocalAppearance.current.muted, fontSize = 12.sp)
        }
        Text("›", color = LocalAppearance.current.muted, fontSize = 25.sp)
    }
    HorizontalDivider(color = LocalAppearance.current.muted.copy(alpha = 0.16f))
}

@Composable
private fun CoverChoice(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = LocalAppearance.current.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = LocalAppearance.current.muted, fontSize = 12.sp)
        }
        RadioButton(selected = selected, onClick = onClick)
    }
    HorizontalDivider(color = LocalAppearance.current.muted.copy(alpha = 0.16f))
}
@Composable
private fun SettingsLine(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
        Text(title, color = LocalAppearance.current.text, fontSize = 15.sp)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = LocalAppearance.current.muted, fontSize = 12.sp)
    }
}

@Composable
private fun ChatRow(chat: TgChat, compact: Boolean = false, onArchive: () -> Unit, onTogglePinned: () -> Unit, onToggleUnread: () -> Unit, onToggleMute: () -> Unit, onClick: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val initials = chat.title.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString("").uppercase()
    val time = remember(chat.lastDate) {
        if (chat.lastDate <= 0) "" else try {
            val d = Date(chat.lastDate.toLong() * 1000L)
            val today = Calendar.getInstance()
            val messageDay = Calendar.getInstance().apply { time = d }
            val sameDay = today.get(Calendar.YEAR) == messageDay.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == messageDay.get(Calendar.DAY_OF_YEAR)
            val daysAgo = ((today.timeInMillis - messageDay.timeInMillis) / 86_400_000L).toInt()
            when {
                sameDay -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(d)
                daysAgo == 1 -> "вчера"
                daysAgo in 2..6 -> SimpleDateFormat("EEE", Locale.getDefault()).format(d)
                else -> SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(d)
            }
        } catch (_: Exception) { "" }
    }
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { menu = true }).padding(horizontal = 16.dp, vertical = if (compact) 5.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(path = chat.avatarPath, fallback = initials.ifBlank { "T" }, id = chat.id, size = 54, online = chat.isOnline, hasStory = chat.hasStory, storyViewed = !chat.hasUnviewedStory)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.title, color = LocalAppearance.current.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (chat.isPremium) Text("✦", color = Color(0xFFB58CFF), fontSize = 13.sp)
                if (chat.isPinned) Text("📌", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 3.dp))
                if (chat.isMuted) Text("🔕", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 2.dp))
                if (time.isNotBlank()) Text(time, color = if (chat.unreadCount > 0 || chat.isMarkedAsUnread) LocalAppearance.current.accent else LocalAppearance.current.muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.preview.ifBlank { chat.status.ifBlank { "Нет сообщений" } }, color = LocalAppearance.current.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (chat.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.defaultMinSize(minWidth = 21.dp, minHeight = 21.dp).clip(CircleShape).background(LocalAppearance.current.accent).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                        Text(if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(if (chat.isPinned) "Открепить чат" else "Закрепить чат") }, onClick = { menu = false; onTogglePinned() })
            DropdownMenuItem(text = { Text(if (chat.isMarkedAsUnread) "Отметить прочитанным" else "Отметить непрочитанным") }, onClick = { menu = false; onToggleUnread() })
            DropdownMenuItem(text = { Text(if (chat.isMuted) "Включить уведомления" else "Отключить уведомления") }, onClick = { menu = false; onToggleMute() })
            HorizontalDivider()
            DropdownMenuItem(text = { Text(if (chat.archived) "Вернуть из архива" else "В архив") }, onClick = { menu = false; onArchive() })
        }
    }
}

@Composable
private fun Avatar(path: String, fallback: String, id: Long, size: Int, online: Boolean = false, hasStory: Boolean = false, storyViewed: Boolean = false) {
    val panelColor = LocalAppearance.current.panel.copy(alpha = 1f - LocalAppearance.current.glass * 0.30f).toArgb()
    AndroidView(
        modifier = Modifier.size(size.dp),
        factory = { context ->
            NativeAvatarView(context, path, fallback, id, size, online, hasStory, storyViewed, panelColor)
        },
        update = { view ->
            view.update(path, fallback, id, size, online, hasStory, storyViewed, panelColor)
        }
    )
}

@Composable
private fun ConversationScreen(
    chat: TgChat, chats: List<TgChat>, messages: List<TgMessage>, searchResults: List<TgMessage>, onSearchMessages: (String) -> Unit, profile: TgProfile?, onOpenProfile: () -> Unit, onCloseProfile: () -> Unit, onBack: () -> Unit,
    onSend: (String, Boolean, Long?) -> Unit,
    onDeleteMessage: (TgMessage, Boolean) -> Unit,
    onClearHistory: (Boolean) -> Unit,
    onEditMessage: (TgMessage, String) -> Unit,
    onReactToMessage: (TgMessage, String) -> Unit,
    onForwardMessage: (TgMessage, TgChat) -> Unit,
    onPinMessage: (TgMessage) -> Unit,
    videoNoteShape: String, onVideoNoteShape: (String) -> Unit, onRecordVideo: () -> Unit,
    operationError: String?, onDismissOperationError: () -> Unit,
    onLoadOlderMessages: () -> Unit,
    onSendPhoto: () -> Unit,
    onSendDocument: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var animatedMode by remember { mutableStateOf(true) }
    var showProfile by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<TgMessage?>(null) }
    var deleteForBoth by remember { mutableStateOf(false) }
    var chatMenuOpen by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var clearForBoth by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<TgMessage?>(null) }
    var editDraft by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<TgMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<TgMessage?>(null) }
    var showVideoShapePicker by remember { mutableStateOf(false) }
    var attachmentMenuOpen by remember { mutableStateOf(false) }
    var messageSearchOpen by remember { mutableStateOf(false) }
    var messageSearchQuery by remember { mutableStateOf("") }
    var messageSearchIndex by remember { mutableIntStateOf(0) }
    val orderedMessages = remember(messages) { messages.sortedBy { it.date } }
    val localSearchMatches = remember(orderedMessages, messageSearchQuery) {
        if (messageSearchQuery.isBlank()) emptyList() else orderedMessages.filter { it.text.contains(messageSearchQuery.trim(), ignoreCase = true) }
    }
    val visibleMessages = if (messageSearchOpen && messageSearchQuery.isNotBlank()) {
        if (chat.kind == "Секретный чат") localSearchMatches else searchResults
    } else orderedMessages
    LaunchedEffect(messageSearchQuery, messageSearchOpen, chat.id) {
        if (messageSearchOpen && messageSearchQuery.isNotBlank() && chat.kind != "Секретный чат") {
            onSearchMessages("")
            delay(350)
            onSearchMessages(messageSearchQuery.trim())
        } else onSearchMessages("")
    }
    val listState = rememberLazyListState()
    var hasScrolledAwayFromBottom by remember(chat.id) { mutableStateOf(false) }
    LaunchedEffect(chat.id, listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { firstVisible ->
            if (firstVisible > 2) hasScrolledAwayFromBottom = true
            else if (hasScrolledAwayFromBottom && !messageSearchOpen) {
                hasScrolledAwayFromBottom = false
                onLoadOlderMessages()
            }
        }
    }
    val scope = rememberCoroutineScope()
    LaunchedEffect(orderedMessages.lastOrNull()?.id, messageSearchOpen, messageSearchQuery) {
        if (!messageSearchOpen && orderedMessages.isNotEmpty()) listState.animateScrollToItem(orderedMessages.lastIndex)
    }
    LaunchedEffect(visibleMessages, messageSearchIndex, messageSearchOpen) {
        if (messageSearchOpen && visibleMessages.isNotEmpty()) {
            messageSearchIndex = messageSearchIndex.coerceIn(0, visibleMessages.lastIndex)
            listState.animateScrollToItem(messageSearchIndex)
        }
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding().background(LocalAppearance.current.chatBackground)) {
        if (showVideoShapePicker) AlertDialog(
            onDismissRequest = { showVideoShapePicker = false },
            title = { Text("Форма видеосообщения", color = LocalAppearance.current.text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("В обычный Telegram придёт обычное видео. Выбранная форма (круг, сердце, звезда и другие) будет отображаться у пользователей Specialgram.", color = LocalAppearance.current.muted, fontSize = 12.sp)
                    listOf("circle" to "Круг", "square" to "Квадрат", "rounded" to "Скруглённый квадрат", "heart" to "Сердце", "star" to "Звезда", "hexagon" to "Шестиугольник").forEach { (key, title) ->
                        Row(Modifier.fillMaxWidth().clickable { onVideoNoteShape(key); showVideoShapePicker = false }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = videoNoteShape == key, onClick = { onVideoNoteShape(key); showVideoShapePicker = false })
                            Text(title, color = LocalAppearance.current.text)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVideoShapePicker = false }) { Text("Готово") } }
        )
        if (operationError != null) AlertDialog(
            onDismissRequest = onDismissOperationError,
            title = { Text("Не удалось выполнить действие", color = LocalAppearance.current.text) },
            text = { Text(operationError, color = LocalAppearance.current.muted) },
            confirmButton = { TextButton(onClick = onDismissOperationError) { Text("Понятно") } }
        )
        selectedMessage?.let { target ->
            AlertDialog(
                onDismissRequest = { selectedMessage = null },
                title = { Text("Сообщение", color = LocalAppearance.current.text) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (target.outgoing) Text("Выберите действие для своего сообщения", color = LocalAppearance.current.muted)
                        else Text("Выберите, удалить сообщение только у себя или у всех участников чата.", color = LocalAppearance.current.muted)
                        Text("Быстрая реакция", color = LocalAppearance.current.muted, fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("👍", "❤️", "😂", "😮", "😢", "🔥").forEach { emoji ->
                                TextButton(onClick = { onReactToMessage(target, emoji); selectedMessage = null }) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = deleteForBoth, onCheckedChange = { deleteForBoth = it })
                            Text("Удалить у всех", color = LocalAppearance.current.text, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        TextButton(onClick = { replyingTo = target; selectedMessage = null; deleteForBoth = false }) { Text("Ответить") }
                        TextButton(onClick = { forwardingMessage = target; selectedMessage = null; deleteForBoth = false }) { Text("Переслать") }
                        TextButton(onClick = { onPinMessage(target); selectedMessage = null; deleteForBoth = false }) { Text(if (target.isPinned) "Открепить" else "Закрепить") }
                        if (target.outgoing) {
                            TextButton(onClick = { editingMessage = target; editDraft = target.text; selectedMessage = null }) { Text("Изменить") }
                        }
                        TextButton(onClick = { onDeleteMessage(target, deleteForBoth); selectedMessage = null; deleteForBoth = false }) { Text("Удалить", color = Color(0xFFFF6B6B)) }
                        TextButton(onClick = { selectedMessage = null; deleteForBoth = false }) { Text("Отмена") }
                    }
                }
            )
        }
        forwardingMessage?.let { target ->
            AlertDialog(
                onDismissRequest = { forwardingMessage = null },
                title = { Text("Переслать сообщение", color = LocalAppearance.current.text) },
                text = {
                    Column(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        Text(target.text, color = LocalAppearance.current.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
                            items(chats.filter { it.id != chat.id }, key = { it.id }) { destination ->
                                Row(Modifier.fillMaxWidth().clickable { onForwardMessage(target, destination); forwardingMessage = null }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Avatar(path = destination.avatarPath, fallback = destination.title.take(1).uppercase(), id = destination.id, size = 38)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(destination.title, color = LocalAppearance.current.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(destination.kind, color = LocalAppearance.current.muted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { forwardingMessage = null }) { Text("Отмена") } }
            )
        }
        editingMessage?.let { target ->
            AlertDialog(
                onDismissRequest = { editingMessage = null },
                title = { Text("Изменить сообщение", color = LocalAppearance.current.text) },
                text = { OutlinedTextField(value = editDraft, onValueChange = { editDraft = it }, modifier = Modifier.fillMaxWidth(), maxLines = 5) },
                confirmButton = {
                    TextButton(onClick = { onEditMessage(target, editDraft); editingMessage = null }) { Text("Сохранить") }
                },
                dismissButton = { TextButton(onClick = { editingMessage = null }) { Text("Отмена") } }
            )
        }
        if (showClearHistoryDialog) AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false; clearForBoth = false },
            title = { Text("Очистить переписку?", color = LocalAppearance.current.text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("История будет удалена у вас. Отметьте галочку, чтобы удалить её у всех участников чата.", color = LocalAppearance.current.muted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearForBoth, onCheckedChange = { clearForBoth = it })
                        Text("Удалить у всех", color = LocalAppearance.current.text, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onClearHistory(clearForBoth); showClearHistoryDialog = false; clearForBoth = false }) {
                    Text("Очистить", color = Color(0xFFFF6B6B))
                }
            },
            dismissButton = { TextButton(onClick = { showClearHistoryDialog = false; clearForBoth = false }) { Text("Отмена") } }
        )
        if (showProfile) AlertDialog(
            onDismissRequest = { showProfile = false; onCloseProfile() },
            title = { Text(profile?.name ?: chat.title, color = LocalAppearance.current.text) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(path = profile?.avatarPath ?: chat.avatarPath, fallback = (profile?.name ?: chat.title).take(1).uppercase(), id = chat.id, size = 112, online = profile?.isOnline == true)
                    Text(profile?.status ?: "Загружаем статус…", color = if (profile?.isOnline == true) Color(0xFF35C759) else LocalAppearance.current.muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (profile?.isVerified == true) Text("✓ Подтверждённый аккаунт", color = LocalAppearance.current.accent, fontSize = 12.sp)
                    if (profile?.isPremium == true) Text("✦ Telegram Premium", color = Color(0xFFB58CFF), fontWeight = FontWeight.SemiBold)
                    if (!profile?.username.isNullOrBlank()) Text("@${profile?.username}", color = LocalAppearance.current.accent)
                    Text(profile?.kind ?: chat.kind, color = LocalAppearance.current.muted)
                    if (!profile?.bio.isNullOrBlank()) Text(profile?.bio ?: "", color = LocalAppearance.current.text)
                    if (!profile?.phone.isNullOrBlank()) ProfileInfoLine("Телефон", profile?.phone ?: "")
                    if ((profile?.commonGroups ?: 0) > 0) ProfileInfoLine("Общие группы", (profile?.commonGroups ?: 0).toString())
                    ProfileInfoLine("ID", chat.id.toString())
                }
            },
            confirmButton = { TextButton(onClick = { showProfile = false; onCloseProfile() }) { Text("Закрыть") } }
        )
        Row(
            Modifier.fillMaxWidth().background(LocalAppearance.current.panel.copy(alpha = 1f - LocalAppearance.current.glass * 0.30f)).clickable { showProfile = true; onOpenProfile() }.padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Text("‹", color = LocalAppearance.current.accent, fontSize = 32.sp) }
            Avatar(path = profile?.avatarPath ?: chat.avatarPath, fallback = (profile?.name ?: chat.title).take(1).uppercase(), id = chat.id, size = 44, online = profile?.isOnline == true)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(profile?.name?.takeIf { it.isNotBlank() } ?: chat.title, color = LocalAppearance.current.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(profile?.status?.takeIf { it.isNotBlank() } ?: chat.status.ifBlank { " " }, color = if (profile?.isOnline == true) Color(0xFF35C759) else LocalAppearance.current.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                IconButton(onClick = { chatMenuOpen = true }) { Text("⋮", color = LocalAppearance.current.text, fontSize = 24.sp) }
                DropdownMenu(expanded = chatMenuOpen, onDismissRequest = { chatMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Поиск") },
                        onClick = { chatMenuOpen = false; messageSearchOpen = true; messageSearchQuery = ""; messageSearchIndex = 0 }
                    )
                    DropdownMenuItem(
                        text = { Text("Очистить переписку") },
                        onClick = { chatMenuOpen = false; clearForBoth = false; showClearHistoryDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text(if (animatedMode) "✓ Постепенная отправка" else "Постепенная отправка") },
                        onClick = { animatedMode = !animatedMode; chatMenuOpen = false }
                    )
                }
            }
        }

        if (messageSearchOpen) {
            Row(Modifier.fillMaxWidth().background(LocalAppearance.current.panel).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = messageSearchQuery, onValueChange = { messageSearchQuery = it; messageSearchIndex = 0 },
                    modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Поиск в переписке") },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = LocalAppearance.current.text, unfocusedTextColor = LocalAppearance.current.text, cursorColor = LocalAppearance.current.accent)
                )
                Text(if (messageSearchQuery.isBlank()) " " else "${if (visibleMessages.isEmpty()) 0 else messageSearchIndex + 1}/${visibleMessages.size}", color = LocalAppearance.current.muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp))
                IconButton(enabled = visibleMessages.isNotEmpty(), onClick = { messageSearchIndex = if (messageSearchIndex <= 0) visibleMessages.lastIndex else messageSearchIndex - 1 }) { Text("↑", color = LocalAppearance.current.text) }
                IconButton(enabled = visibleMessages.isNotEmpty(), onClick = { messageSearchIndex = (messageSearchIndex + 1) % visibleMessages.size }) { Text("↓", color = LocalAppearance.current.text) }
                IconButton(onClick = { messageSearchOpen = false; messageSearchQuery = ""; messageSearchIndex = 0 }) { Text("✕", color = LocalAppearance.current.text) }
            }
        }
        if (visibleMessages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (messageSearchOpen && messageSearchQuery.isNotBlank()) "⌕" else "✉", color = LocalAppearance.current.muted, fontSize = 34.sp)
                    Text(if (messageSearchOpen && messageSearchQuery.isNotBlank()) "Ничего не найдено" else "Пока нет сообщений", color = LocalAppearance.current.muted, fontSize = 14.sp)
                    Text(if (messageSearchOpen) if (chat.kind == "Секретный чат") "Поиск по загруженным сообщениям" else "Поиск по истории Telegram" else "Отправьте первое сообщение", color = LocalAppearance.current.muted.copy(alpha = 0.72f), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(visibleMessages, key = { _, message -> message.id }) { index, message ->
                    val previous = visibleMessages.getOrNull(index - 1)
                    val next = visibleMessages.getOrNull(index + 1)
                    val startsDay = !messageSearchOpen && (previous == null || !isSameMessageDay(previous.date, message.date))
                    val continuesGroup = previous != null && previous.outgoing == message.outgoing &&
                        isSameMessageDay(previous.date, message.date) && message.date - previous.date < 300
                    val continuesAfter = next != null && next.outgoing == message.outgoing &&
                        isSameMessageDay(next.date, message.date) && next.date - message.date < 300
                    if (startsDay) MessageDateDivider(message.date)
                    Spacer(Modifier.height(if (continuesGroup) 2.dp else 6.dp))
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 })) {
                        MessageBubble(
                            message,
                            onLongPress = { selectedMessage = message },
                            onReaction = { emoji -> onReactToMessage(message, emoji) },
                            groupedWithPrevious = continuesGroup,
                            groupedWithNext = continuesAfter
                        )
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(LocalAppearance.current.panel.copy(alpha = 1f - LocalAppearance.current.glass * 0.30f)).padding(horizontal = 10.dp, vertical = 7.dp)) {
            if (replyingTo != null) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(LocalAppearance.current.panelLight).padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (replyingTo!!.outgoing) "Ответ на ваше сообщение" else "Ответ на сообщение ${chat.title}", color = LocalAppearance.current.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(replyingTo!!.text, color = LocalAppearance.current.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { replyingTo = null }) { Text("✕", color = LocalAppearance.current.muted) }
                }
                Spacer(Modifier.height(5.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Box {
                    IconButton(onClick = { attachmentMenuOpen = true }) { Text("＋", color = LocalAppearance.current.accent, fontSize = 27.sp) }
                    DropdownMenu(expanded = attachmentMenuOpen, onDismissRequest = { attachmentMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("Фото или изображение") }, onClick = { attachmentMenuOpen = false; onSendPhoto() })
                        DropdownMenuItem(text = { Text("Файл или документ") }, onClick = { attachmentMenuOpen = false; onSendDocument() })
                    }
                }
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение", color = LocalAppearance.current.muted) }, maxLines = 4,
                    shape = RoundedCornerShape(23.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LocalAppearance.current.panelLight.copy(alpha = 1f - LocalAppearance.current.glass * 0.38f), unfocusedContainerColor = LocalAppearance.current.panelLight.copy(alpha = 1f - LocalAppearance.current.glass * 0.38f),
                        focusedBorderColor = LocalAppearance.current.accent, unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = LocalAppearance.current.text, unfocusedTextColor = LocalAppearance.current.text, cursorColor = LocalAppearance.current.accent
                    )
                )
                Spacer(Modifier.width(5.dp))
                Surface(
                    modifier = Modifier.size(49.dp).clip(CircleShape).combinedClickable(onClick = onRecordVideo, onLongClick = { showVideoShapePicker = true }),
                    color = LocalAppearance.current.panelLight.copy(alpha = 1f - LocalAppearance.current.glass * 0.38f), shape = CircleShape
                ) { Box(contentAlignment = Alignment.Center) { Text("◎", color = LocalAppearance.current.accent, fontSize = 25.sp) } }
                Spacer(Modifier.width(5.dp))
                val sendScale by animateFloatAsState(if (draft.isBlank()) 0.92f else 1f, label = "send-button")
                Surface(
                    modifier = Modifier.size(49.dp).clip(CircleShape).clickable(enabled = draft.isNotBlank()) {
                        val value = draft.trim()
                        draft = ""
                        val replyId = replyingTo?.id
                        replyingTo = null
                        onSend(value, animatedMode, replyId)
                    },
                    color = if (draft.isBlank()) LocalAppearance.current.panelLight.copy(alpha = 1f - LocalAppearance.current.glass * 0.38f) else LocalAppearance.current.accent,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("➤", color = Color.White, fontSize = (21f * sendScale).sp) }
                }
            }
        }
    }
}

@Composable
private fun StoryViewer(story: TgStory, onDismiss: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, storyPosition: Int, storyCount: Int, onReact: () -> Unit, onReply: (String) -> Unit) {
    var replyText by remember(story.chatId, story.storyId) { mutableStateOf("") }

    val bitmap = remember(story.path, story.isVideo) {
        if (!story.isVideo && story.path.isNotBlank() && story.path != "unsupported") runCatching { BitmapFactory.decodeFile(story.path)?.asImageBitmap() }.getOrNull() else null
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth().height(620.dp), color = Color(0xFF090B12), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(story.ownerName, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("$storyPosition/$storyCount", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        TextButton(onClick = onDismiss) { Text("Закрыть", color = Color.White) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(storyCount.coerceAtLeast(1)) { index ->
                            Box(
                                Modifier.weight(1f).height(3.dp).clip(CircleShape)
                                    .background(if (index < storyPosition) Color.White else Color.White.copy(alpha = 0.22f))
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (story.path.isBlank()) CircularProgressIndicator(color = Color.White)
                    else if (story.path == "unsupported") Text("Этот тип истории пока не поддерживается", color = Color.White)
                    else if (story.isVideo) {
                        AndroidView(factory = { ctx -> VideoView(ctx).apply {
                            setVideoPath(story.path)
                            setOnPreparedListener { player -> player.isLooping = true; start() }
                        } }, modifier = Modifier.fillMaxSize())
                    } else if (bitmap != null) {
                        Image(bitmap = bitmap, contentDescription = "История", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    } else Text("Не удалось загрузить историю", color = Color.White)
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxHeight().clickable { onPrevious() })
                        Box(Modifier.weight(1f).fillMaxHeight().clickable { onNext() })
                    }
                }
                if (story.caption.isNotBlank()) Text(story.caption, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onPrevious, enabled = storyPosition > 1) { Text("Назад", color = Color.White) }
                    TextButton(onClick = onReact) { Text("❤️", fontSize = 22.sp) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onNext) { Text("Далее", color = Color.White) }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ответить на историю…", color = Color.LightGray) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF42C7FF), unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                            cursorColor = Color(0xFF42C7FF)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            val value = replyText.trim()
                            if (value.isNotEmpty()) { onReply(value); replyText = "" }
                        })
                    )
                    TextButton(
                        onClick = { val value = replyText.trim(); if (value.isNotEmpty()) { onReply(value); replyText = "" } },
                        enabled = replyText.isNotBlank()
                    ) { Text("Отпр.", color = Color(0xFF42C7FF)) }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = LocalAppearance.current.muted, fontSize = 12.sp)
        Text(value, color = LocalAppearance.current.text, fontSize = 12.sp)
    }
}

private fun isSameMessageDay(first: Int, second: Int): Boolean {
    if (first <= 0 || second <= 0) return false
    val a = Calendar.getInstance().apply { timeInMillis = first.toLong() * 1000L }
    val b = Calendar.getInstance().apply { timeInMillis = second.toLong() * 1000L }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun MessageDateDivider(timestamp: Int) {
    val label = remember(timestamp) {
        val date = Date(timestamp.toLong() * 1000L)
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        when {
            today.get(Calendar.YEAR) == target.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> "Сегодня"
            yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) && yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> "Вчера"
            else -> SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(date)
        }
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
        Surface(color = LocalAppearance.current.panel.copy(alpha = 0.88f), shape = RoundedCornerShape(20.dp)) {
            Text(label, color = LocalAppearance.current.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
        }
    }
}

@Composable
private fun MessageBubble(
    message: TgMessage,
    onLongPress: () -> Unit,
    onReaction: (String) -> Unit,
    groupedWithPrevious: Boolean = false,
    groupedWithNext: Boolean = false
) {
    val time = remember(message.date) {
        try { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.date.toLong() * 1000L)) }
        catch (_: Exception) { "" }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 310.dp).combinedClickable(onClick = {}, onLongClick = onLongPress).clip(
                RoundedCornerShape(
                    topStart = if (!message.outgoing && groupedWithPrevious) 4.dp else LocalAppearance.current.bubbleRadius.dp,
                    topEnd = if (message.outgoing && groupedWithPrevious) 4.dp else LocalAppearance.current.bubbleRadius.dp,
                    bottomStart = if (!message.outgoing && groupedWithNext) 4.dp else LocalAppearance.current.bubbleRadius.dp,
                    bottomEnd = if (message.outgoing && groupedWithNext) 4.dp else LocalAppearance.current.bubbleRadius.dp
                )
            ).background(if (message.outgoing) LocalAppearance.current.outgoing else LocalAppearance.current.incoming).padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
            if (message.replyToMessageId != 0L) {
                Text("↪ ${message.replyToText.ifBlank { "Сообщение" }}", color = LocalAppearance.current.accent, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val photoBitmap = remember(message.photoPath) {
                if (message.photoPath.isNotBlank()) runCatching { BitmapFactory.decodeFile(message.photoPath)?.asImageBitmap() }.getOrNull() else null
            }
            val context = androidx.compose.ui.platform.LocalContext.current
            if (photoBitmap != null) {
                Image(bitmap = photoBitmap, contentDescription = "Фото", modifier = Modifier.width(240.dp).heightIn(max = 300.dp).clip(RoundedCornerShape(12.dp)).clickable { openDownloadedMedia(context, message.mediaPath.ifBlank { message.photoPath }, "image/*") }, contentScale = ContentScale.Crop)
                if (message.text.isNotBlank() && message.text != "📷 Фото") {
                    Spacer(Modifier.height(4.dp))
                    Text(message.text, color = LocalAppearance.current.text, fontSize = 15.sp, lineHeight = 20.sp)
                }
            } else if (message.mediaKind == "sticker" && message.mediaPath.isNotBlank()) {
                val stickerBitmap = remember(message.mediaPath) { runCatching { BitmapFactory.decodeFile(message.mediaPath)?.asImageBitmap() }.getOrNull() }
                if (stickerBitmap != null) Image(stickerBitmap, contentDescription = "Стикер", modifier = Modifier.size(156.dp), contentScale = ContentScale.Fit)
                else Text("Стикер загружается…", color = LocalAppearance.current.muted, fontSize = 13.sp)
            } else if (message.mediaKind == "document") {
                Row(Modifier.widthIn(min = 190.dp, max = 270.dp).clip(RoundedCornerShape(10.dp)).clickable { openDownloadedMedia(context, message.mediaPath, message.mediaMimeType) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📎", fontSize = 27.sp); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(message.mediaName.ifBlank { "Файл" }, color = LocalAppearance.current.text, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${formatFileSize(message.mediaSize)} · ${if (message.mediaPath.isBlank()) "Загрузка…" else "Открыть файл"}", color = LocalAppearance.current.muted, fontSize = 11.sp)
                    }
                }
                if (message.text.isNotBlank() && !message.text.startsWith("📎")) Text(message.text, color = LocalAppearance.current.text, fontSize = 14.sp)
            } else if (message.mediaKind == "audio" || message.mediaKind == "voice") {
                AudioMessagePlayer(message.mediaPath, message.mediaName.ifBlank { message.text }, message.mediaDuration)
            } else if (message.mediaKind == "animation" && message.mediaPath.isNotBlank()) {
                AndroidView(factory = { viewContext -> VideoView(viewContext).apply { setVideoPath(message.mediaPath); setOnPreparedListener { player -> player.isLooping = true; start() } } }, modifier = Modifier.width(230.dp).height(230.dp).clip(RoundedCornerShape(12.dp)))
                if (message.text.isNotBlank() && message.text != "GIF") Text(message.text, color = LocalAppearance.current.text, fontSize = 14.sp)
            } else if (message.videoPath.isNotBlank()) {
                AndroidView(
                    factory = { viewContext -> VideoView(viewContext).apply {
                        setVideoPath(message.videoPath)
                        setOnPreparedListener { player -> player.isLooping = true; start() }
                    } },
                    modifier = Modifier.width(224.dp).height(224.dp).clip(messageVideoShape(message.videoShape))
                )
                Text(message.text, color = LocalAppearance.current.text, fontSize = 12.sp, lineHeight = 16.sp)
            } else Text(message.text, color = LocalAppearance.current.text, fontSize = 16.sp, lineHeight = 21.sp)
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                if (message.isPinned) Text("📌 ", color = LocalAppearance.current.muted, fontSize = 10.sp)
                Text(time, color = LocalAppearance.current.muted.copy(alpha = 0.88f), fontSize = 11.sp)
                if (message.outgoing) Text(if (message.isRead) "  ✓✓" else "  ✓", color = if (message.isRead) LocalAppearance.current.accent else LocalAppearance.current.muted, fontSize = 11.sp)
            }
            if (message.reactions.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    message.reactions.forEach { reaction ->
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).background(if (reaction.chosen) LocalAppearance.current.accent.copy(alpha = 0.28f) else LocalAppearance.current.panelLight.copy(alpha = 0.8f)).clickable { onReaction(reaction.emoji) }.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(reaction.emoji, fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(reaction.count.toString(), color = if (reaction.chosen) LocalAppearance.current.accent else LocalAppearance.current.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private fun messageVideoShape(name:String): Shape = when(name) {
    "circle" -> CircleShape
    "square" -> RectangleShape
    "rounded" -> RoundedCornerShape(22.dp)
    else -> object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val w=size.width; val h=size.height; val path=Path()
            when(name) {
                "heart" -> {
                    path.moveTo(w*0.5f,h*0.92f)
                    path.cubicTo(w*0.08f,h*0.62f,-w*0.04f,h*0.30f,w*0.22f,h*0.16f)
                    path.cubicTo(w*0.38f,h*0.06f,w*0.5f,h*0.22f,w*0.5f,h*0.29f)
                    path.cubicTo(w*0.5f,h*0.22f,w*0.62f,h*0.06f,w*0.78f,h*0.16f)
                    path.cubicTo(w*1.04f,h*0.30f,w*0.92f,h*0.62f,w*0.5f,h*0.92f)
                }
                "star" -> for(i in 0 until 10) {
                    val radius=if(i%2==0) minOf(w,h)*0.49f else minOf(w,h)*0.22f
                    val angle=(-Math.PI/2.0)+(Math.PI/5.0*i)
                    val x=w/2f+(kotlin.math.cos(angle)*radius).toFloat(); val y=h/2f+(kotlin.math.sin(angle)*radius).toFloat()
                    if(i==0) path.moveTo(x,y) else path.lineTo(x,y)
                }
                else -> for(i in 0 until 6) {
                    val angle=(-Math.PI/2.0)+(Math.PI/3.0*i); val radius=minOf(w,h)*0.49f
                    val x=w/2f+(kotlin.math.cos(angle)*radius).toFloat(); val y=h/2f+(kotlin.math.sin(angle)*radius).toFloat()
                    if(i==0) path.moveTo(x,y) else path.lineTo(x,y)
                }
            }
            path.close(); return Outline.Generic(path)
        }
    }
}

@Composable
private fun AuthScreen(
    title: String, subtitle: String, value: String, onValue: (String) -> Unit,
    button: String, onSubmit: () -> Unit, secret: Boolean = false
) {
    val appearance = LocalAppearance.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            NativeEntryPanel(context, title, subtitle, value, button, secret,
                appearance.background.toArgb(), appearance.text.toArgb(),
                appearance.muted.toArgb(), appearance.accent.toArgb(), onValue, onSubmit)
        },
        update = { panel ->
            panel.update(title, subtitle, value, button, secret,
                appearance.background.toArgb(), appearance.text.toArgb(),
                appearance.muted.toArgb(), appearance.accent.toArgb(), onValue, onSubmit)
        }
    )
}

@Composable
private fun CenterPanel(title: String, subtitle: String) {
    val appearance = LocalAppearance.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            NativeEntryPanel.loading(context, title, subtitle,
                appearance.background.toArgb(), appearance.text.toArgb(),
                appearance.muted.toArgb(), appearance.accent.toArgb())
        },
        update = { }
    )
}

@Composable
private fun AudioMessagePlayer(path: String, title: String, duration: Int) {
    var player by remember(path) { mutableStateOf<MediaPlayer?>(null) }
    var prepared by remember(path) { mutableStateOf(false) }
    var playing by remember(path) { mutableStateOf(false) }
    var pendingPlay by remember(path) { mutableStateOf(false) }
    DisposableEffect(path) {
        if (path.isNotBlank()) {
            val media = MediaPlayer()
            runCatching {
                media.setDataSource(path)
                media.setOnPreparedListener { prepared = true; if (pendingPlay) { it.start(); playing = true; pendingPlay = false } }
                media.setOnCompletionListener { playing = false; runCatching { it.seekTo(0) } }
                media.prepareAsync(); player = media
            }.onFailure { media.release() }
        }
        onDispose { runCatching { player?.release() }; player = null }
    }
    Row(Modifier.widthIn(min = 210.dp, max = 270.dp).padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(enabled = path.isNotBlank(), onClick = {
            val media = player ?: return@IconButton
            if (!prepared) pendingPlay = true else if (playing) { media.pause(); playing = false } else { media.start(); playing = true }
        }) { Text(if (playing) "Ⅱ" else "▶", color = LocalAppearance.current.accent, fontSize = 22.sp) }
        Column(Modifier.weight(1f)) {
            Text(title.ifBlank { "Аудиосообщение" }, color = LocalAppearance.current.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (path.isBlank()) "Загрузка…" else if (prepared) formatDuration(duration) else "Подготовка аудио…", color = LocalAppearance.current.muted, fontSize = 11.sp)
        }
    }
}
private fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%d:%02d".format(Locale.getDefault(), safe / 60, safe % 60)
}
private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Файл"
    val kb = bytes / 1024.0
    return if (kb < 1024) "%.0f КБ".format(Locale.getDefault(), kb) else "%.1f МБ".format(Locale.getDefault(), kb / 1024.0)
}
private fun openDownloadedMedia(context: android.content.Context, path: String, mimeType: String) {
    if (path.isBlank()) { android.widget.Toast.makeText(context, "Файл ещё загружается", android.widget.Toast.LENGTH_SHORT).show(); return }
    runCatching {
        val file = File(path); require(file.exists()) { "Файл ещё не загружен" }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeType.ifBlank { "*/*" }).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }.onFailure { android.widget.Toast.makeText(context, "Не удалось открыть файл: ${it.message ?: "нет подходящего приложения"}", android.widget.Toast.LENGTH_LONG).show() }
}
private fun prepareOutgoingMedia(context: android.content.Context, uri: Uri, prefix: String, maxBytes: Long): File {
    val resolver = context.contentResolver
    val name = resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }?.substringAfterLast('/')?.take(100)
    val extension = name?.substringAfterLast('.', "")?.takeIf { it.matches(Regex("[A-Za-z0-9]{1,8}")) }?.let { ".$it" } ?: ""
    val target = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}$extension")
    resolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output ->
            val buffer = ByteArray(64 * 1024); var total = 0L
            while (true) { val count = input.read(buffer); if (count < 0) break; total += count; require(total <= maxBytes) { "Файл превышает лимит ${maxBytes / (1024 * 1024)} МБ" }; output.write(buffer, 0, count) }
        }
    } ?: error("Не удалось открыть выбранный файл")
    require(target.length() > 0L) { "Выбран пустой файл" }
    return target
}

private fun avatarColor(id: Long): Color {
    val colors = listOf(Color(0xFF5B8DEF), Color(0xFF8E6AC8), Color(0xFF26A69A), Color(0xFFE28B55), Color(0xFFCC6688))
    return colors[kotlin.math.abs(id % colors.size).toInt()]
}
