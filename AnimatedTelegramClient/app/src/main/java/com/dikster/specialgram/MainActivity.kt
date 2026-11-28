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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
    var showStoryComposer by remember { mutableStateOf(false) }
    var viewingStory by remember { mutableStateOf<Pair<Long,Int>?>(null) }
    val orderedStories = remember(stories) { TdStoryListFilter.ordered(stories) }
    val activeStoryIndex = orderedStories.indexOfFirst { viewingStory == (it.chatId to it.storyId) }
    val activeStory = orderedStories.getOrNull(activeStoryIndex)
    var selectedTab by remember { mutableStateOf(0) }
    BackHandler(enabled = selectedTab != 0) { selectedTab = 0 }
    var unreadOnly by remember { mutableStateOf(false) }
    val searchBarRef = remember { mutableStateOf<NativeChatSearchBarView?>(null) }
    val sortAction = onSort
    val chatsHeaderListener = remember(sortAction) {
        object : NativeChatsHeaderView.Listener {
            override fun onSelectTab(tab: Int) { selectedTab = tab }
            override fun onSort(newestFirst: Boolean) { sortAction(newestFirst); selectedTab = 0 }
            override fun onToggleUnread() { unreadOnly = !unreadOnly; selectedTab = 0 }
            override fun onSearch() { if (selectedTab != 0) selectedTab = 0 else searchBarRef.value?.focusSearch() }
        }
    }
    var folder by remember { mutableStateOf("Все") }
    val storyStripListener = remember(onPostStory, onOpenStory) {
        object : NativeStoryStripView.Listener {
            override fun onPostStory() = onPostStory()
            override fun onOpenStory(story: TgStory) {
                viewingStory = story.chatId to story.storyId
                onOpenStory(story)
            }
        }
    }
    val folderListener = remember {
        NativeChatFoldersView.Listener { value -> folder = value }
    }
    val filtered = remember(chats, query, sortNewest, unreadOnly, folder) {
        TdChatListFilter.filter(chats, query, sortNewest, unreadOnly, folder)
    }
    Column(Modifier.fillMaxSize().background(LocalAppearance.current.background)) {
        Box(Modifier.fillMaxWidth()) {
            HeaderWallpaper(uri = if (selectedTab == 0) headerWallpaper else null, modifier = Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
                val toolbarChatCount = chats.count { !it.archived }
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        NativeChatsHeaderView(context, selectedTab, toolbarChatCount,
                            unreadOnly, sortNewest, Color.WHITE.toArgb(), chatsHeaderListener)
                    },
                    update = { view ->
                        view.update(selectedTab, toolbarChatCount, unreadOnly, sortNewest,
                            Color.WHITE.toArgb(), chatsHeaderListener)
                    }
                )
                if (selectedTab == 0) {
                    val queryAction = onQuery
                    val searchListener = remember(queryAction) {
                        NativeChatSearchBarView.Listener { value -> queryAction(value) }
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp)
                            .height(50.dp),
                        factory = { context -> NativeChatSearchBarView(context, query, searchListener).also { searchBarRef.value = it } },
                        update = { view -> searchBarRef.value = view; view.update(query, searchListener) }
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
                val appearance = LocalAppearance.current
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(92.dp),
                    factory = { context ->
                        NativeStoryStripView(context, stories, chats, appearance.text.toArgb(),
                            appearance.accent.toArgb(), appearance.panelLight.toArgb(), storyStripListener)
                    },
                    update = { view ->
                        view.update(stories, chats, appearance.text.toArgb(),
                            appearance.accent.toArgb(), appearance.panelLight.toArgb(), storyStripListener)
                    }
                )
            }
            val folderAppearance = LocalAppearance.current
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                factory = { context ->
                    NativeChatFoldersView(context, folder, folderAppearance.text.toArgb(),
                        folderAppearance.muted.toArgb(), folderAppearance.accent.toArgb(), folderListener)
                },
                update = { view ->
                    view.update(folder, folderAppearance.text.toArgb(),
                        folderAppearance.muted.toArgb(), folderAppearance.accent.toArgb(), folderListener)
                }
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Все чаты", color = LocalAppearance.current.accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text(if (unreadOnly) "Непрочитанные" else if (sortNewest) "По активности" else "А — Я", color = LocalAppearance.current.muted, fontSize = 12.sp)
            }
            if (filtered.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(if (chats.isEmpty()) "Загружаем чаты…" else "Ничего не найдено",
                        color = LocalAppearance.current.muted)
                }
            } else {
                val openAction = onOpen
                val archiveAction = onArchive
                val pinAction = onTogglePinned
                val unreadAction = onToggleUnread
                val muteAction = onToggleMute
                val chatListListener = remember(openAction, archiveAction, pinAction, unreadAction, muteAction) {
                    object : NativeChatListView.Listener {
                        override fun onOpen(chat: TgChat) = openAction(chat)
                        override fun onArchive(chat: TgChat) = archiveAction(chat)
                        override fun onTogglePinned(chat: TgChat) = pinAction(chat)
                        override fun onToggleUnread(chat: TgChat) = unreadAction(chat)
                        override fun onToggleMute(chat: TgChat) = muteAction(chat)
                    }
                }
                val chatListAppearance = LocalAppearance.current
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    factory = { context ->
                        NativeChatListView(context, filtered, compactChats,
                            chatListAppearance.text.toArgb(), chatListAppearance.muted.toArgb(),
                            chatListAppearance.accent.toArgb(), chatListAppearance.panel.copy(alpha = 1f - chatListAppearance.glass * 0.30f).toArgb(),
                            chatListListener)
                    },
                    update = { view ->
                        view.update(filtered, compactChats, chatListAppearance.text.toArgb(),
                            chatListAppearance.muted.toArgb(), chatListAppearance.accent.toArgb(),
                            chatListAppearance.panel.copy(alpha = 1f - chatListAppearance.glass * 0.30f).toArgb(), chatListListener)
                    }
                )
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
    val panelColor = LocalAppearance.current.panel.toArgb()
    AndroidView(
        modifier = modifier,
        factory = { context -> NativeHeaderWallpaperView(context, uri, panelColor) },
        update = { view -> view.update(uri, panelColor) }
    )
}

@Composable
private fun ContactsPanel(contacts: List<TgContact>, query: String, onQuery: (String) -> Unit, onOpen: (TgContact) -> Unit) {
    val appearance = LocalAppearance.current
    val listener = remember(onQuery, onOpen) {
        object : NativeContactsPanel.Listener {
            override fun onQuery(query: String) = onQuery(query)
            override fun onOpen(contact: TgContact) = onOpen(contact)
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            NativeContactsPanel(context, contacts, query, appearance.background.toArgb(),
                appearance.text.toArgb(), appearance.muted.toArgb(), appearance.accent.toArgb(),
                appearance.panelLight.toArgb(), listener)
        },
        update = { view ->
            view.update(contacts, query, appearance.background.toArgb(),
                appearance.text.toArgb(), appearance.muted.toArgb(), appearance.accent.toArgb(),
                appearance.panelLight.toArgb(), listener)
        }
    )
}

@Composable
private fun SettingsPanel(
    account: TgAccount?, onSaveMyProfile: (String,String,String) -> Unit, onPickProfilePhoto: () -> Unit, operationError: String?, onDismissOperationError: () -> Unit, privacyLastSeen: String, onPrivacyLastSeen: (String) -> Unit, privacyPhone: String, onPrivacyPhone: (String) -> Unit, privacyPhoto: String, onPrivacyPhoto: (String) -> Unit, themeName: String, onTheme: (String) -> Unit,
    glass: Float, onGlass: (Float) -> Unit, fontScale: Float, onFontScale: (Float) -> Unit,
    accentName: String, onAccent: (String) -> Unit, bubbleRadius: Float, onBubbleRadius: (Float) -> Unit,
    wallpaperName: String, onWallpaper: (String) -> Unit, compactChats: Boolean, onCompactChats: (Boolean) -> Unit,
    headerWallpaper: String?, onPickHeaderWallpaper: () -> Unit, onClearHeaderWallpaper: () -> Unit, onSelectCosmicCover: () -> Unit
) { 
    val appearance = LocalAppearance.current
    val saveProfileAction = onSaveMyProfile
    val pickPhotoAction = onPickProfilePhoto
    val dismissErrorAction = onDismissOperationError
    val lastSeenAction = onPrivacyLastSeen
    val phonePrivacyAction = onPrivacyPhone
    val photoPrivacyAction = onPrivacyPhoto
    val themeAction = onTheme
    val glassAction = onGlass
    val fontScaleAction = onFontScale
    val accentAction = onAccent
    val bubbleRadiusAction = onBubbleRadius
    val wallpaperAction = onWallpaper
    val compactChatsAction = onCompactChats
    val pickHeaderWallpaperAction = onPickHeaderWallpaper
    val clearHeaderWallpaperAction = onClearHeaderWallpaper
    val cosmicCoverAction = onSelectCosmicCover
    val settingsListener = remember(saveProfileAction,pickPhotoAction,dismissErrorAction,
        lastSeenAction,phonePrivacyAction,photoPrivacyAction,themeAction,glassAction,fontScaleAction,
        accentAction,bubbleRadiusAction,wallpaperAction,compactChatsAction,pickHeaderWallpaperAction,
        clearHeaderWallpaperAction,cosmicCoverAction) {
        object : NativeSettingsPanelView.Listener {
            override fun onSaveMyProfile(firstName:String,lastName:String,bio:String) =
                saveProfileAction(firstName,lastName,bio)
            override fun onPickProfilePhoto() = pickPhotoAction()
            override fun onDismissOperationError() = dismissErrorAction()
            override fun onPrivacyLastSeen(value:String) = lastSeenAction(value)
            override fun onPrivacyPhone(value:String) = phonePrivacyAction(value)
            override fun onPrivacyPhoto(value:String) = photoPrivacyAction(value)
            override fun onTheme(value:String) = themeAction(value)
            override fun onGlass(value:Float) = glassAction(value)
            override fun onFontScale(value:Float) = fontScaleAction(value)
            override fun onAccent(value:String) = accentAction(value)
            override fun onBubbleRadius(value:Float) = bubbleRadiusAction(value)
            override fun onWallpaper(value:String) = wallpaperAction(value)
            override fun onCompactChats(value:Boolean) = compactChatsAction(value)
            override fun onPickHeaderWallpaper() = pickHeaderWallpaperAction()
            override fun onClearHeaderWallpaper() = clearHeaderWallpaperAction()
            override fun onSelectCosmicCover() = cosmicCoverAction()
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            NativeSettingsPanelView(context, account, operationError, privacyLastSeen,
                privacyPhone, privacyPhoto, themeName, glass, fontScale, accentName,
                bubbleRadius, wallpaperName, compactChats, headerWallpaper,
                appearance.background.toArgb(), appearance.panel.toArgb(),
                appearance.text.toArgb(), appearance.muted.toArgb(), appearance.accent.toArgb(),
                appearance.panelLight.toArgb(), settingsListener)
        },
        update = { view ->
            view.update(account, operationError, privacyLastSeen, privacyPhone, privacyPhoto,
                themeName, glass, fontScale, accentName, bubbleRadius, wallpaperName,
                compactChats, headerWallpaper, appearance.background.toArgb(),
                appearance.panel.toArgb(), appearance.text.toArgb(), appearance.muted.toArgb(),
                appearance.accent.toArgb(), appearance.panelLight.toArgb(), settingsListener)
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
    val shapePickerContext = androidx.compose.ui.platform.LocalContext.current

    var draft by remember { mutableStateOf("") }
    var animatedMode by remember { mutableStateOf(true) }
    var showProfile by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<TgMessage?>(null) }
    var replyingTo by remember { mutableStateOf<TgMessage?>(null) }
    var attachmentMenuOpen by remember { mutableStateOf(false) }
    var messageSearchOpen by remember { mutableStateOf(false) }
    var messageSearchQuery by remember { mutableStateOf("") }
    var messageSearchIndex by remember { mutableIntStateOf(0) }
    val backAction = onBack
    val openProfileAction = onOpenProfile
    val conversationHeaderListener = remember(backAction, openProfileAction) {
        object : NativeConversationHeaderView.Listener {
            override fun onBack() = backAction()
            override fun onOpenProfile() { showProfile = true; openProfileAction() }
            override fun onSearch() { messageSearchOpen = true; messageSearchQuery = ""; messageSearchIndex = 0 }
            override fun onClearHistory() { NativeClearHistoryDialog.show(shapePickerContext, chat.title, NativeClearHistoryDialog.Listener { onClearHistory(it) }) }
            override fun onToggleAnimated() { animatedMode = !animatedMode }
        }
    }
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
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding().background(LocalAppearance.current.chatBackground)) {
        if (operationError != null) AlertDialog(
            onDismissRequest = onDismissOperationError,
            title = { Text("Не удалось выполнить действие", color = LocalAppearance.current.text) },
            text = { Text(operationError, color = LocalAppearance.current.muted) },
            confirmButton = { TextButton(onClick = onDismissOperationError) { Text("Понятно") } }
        )
        val dialogAppearance = LocalAppearance.current
        val messageDialogContext = shapePickerContext
        val messageActionsListener = remember(chats, chat.id, onReactToMessage, onForwardMessage,
            onPinMessage, onDeleteMessage, onEditMessage) {
            object : NativeMessageActionsDialog.Listener {
                override fun onDismiss() { selectedMessage = null }
                override fun onReply(message: TgMessage) { replyingTo = message }
                override fun onReaction(message: TgMessage, emoji: String) { onReactToMessage(message, emoji) }
                override fun onForward(message: TgMessage, destination: TgChat) { onForwardMessage(message, destination) }
                override fun onPin(message: TgMessage) { onPinMessage(message) }
                override fun onEdit(message: TgMessage, text: String) { onEditMessage(message, text) }
                override fun onDelete(message: TgMessage, forEveryone: Boolean) { onDeleteMessage(message, forEveryone) }
            }
        }
        LaunchedEffect(selectedMessage?.id) {
            val target = selectedMessage ?: return@LaunchedEffect
            NativeMessageActionsDialog.show(messageDialogContext, target, chats, chat.id,
                dialogAppearance.text.toArgb(), dialogAppearance.muted.toArgb(),
                dialogAppearance.accent.toArgb(), messageActionsListener)
        }
        if (showProfile) {
            val profileAppearance = LocalAppearance.current
            val closeProfileAction = onCloseProfile
            val profileDialogListener = remember(closeProfileAction) {
                NativeProfileDialogHost.Listener {
                    showProfile = false
                    closeProfileAction()
                }
            }
            AndroidView(
                modifier = Modifier.size(1.dp),
                factory = { context ->
                    NativeProfileDialogHost(context, chat, profile,
                        profileAppearance.text.toArgb(), profileAppearance.muted.toArgb(),
                        profileAppearance.accent.toArgb(), profileAppearance.panelLight.toArgb(),
                        profileDialogListener)
                },
                update = { view ->
                    view.update(chat, profile, profileAppearance.text.toArgb(),
                        profileAppearance.muted.toArgb(), profileAppearance.accent.toArgb(),
                        profileAppearance.panelLight.toArgb())
                }
            )
        }
        val headerAppearance = LocalAppearance.current
        AndroidView(
            modifier = Modifier.fillMaxWidth()
                .background(headerAppearance.panel.copy(alpha = 1f - headerAppearance.glass * 0.30f)),
            factory = { context ->
                NativeConversationHeaderView(context, chat, profile, animatedMode,
                    headerAppearance.text.toArgb(), headerAppearance.muted.toArgb(),
                    headerAppearance.accent.toArgb(), headerAppearance.panelLight.toArgb(),
                    conversationHeaderListener)
            },
            update = { view ->
                view.update(chat, profile, animatedMode, headerAppearance.text.toArgb(),
                    headerAppearance.muted.toArgb(), headerAppearance.accent.toArgb(),
                    headerAppearance.panelLight.toArgb(), conversationHeaderListener)
            }
        )

        if (messageSearchOpen) {
            val searchListener = remember(messageSearchIndex, visibleMessages) {
                object : NativeMessageSearchBarView.Listener {
                    override fun onQuery(value: String) { messageSearchQuery = value; messageSearchIndex = 0 }
                    override fun onPrevious() { messageSearchIndex = if (messageSearchIndex <= 0) visibleMessages.lastIndex else messageSearchIndex - 1 }
                    override fun onNext() { if (visibleMessages.isNotEmpty()) messageSearchIndex = (messageSearchIndex + 1) % visibleMessages.size }
                    override fun onClose() { messageSearchOpen = false; messageSearchQuery = ""; messageSearchIndex = 0 }
                }
            }
            val searchAppearance = LocalAppearance.current
            AndroidView(
                modifier = Modifier.fillMaxWidth().background(searchAppearance.panel)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                factory = { context ->
                    NativeMessageSearchBarView(context, messageSearchQuery, messageSearchIndex,
                        visibleMessages.size, searchAppearance.text.toArgb(),
                        searchAppearance.muted.toArgb(), searchAppearance.accent.toArgb(),
                        searchAppearance.panelLight.toArgb(), searchListener)
                },
                update = { view ->
                    view.update(messageSearchQuery, messageSearchIndex, visibleMessages.size,
                        searchAppearance.text.toArgb(), searchAppearance.muted.toArgb(),
                        searchAppearance.accent.toArgb(), searchAppearance.panelLight.toArgb(),
                        searchListener)
                }
            )
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
            val listAppearance = LocalAppearance.current
            val loadOlderAction = onLoadOlderMessages
            val reactionAction = onReactToMessage
            val messageListListener = remember(loadOlderAction, reactionAction) {
                object : NativeMessageListView.Listener {
                    override fun onLongPress(message: TgMessage) { selectedMessage = message }
                    override fun onReaction(message: TgMessage, emoji: String) { reactionAction(message, emoji) }
                    override fun onLoadOlder() = loadOlderAction()
                }
            }
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { context ->
                    NativeMessageListView(context, chat.id, visibleMessages, messageSearchOpen,
                        messageSearchIndex, listAppearance.text.toArgb(), listAppearance.muted.toArgb(),
                        listAppearance.accent.toArgb(), listAppearance.incoming.toArgb(),
                        listAppearance.outgoing.toArgb(), listAppearance.panel.toArgb(),
                        listAppearance.bubbleRadius, messageListListener)
                },
                update = { view ->
                    view.update(chat.id, visibleMessages, messageSearchOpen, messageSearchIndex,
                        listAppearance.text.toArgb(), listAppearance.muted.toArgb(),
                        listAppearance.accent.toArgb(), listAppearance.incoming.toArgb(),
                        listAppearance.outgoing.toArgb(), listAppearance.panel.toArgb(),
                        listAppearance.bubbleRadius, messageListListener)
                }
            )
        }

        val sendAction = onSend
        val photoAction = onSendPhoto
        val documentAction = onSendDocument
        val recordAction = onRecordVideo
        val composerListener = remember(animatedMode, replyingTo, chat.title, videoNoteShape, onVideoNoteShape, shapePickerContext,
            sendAction, photoAction, documentAction, recordAction) {
            object : NativeMessageComposerView.Listener {
                override fun onDraftChanged(value: String) { draft = value }
                override fun onSend(value: String) {
                    val replyId = replyingTo?.id
                    draft = ""
                    replyingTo = null
                    sendAction(value, animatedMode, replyId)
                }
                override fun onSendPhoto() = photoAction()
                override fun onSendDocument() = documentAction()
                override fun onRecordVideo() = recordAction()
                override fun onChooseVideoShape() { NativeVideoShapePickerDialog.show(shapePickerContext, videoNoteShape, NativeVideoShapePickerDialog.Listener { onVideoNoteShape(it) }) }
                override fun onClearReply() { replyingTo = null }
            }
        }
        val composerAppearance = LocalAppearance.current
        AndroidView(
            modifier = Modifier.fillMaxWidth()
                .background(composerAppearance.panel.copy(alpha = 1f - composerAppearance.glass * 0.30f)),
            factory = { context ->
                NativeMessageComposerView(context, draft, replyingTo, chat.title,
                    composerAppearance.text.toArgb(), composerAppearance.muted.toArgb(),
                    composerAppearance.accent.toArgb(), composerAppearance.panel.copy(alpha = 1f - composerAppearance.glass * 0.30f).toArgb(),
                    composerAppearance.panelLight.copy(alpha = 1f - composerAppearance.glass * 0.38f).toArgb(),
                    composerListener)
            },
            update = { view ->
                view.update(draft, replyingTo, chat.title,
                    composerAppearance.text.toArgb(), composerAppearance.muted.toArgb(),
                    composerAppearance.accent.toArgb(), composerAppearance.panel.copy(alpha = 1f - composerAppearance.glass * 0.30f).toArgb(),
                    composerAppearance.panelLight.copy(alpha = 1f - composerAppearance.glass * 0.38f).toArgb(),
                    composerListener)
            }
        )

    }
}

@Composable
private fun StoryViewer(story: TgStory, onDismiss: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, storyPosition: Int, storyCount: Int, onReact: () -> Unit, onReply: (String) -> Unit) {
    val listener = remember(onDismiss, onPrevious, onNext, onReact, onReply) {
        object : NativeStoryViewerView.Listener {
            override fun onDismiss() = onDismiss()
            override fun onPrevious() = onPrevious()
            override fun onNext() = onNext()
            override fun onReact() = onReact()
            override fun onReply(text: String) = onReply(text)
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(620.dp),
            factory = { context ->
                NativeStoryViewerView(context, story, storyPosition, storyCount, listener)
            },
            update = { view -> view.update(story, storyPosition, storyCount, listener) }
        )
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
                appearance.muted.toArgb(), appearance.accent.toArgb(), { onValue(it) }, { onSubmit() })
        },
        update = { panel ->
            panel.update(title, subtitle, value, button, secret,
                appearance.background.toArgb(), appearance.text.toArgb(),
                appearance.muted.toArgb(), appearance.accent.toArgb(), { onValue(it) }, { onSubmit() })
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

