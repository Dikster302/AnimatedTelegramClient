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
        td.closeChat()
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
                        val activeChat = selectedChat?.let { selected ->
                            chats.firstOrNull { it.id == selected.id } ?: selected
                        }
                        ConversationScreen(
                            chat = activeChat ?: selectedChat!!,
                            chats = chats,
                            messages = messages,
                            searchResults = searchResults,
                            onSearchMessages = { td.searchChatMessages(selectedChat!!.id,it) },
                            profile = profile,
                            onOpenProfile = { td.openProfile(selectedChat!!.id) },
                            onCloseProfile = { td.closeProfile() },
                            onBack = { td.closeProfile(); td.closeChat(); selectedChat = null },
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
    val appearance = LocalAppearance.current
    val storyOpenAction = onOpenStory
    val storyCloseAction = onCloseStory
    val storyReactionAction = onReactToStory
    val postStoryAction = onPostStory
    val postVideoStoryAction = onPostVideoStory
    val storyReplyAction = onReplyToStory
    val queryAction = onQuery
    val contactsQueryAction = onContactsQuery
    val sortAction = onSort
    val compactAction = onCompactChats
    val saveProfileAction = onSaveMyProfile
    val pickProfilePhotoAction = onPickProfilePhoto
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
    val pickHeaderWallpaperAction = onPickHeaderWallpaper
    val clearHeaderWallpaperAction = onClearHeaderWallpaper
    val cosmicCoverAction = onSelectCosmicCover
    val openAction = onOpen
    val openContactAction = onOpenContact
    val archiveAction = onArchive
    val pinAction = onTogglePinned
    val unreadAction = onToggleUnread
    val muteAction = onToggleMute
    val screenState = NativeChatsScreenView.State(
        chats, contacts, stories, query, contactsQuery, sortNewest, compactChats, account,
        operationError, privacyLastSeen, privacyPhone, privacyPhoto, themeName, glass, fontScale,
        accentName, bubbleRadius, wallpaperName, headerWallpaper, appearance.background.toArgb(),
        appearance.panel.toArgb(), appearance.text.toArgb(), appearance.muted.toArgb(),
        appearance.accent.toArgb(), appearance.panelLight.toArgb()
    )
    val screenListener = remember(chats, contacts, stories, query, contactsQuery, sortNewest,
        compactChats, account, operationError, privacyLastSeen, privacyPhone, privacyPhoto,
        themeName, glass, fontScale, accentName, bubbleRadius, wallpaperName, headerWallpaper,
        storyOpenAction, storyCloseAction, storyReactionAction, postStoryAction, postVideoStoryAction,
        storyReplyAction, queryAction, contactsQueryAction, sortAction, compactAction, saveProfileAction,
        pickProfilePhotoAction, dismissErrorAction, lastSeenAction, phonePrivacyAction, photoPrivacyAction,
        themeAction, glassAction, fontScaleAction, accentAction, bubbleRadiusAction, wallpaperAction,
        pickHeaderWallpaperAction, clearHeaderWallpaperAction, cosmicCoverAction, openAction,
        openContactAction, archiveAction, pinAction, unreadAction, muteAction) {
        object : NativeChatsScreenView.Listener {
            override fun onOpenStory(story:TgStory) = storyOpenAction(story)
            override fun onCloseStory(story:TgStory) = storyCloseAction(story)
            override fun onReactToStory(story:TgStory) = storyReactionAction(story)
            override fun onPostStory() = postStoryAction()
            override fun onPostVideoStory() = postVideoStoryAction()
            override fun onReplyToStory(story:TgStory,text:String) = storyReplyAction(story,text)
            override fun onQuery(query:String) = queryAction(query)
            override fun onContactsQuery(query:String) = contactsQueryAction(query)
            override fun onSort(newestFirst:Boolean) = sortAction(newestFirst)
            override fun onCompactChats(compact:Boolean) = compactAction(compact)
            override fun onSaveMyProfile(firstName:String,lastName:String,bio:String) = saveProfileAction(firstName,lastName,bio)
            override fun onPickProfilePhoto() = pickProfilePhotoAction()
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
            override fun onPickHeaderWallpaper() = pickHeaderWallpaperAction()
            override fun onClearHeaderWallpaper() = clearHeaderWallpaperAction()
            override fun onSelectCosmicCover() = cosmicCoverAction()
            override fun onOpen(chat:TgChat) = openAction(chat)
            override fun onOpenContact(contact:TgContact) = openContactAction(contact)
            override fun onArchive(chat:TgChat) = archiveAction(chat)
            override fun onTogglePinned(chat:TgChat) = pinAction(chat)
            override fun onToggleUnread(chat:TgChat) = unreadAction(chat)
            override fun onToggleMute(chat:TgChat) = muteAction(chat)
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
            .background(appearance.background),
        factory = { context -> NativeChatsScreenView(context, screenState, screenListener) },
        update = { view -> view.update(screenState, screenListener) }
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
    val appearance = LocalAppearance.current
    val sendAction = onSend
    val searchAction = onSearchMessages
    val openProfileAction = onOpenProfile
    val closeProfileAction = onCloseProfile
    val backAction = onBack
    val deleteAction = onDeleteMessage
    val clearAction = onClearHistory
    val editAction = onEditMessage
    val reactionAction = onReactToMessage
    val forwardAction = onForwardMessage
    val pinAction = onPinMessage
    val shapeAction = onVideoNoteShape
    val recordAction = onRecordVideo
    val dismissErrorAction = onDismissOperationError
    val olderAction = onLoadOlderMessages
    val photoAction = onSendPhoto
    val documentAction = onSendDocument
    val conversationListener = remember(chat.id,chats,sendAction,searchAction,openProfileAction,
        closeProfileAction,backAction,deleteAction,clearAction,editAction,reactionAction,
        forwardAction,pinAction,shapeAction,recordAction,dismissErrorAction,olderAction,
        photoAction,documentAction) {
        object : NativeConversationScreenView.Listener {
            override fun onSearchMessages(query:String) = searchAction(query)
            override fun onOpenProfile() = openProfileAction()
            override fun onCloseProfile() = closeProfileAction()
            override fun onBack() = backAction()
            override fun onSend(text:String,animated:Boolean,replyToMessageId:Long?) =
                sendAction(text,animated,replyToMessageId)
            override fun onDeleteMessage(message:TgMessage,forEveryone:Boolean) =
                deleteAction(message,forEveryone)
            override fun onClearHistory(forEveryone:Boolean) = clearAction(forEveryone)
            override fun onEditMessage(message:TgMessage,text:String) = editAction(message,text)
            override fun onReactToMessage(message:TgMessage,emoji:String) = reactionAction(message,emoji)
            override fun onForwardMessage(message:TgMessage,destination:TgChat) = forwardAction(message,destination)
            override fun onPinMessage(message:TgMessage) = pinAction(message)
            override fun onVideoNoteShape(shape:String) = shapeAction(shape)
            override fun onRecordVideo() = recordAction()
            override fun onDismissOperationError() = dismissErrorAction()
            override fun onLoadOlderMessages() = olderAction()
            override fun onSendPhoto() = photoAction()
            override fun onSendDocument() = documentAction()
        }
    }
    val panelColor = appearance.panel.copy(alpha = 1f - appearance.glass * 0.30f).toArgb()
    val panelLightColor = appearance.panelLight.copy(alpha = 1f - appearance.glass * 0.38f).toArgb()
    AndroidView(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
            .imePadding().background(appearance.chatBackground),
        factory = { context ->
            NativeConversationScreenView(context, chat, chats, messages, searchResults, profile,
                videoNoteShape, operationError, appearance.background.toArgb(),
                appearance.chatBackground.toArgb(), panelColor, panelLightColor,
                appearance.text.toArgb(), appearance.muted.toArgb(), appearance.accent.toArgb(),
                appearance.incoming.toArgb(), appearance.outgoing.toArgb(), appearance.bubbleRadius,
                conversationListener)
        },
        update = { view ->
            view.update(chat, chats, messages, searchResults, profile, videoNoteShape, operationError,
                appearance.background.toArgb(), appearance.chatBackground.toArgb(), panelColor,
                panelLightColor, appearance.text.toArgb(), appearance.muted.toArgb(),
                appearance.accent.toArgb(), appearance.incoming.toArgb(), appearance.outgoing.toArgb(),
                appearance.bubbleRadius, conversationListener)
        }
    )
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

