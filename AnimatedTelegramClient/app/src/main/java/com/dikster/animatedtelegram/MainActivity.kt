package com.dikster.animatedtelegram

import android.os.Bundle
import android.net.Uri
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
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
import java.text.SimpleDateFormat
import java.util.Date
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
        window.statusBarColor = android.graphics.Color.rgb(23, 33, 43)
        window.navigationBarColor = android.graphics.Color.rgb(14, 22, 33)
        td = TdLibClient(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF2AABEE), background = Color(0xFF0E1621), surface = Color(0xFF17212B),
                onBackground = Color(0xFFEAF2F8), onSurface = Color(0xFFEAF2F8)
            )) { TelegramApp(td) }
        }
    }
    override fun onDestroy() {
        if (::td.isInitialized) td.close()
        super.onDestroy()
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
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            headerWallpaper = uri.toString()
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
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.platform.LocalDensity.current.copy(fontScale = fontScale)) {
    val auth by td.auth.collectAsState()
    val chats by td.chats.collectAsState()
    val contacts by td.contacts.collectAsState()
    val messages by td.messages.collectAsState()
    val profile by td.profile.collectAsState()
    val account by td.account.collectAsState()
    val operationError by td.operationError.collectAsState()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedChat by remember { mutableStateOf<TgChat?>(null) }
    var query by remember { mutableStateOf("") }
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
                            query = query,
                            onQuery = { query = it },
                            sortNewest = sortNewest,
                            onSort = { sortNewest = it },
                            compactChats = compactChats,
                            onCompactChats = { compactChats = it },
                            account = account, themeName = themeName, onTheme = { themeName = it },
                            glass = glass, onGlass = { glass = it }, fontScale = fontScale, onFontScale = { fontScale = it },
                            accentName = accentName, onAccent = { accentName = it }, bubbleRadius = bubbleRadius, onBubbleRadius = { bubbleRadius = it },
                            wallpaperName = wallpaperName, onWallpaper = { wallpaperName = it },
                            headerWallpaper = headerWallpaper,
                            onPickHeaderWallpaper = { imagePicker.launch(arrayOf("image/*")) },
                            onClearHeaderWallpaper = { headerWallpaper = null },
                            onSelectCosmicCover = { headerWallpaper = "preset:cosmic" },
                            onOpen = { selectedChat = it; td.openChat(it.id) },
                            onOpenContact = { contact -> td.openContact(contact.id) { selectedChat = it; td.openChat(it.id) } },
                            onArchive = { td.archiveChat(it.id, !it.archived) }
                        )
                    } else {
                        ConversationScreen(
                            chat = selectedChat!!,
                            messages = messages,
                            profile = profile,
                            onOpenProfile = { td.openProfile(selectedChat!!.id) },
                            onCloseProfile = { td.closeProfile() },
                            onBack = { selectedChat = null },
                            onSend = { text, animated ->
                                if (animated) td.sendAnimated(selectedChat!!.id, text)
                                else td.sendPlain(selectedChat!!.id, text)
                            },
                            onDeleteMessage = { message, revoke -> td.deleteMessage(selectedChat!!.id, message.id, revoke) },
                            onClearHistory = { revoke -> td.clearHistory(selectedChat!!.id, revoke) },
                            onEditMessage = { message, text -> td.editMessage(selectedChat!!.id, message.id, text) },
                            videoNoteShape = videoNoteShape,
                            onVideoNoteShape = { videoNoteShape = it },
                            onRecordVideo = { runCatching { videoCaptureLauncher.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE).putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60)) }.onFailure { td.reportOperationError("Не удалось открыть камеру: ${it.message ?: "камера недоступна"}") } },
                            operationError = operationError,
                            onDismissOperationError = { td.clearOperationError() }
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
private fun ChatsScreen(chats: List<TgChat>, contacts: List<TgContact>, query: String, onQuery: (String) -> Unit, sortNewest: Boolean, onSort: (Boolean) -> Unit, compactChats: Boolean, onCompactChats: (Boolean) -> Unit, account: TgAccount?, themeName: String, onTheme: (String) -> Unit, glass: Float, onGlass: (Float) -> Unit, fontScale: Float, onFontScale: (Float) -> Unit, accentName: String, onAccent: (String) -> Unit, bubbleRadius: Float, onBubbleRadius: (Float) -> Unit, wallpaperName: String, onWallpaper: (String) -> Unit, headerWallpaper: String?, onPickHeaderWallpaper: () -> Unit, onClearHeaderWallpaper: () -> Unit, onSelectCosmicCover: () -> Unit, onOpen: (TgChat) -> Unit, onOpenContact: (TgContact) -> Unit, onArchive: (TgChat) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    BackHandler(enabled = selectedTab != 0) { selectedTab = 0 }
    var unreadOnly by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf("Все") }
    val filtered = remember(chats, query, sortNewest, unreadOnly, folder) {
        val found = chats.filter { chat -> chat.title.contains(query, ignoreCase = true) && (!unreadOnly || chat.unreadCount > 0) && when(folder) { "Архив" -> chat.archived; "Личные" -> !chat.archived && chat.kind == "Личный чат"; "Группы" -> !chat.archived && chat.kind.contains("Группа"); "Каналы" -> !chat.archived && chat.kind == "Канал или группа"; else -> !chat.archived } }
        if (sortNewest) found.sortedByDescending { it.lastDate } else found.sortedBy { it.title.lowercase(Locale.getDefault()) }
    }
    Column(Modifier.fillMaxSize().background(LocalAppearance.current.background)) {
        Box(Modifier.fillMaxWidth()) {
            HeaderWallpaper(uri = if (selectedTab == 0) headerWallpaper else null, modifier = Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().background(Color.Transparent).padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = "Specialgram", modifier = Modifier.size(42.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (selectedTab == 2) "Настройки" else if (selectedTab == 1) "Контакты" else "Specialgram", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("${chats.size} чатов", color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
                    }
                    if (selectedTab != 2) Box {
                        IconButton(onClick = { menuOpen = true }) { Text("⋮", color = Color.White, fontSize = 25.sp) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Сначала новые") }, onClick = { onSort(true); menuOpen = false })
                            DropdownMenuItem(text = { Text("По названию: А — Я") }, onClick = { onSort(false); menuOpen = false })
                            DropdownMenuItem(text = { Text(if (unreadOnly) "Показать все чаты" else "Только непрочитанные") }, onClick = { unreadOnly = !unreadOnly; menuOpen = false })
                        }
                    }
                }
                if (selectedTab == 0) {
                    OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), singleLine = true,
                        placeholder = { Text("Поиск") }, leadingIcon = { Text("⌕", fontSize = 24.sp, color = LocalAppearance.current.muted) }, shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = LocalAppearance.current.panel.copy(alpha = 0.78f), unfocusedContainerColor = LocalAppearance.current.panel.copy(alpha = 0.78f), focusedBorderColor = LocalAppearance.current.accent, unfocusedBorderColor = Color.Transparent, focusedTextColor = LocalAppearance.current.text, unfocusedTextColor = LocalAppearance.current.text))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Все", "Личные", "Группы", "Каналы", "Архив").forEach { item -> FilterChip(selected = folder == item, onClick = { folder = item }, label = { Text(item) }) }
                    }
                }
            }
        }
        if (selectedTab == 2) {
            Box(Modifier.weight(1f).fillMaxWidth()) { SettingsPanel(account, themeName, onTheme, glass, onGlass, fontScale, onFontScale, accentName, onAccent, bubbleRadius, onBubbleRadius, wallpaperName, onWallpaper, compactChats, onCompactChats, headerWallpaper, onPickHeaderWallpaper, onClearHeaderWallpaper, onSelectCosmicCover) }
        } else if (selectedTab == 1) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                ContactsPanel(contacts.filter { it.name.contains(query, true) || it.username.contains(query, true) || it.phone.contains(query) }, query, onQuery, onOpenContact)
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Все чаты", color = LocalAppearance.current.accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text(if (unreadOnly) "Непрочитанные" else if (sortNewest) "По активности" else "А — Я", color = LocalAppearance.current.muted, fontSize = 12.sp)
            }
            if (filtered.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(if (chats.isEmpty()) "Загружаем чаты…" else "Ничего не найдено", color = LocalAppearance.current.muted) }
            else LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(filtered, key = { it.id }) { chat ->
                    ChatRow(chat, compact = compactChats, onArchive = { onArchive(chat) }) { onOpen(chat) }
                    HorizontalDivider(color = Color(0xFF253341), thickness = 0.6.dp, modifier = Modifier.padding(start = 82.dp))
                }
            }
        }
        NavigationBar(containerColor = LocalAppearance.current.panel.copy(alpha = 1f - LocalAppearance.current.glass * 0.30f), contentColor = LocalAppearance.current.accent, tonalElevation = 0.dp) {
            NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Text("☷", fontSize = 20.sp) }, label = { Text("Чаты") })
            NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Text("♙", fontSize = 20.sp) }, label = { Text("Контакты") })
            NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Text("⚙", fontSize = 20.sp) }, label = { Text("Настройки") })
        }
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
            placeholder = { Text("Поиск контактов") }, shape = RoundedCornerShape(24.dp),
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
    account: TgAccount?, themeName: String, onTheme: (String) -> Unit,
    glass: Float, onGlass: (Float) -> Unit, fontScale: Float, onFontScale: (Float) -> Unit,
    accentName: String, onAccent: (String) -> Unit, bubbleRadius: Float, onBubbleRadius: (Float) -> Unit,
    wallpaperName: String, onWallpaper: (String) -> Unit, compactChats: Boolean, onCompactChats: (Boolean) -> Unit,
    headerWallpaper: String?, onPickHeaderWallpaper: () -> Unit, onClearHeaderWallpaper: () -> Unit, onSelectCosmicCover: () -> Unit
) {
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
                Text(if (page == "cover") "Обложка Specialgram" else "Внешний вид", color = LocalAppearance.current.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (page == "main") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = "Specialgram", modifier = Modifier.size(60.dp))
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
                SettingsNavigationRow("О приложении", "Specialgram · 0.3") { page = "about" }
                Text("Авторская функция", color = LocalAppearance.current.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                Text("Постепенная отправка текста доступна в каждом чате — включай или выключай её переключателем.", color = LocalAppearance.current.muted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(bottom = 18.dp))
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
            } else {
                Text(when (page) { "account" -> "Данные аккаунта"; "notifications" -> "Уведомления"; "privacy" -> "Конфиденциальность"; "storage" -> "Данные и память"; else -> "О приложении" }, color = LocalAppearance.current.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
                Text(when (page) { "account" -> account?.phone?.ifBlank { "Номер скрыт Telegram" } ?: "Данные аккаунта загружаются"; "notifications" -> "Управление уведомлениями Telegram будет доступно после подключения соответствующих настроек TDLib."; "privacy" -> "Параметры приватности пока не подключены к интерфейсу TDLib."; "storage" -> "Управление хранилищем пока не подключено."; else -> "Specialgram — клиент Telegram на базе TDLib." }, color = LocalAppearance.current.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
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
private fun ChatRow(chat: TgChat, compact: Boolean = false, onArchive: () -> Unit, onClick: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val initials = chat.title.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString("").uppercase()
    val time = remember(chat.lastDate) {
        if (chat.lastDate <= 0) "" else try {
            val d = Date(chat.lastDate.toLong() * 1000L)
            val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val day = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(d)
            if (today == day) SimpleDateFormat("HH:mm", Locale.getDefault()).format(d) else SimpleDateFormat("dd.MM", Locale.getDefault()).format(d)
        } catch (_: Exception) { "" }
    }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = if (compact) 5.dp else 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(path = chat.avatarPath, fallback = initials.ifBlank { "T" }, id = chat.id, size = 56, online = chat.isOnline, hasStory = chat.hasStory)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.title, color = LocalAppearance.current.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (chat.isPremium) Text("✦", color = Color(0xFFB58CFF), fontSize = 13.sp)
                if (time.isNotBlank()) Text(time, color = if (chat.unreadCount > 0) LocalAppearance.current.accent else LocalAppearance.current.muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.preview.ifBlank { chat.status.ifBlank { "Нет сообщений" } }, color = if (chat.isOnline) Color(0xFF35C759) else LocalAppearance.current.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (chat.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.defaultMinSize(minWidth = 21.dp, minHeight = 21.dp).clip(CircleShape).background(LocalAppearance.current.accent).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                        Text(if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Text("⋮", color = LocalAppearance.current.muted, fontSize = 20.sp) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(if (chat.archived) "Вернуть из архива" else "В архив") }, onClick = { menu = false; onArchive() })
            }
        }
    }
}

@Composable
private fun Avatar(path: String, fallback: String, id: Long, size: Int, online: Boolean = false, hasStory: Boolean = false) {
    val bitmap = remember(path) { if (path.isNotBlank()) runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() else null }
    val storyRing = Brush.sweepGradient(listOf(Color(0xFF42C7FF), Color(0xFF6575FF), Color(0xFFB34DFF), Color(0xFF6575FF), Color(0xFF42C7FF)))
    val ringModifier = if (hasStory) Modifier.border(2.8.dp, storyRing, CircleShape).padding(3.dp) else Modifier
    Box(Modifier.size(size.dp).then(ringModifier).clip(CircleShape).background(avatarColor(id)), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap = bitmap, contentDescription = "Аватар", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(fallback, color = Color.White, fontSize = (size / 3).sp, fontWeight = FontWeight.SemiBold)
        if (online) Box(Modifier.align(Alignment.BottomEnd).size((size / 4).dp).clip(CircleShape).background(Color(0xFF35C759)).padding(2.dp).background(LocalAppearance.current.panel.copy(alpha = 1f - LocalAppearance.current.glass * 0.30f), CircleShape).padding(2.dp).background(Color(0xFF35C759), CircleShape))
    }
}

@Composable
private fun ConversationScreen(
    chat: TgChat, messages: List<TgMessage>, profile: TgProfile?, onOpenProfile: () -> Unit, onCloseProfile: () -> Unit, onBack: () -> Unit,
    onSend: (String, Boolean) -> Unit,
    onDeleteMessage: (TgMessage, Boolean) -> Unit,
    onClearHistory: (Boolean) -> Unit,
    onEditMessage: (TgMessage, String) -> Unit,
    videoNoteShape: String, onVideoNoteShape: (String) -> Unit, onRecordVideo: () -> Unit,
    operationError: String?, onDismissOperationError: () -> Unit
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
    var showVideoShapePicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    Column(Modifier.fillMaxSize().background(LocalAppearance.current.chatBackground)) {
        if (showVideoShapePicker) AlertDialog(
            onDismissRequest = { showVideoShapePicker = false },
            title = { Text("Форма видеосообщения", color = LocalAppearance.current.text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("В обычном Telegram видеосообщения будут круглыми. Другие формы станут доступны между пользователями Specialgram после подключения сервера.", color = LocalAppearance.current.muted, fontSize = 12.sp)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = deleteForBoth, onCheckedChange = { deleteForBoth = it })
                            Text("Удалить у всех", color = LocalAppearance.current.text, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    Row {
                        if (target.outgoing) {
                            TextButton(onClick = { editingMessage = target; editDraft = target.text; selectedMessage = null }) { Text("Изменить") }
                        }
                        TextButton(onClick = { onDeleteMessage(target, deleteForBoth); selectedMessage = null; deleteForBoth = false }) { Text("Удалить", color = Color(0xFFFF6B6B)) }
                        TextButton(onClick = { selectedMessage = null; deleteForBoth = false }) { Text("Отмена") }
                    }
                }
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
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Avatar(path = profile?.avatarPath ?: chat.avatarPath, fallback = (profile?.name ?: chat.title).take(1).uppercase(), id = chat.id, size = 104, online = profile?.isOnline == true)
                    Text(profile?.status ?: "Загружаем статус…", color = if (profile?.isOnline == true) Color(0xFF35C759) else LocalAppearance.current.muted, fontSize = 13.sp)
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
            Text("‹", color = LocalAppearance.current.accent, fontSize = 34.sp, modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 9.dp))
            Avatar(path = chat.avatarPath, fallback = chat.title.take(1).uppercase(), id = chat.id, size = 42)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(chat.title, color = LocalAppearance.current.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(profile?.status ?: "Telegram", color = if (profile?.isOnline == true) Color(0xFF35C759) else LocalAppearance.current.muted, fontSize = 12.sp)
            }
            Box {
                IconButton(onClick = { chatMenuOpen = true }) { Text("⋮", color = LocalAppearance.current.text, fontSize = 24.sp) }
                DropdownMenu(expanded = chatMenuOpen, onDismissRequest = { chatMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Очистить переписку") },
                        onClick = { chatMenuOpen = false; clearForBoth = false; showClearHistoryDialog = true }
                    )
                }
            }
        }

        if (messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Пока нет сообщений", color = LocalAppearance.current.muted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(messages.sortedBy { it.date }, key = { it.id }) { message ->
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 })) {
                        MessageBubble(message, onLongPress = { selectedMessage = message })
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(LocalAppearance.current.panel.copy(alpha = 1f - LocalAppearance.current.glass * 0.30f)).padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(LocalAppearance.current.panelLight.copy(alpha = 1f - LocalAppearance.current.glass * 0.38f)).padding(horizontal = 11.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Постепенная отправка", color = LocalAppearance.current.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(if (animatedMode) "Текст появляется по частям" else "Обычная отправка", color = LocalAppearance.current.muted, fontSize = 11.sp)
                }
                Switch(checked = animatedMode, onCheckedChange = { animatedMode = it })
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
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
                        onSend(value, animatedMode)
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
private fun ProfileInfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = LocalAppearance.current.muted, fontSize = 12.sp)
        Text(value, color = LocalAppearance.current.text, fontSize = 12.sp)
    }
}

@Composable
private fun MessageBubble(message: TgMessage, onLongPress: () -> Unit) {
    val time = remember(message.date) {
        try { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.date.toLong() * 1000L)) }
        catch (_: Exception) { "" }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 310.dp).combinedClickable(onClick = {}, onLongClick = onLongPress).clip(
                RoundedCornerShape(
                    topStart = LocalAppearance.current.bubbleRadius.dp, topEnd = LocalAppearance.current.bubbleRadius.dp,
                    bottomStart = if (message.outgoing) LocalAppearance.current.bubbleRadius.dp else 4.dp,
                    bottomEnd = if (message.outgoing) 4.dp else LocalAppearance.current.bubbleRadius.dp
                )
            ).background(if (message.outgoing) LocalAppearance.current.outgoing else LocalAppearance.current.incoming).padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
            if (message.videoPath.isNotBlank()) {
                AndroidView(
                    factory = { viewContext -> VideoView(viewContext).apply {
                        setVideoPath(message.videoPath)
                        setOnPreparedListener { player -> player.isLooping = true; start() }
                    } },
                    modifier = Modifier.width(224.dp).height(224.dp).clip(messageVideoShape(message.videoShape))
                )
                Text(message.text, color = LocalAppearance.current.text, fontSize = 12.sp, lineHeight = 16.sp)
            } else Text(message.text, color = LocalAppearance.current.text, fontSize = 15.sp, lineHeight = 20.sp)
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Text(time, color = Color(0xFFB5C9D8), fontSize = 10.sp)
                if (message.outgoing) Text(if (message.isRead) "  ✓✓" else "  ✓", color = if (message.isRead) Color(0xFF79C6F5) else Color(0xFFB5C9D8), fontSize = 10.sp)
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
    Column(
        Modifier.fillMaxSize().background(LocalAppearance.current.background).padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = "Specialgram", modifier = Modifier.size(92.dp))
        Spacer(Modifier.height(25.dp))
        Text(title, color = LocalAppearance.current.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = LocalAppearance.current.muted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = value, onValueChange = onValue, modifier = Modifier.fillMaxWidth(),
            singleLine = true, visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalAppearance.current.accent, unfocusedBorderColor = LocalAppearance.current.muted,
                focusedTextColor = LocalAppearance.current.text, unfocusedTextColor = LocalAppearance.current.text
            )
        )
        Spacer(Modifier.height(17.dp))
        Button(onClick = onSubmit, enabled = value.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text(button, modifier = Modifier.padding(vertical = 5.dp))
        }
    }
}

@Composable
private fun CenterPanel(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxSize().background(LocalAppearance.current.background).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = LocalAppearance.current.accent, strokeWidth = 3.dp, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(22.dp))
        Text(title, color = LocalAppearance.current.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = LocalAppearance.current.muted, fontSize = 13.sp)
    }
}

private fun avatarColor(id: Long): Color {
    val colors = listOf(Color(0xFF5B8DEF), Color(0xFF8E6AC8), Color(0xFF26A69A), Color(0xFFE28B55), Color(0xFFCC6688))
    return colors[kotlin.math.abs(id % colors.size).toInt()]
}
