package com.dikster.animatedtelegram

import android.os.Bundle
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
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

private val tgBackground = Color(0xFF0E1621)
private val tgPanel = Color(0xFF17212B)
private val tgPanelLight = Color(0xFF202B36)
private val tgBlue = Color(0xFF2AABEE)
private val tgOutgoing = Color(0xFF2B5278)
private val tgIncoming = Color(0xFF182533)
private val tgText = Color(0xFFEAF2F8)
private val tgMuted = Color(0xFF8A9AA9)

class MainActivity : ComponentActivity() {
    private lateinit var td: TdLibClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(23, 33, 43)
        window.navigationBarColor = android.graphics.Color.rgb(14, 22, 33)
        td = TdLibClient(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = tgBlue, background = tgBackground, surface = tgPanel,
                onBackground = tgText, onSurface = tgText
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
    val auth by td.auth.collectAsState()
    val chats by td.chats.collectAsState()
    val contacts by td.contacts.collectAsState()
    val messages by td.messages.collectAsState()
    val profile by td.profile.collectAsState()\n    val account by td.account.collectAsState()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedChat by remember { mutableStateOf<TgChat?>(null) }
    var query by remember { mutableStateOf("") }
    var sortNewest by remember { mutableStateOf(true) }

    Surface(Modifier.fillMaxSize(), color = tgBackground) {
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
                            }
                        )
                    }
                }
                is Auth.Error -> CenterPanel("Не удалось подключиться", state.text)
            }
        }
    }
}

@Composable
private fun ChatsScreen(chats: List<TgChat>, contacts: List<TgContact>, query: String, onQuery: (String) -> Unit, sortNewest: Boolean, onSort: (Boolean) -> Unit, onOpen: (TgChat) -> Unit, onOpenContact: (TgContact) -> Unit, onArchive: (TgChat) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var unreadOnly by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf("Все") }
    val filtered = remember(chats, query, sortNewest, unreadOnly, folder) {
        val found = chats.filter { chat -> chat.title.contains(query, ignoreCase = true) && (!unreadOnly || chat.unreadCount > 0) && when(folder) { "Архив" -> chat.archived; "Личные" -> !chat.archived && chat.kind == "Личный чат"; "Группы" -> !chat.archived && chat.kind.contains("Группа"); "Каналы" -> !chat.archived && chat.kind == "Канал или группа"; else -> !chat.archived } }
        if (sortNewest) found.sortedByDescending { it.lastDate } else found.sortedBy { it.title.lowercase(Locale.getDefault()) }
    }
    Column(Modifier.fillMaxSize().background(tgBackground)) {
        Row(Modifier.fillMaxWidth().background(tgPanel).padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = "Animated Telegram", modifier = Modifier.size(42.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (selectedTab == 2) "Настройки" else if (selectedTab == 1) "Контакты" else "Telegram", color = tgText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${chats.size} чатов", color = tgMuted, fontSize = 12.sp)
            }
            if (selectedTab != 2) Box {
                IconButton(onClick = { menuOpen = true }) { Text("⋮", color = tgText, fontSize = 25.sp) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Сначала новые") }, onClick = { onSort(true); menuOpen = false })
                    DropdownMenuItem(text = { Text("По названию: А — Я") }, onClick = { onSort(false); menuOpen = false })
                    DropdownMenuItem(text = { Text(if (unreadOnly) "Показать все чаты" else "Только непрочитанные") }, onClick = { unreadOnly = !unreadOnly; menuOpen = false })
                }
            }
        }
        if (selectedTab == 2) {
            Box(Modifier.weight(1f).fillMaxWidth()) { SettingsPanel(account) }
        } else if (selectedTab == 1) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                ContactsPanel(contacts.filter { it.name.contains(query, true) || it.username.contains(query, true) || it.phone.contains(query) }, query, onQuery, onOpenContact)
            }
        } else {
            OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), singleLine = true,
                placeholder = { Text("Поиск") }, leadingIcon = { Text("⌕", fontSize = 24.sp, color = tgMuted) }, shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = tgPanelLight, unfocusedContainerColor = tgPanelLight, focusedBorderColor = tgBlue, unfocusedBorderColor = Color.Transparent, focusedTextColor = tgText, unfocusedTextColor = tgText))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Все", "Личные", "Группы", "Каналы", "Архив").forEach { item -> FilterChip(selected = folder == item, onClick = { folder = item }, label = { Text(item) }) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (selectedTab == 1) "Все контакты" else "Все чаты", color = tgBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text(if (unreadOnly) "Непрочитанные" else if (sortNewest) "По активности" else "А — Я", color = tgMuted, fontSize = 12.sp)
            }
            if (filtered.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(if (chats.isEmpty()) "Загружаем чаты…" else "Ничего не найдено", color = tgMuted) }
            else LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(filtered, key = { it.id }) { chat ->
                    ChatRow(chat, onArchive = { onArchive(chat) }) { onOpen(chat) }
                    HorizontalDivider(color = Color(0xFF253341), thickness = 0.6.dp, modifier = Modifier.padding(start = 82.dp))
                }
            }
        }
        NavigationBar(containerColor = tgPanel, contentColor = tgBlue, tonalElevation = 0.dp) {
            NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Text("☷", fontSize = 20.sp) }, label = { Text("Чаты") })
            NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Text("♙", fontSize = 20.sp) }, label = { Text("Контакты") })
            NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Text("⚙", fontSize = 20.sp) }, label = { Text("Настройки") })
        }
    }
}

@Composable
private fun ContactsPanel(contacts: List<TgContact>, query: String, onQuery: (String) -> Unit, onOpen: (TgContact) -> Unit) {
    Column(Modifier.fillMaxSize().background(tgBackground)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Контакты", color = tgText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(contacts.size.toString(), color = tgMuted, fontSize = 13.sp)
        }
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), singleLine = true,
            placeholder = { Text("Поиск контактов") }, shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = tgPanelLight, unfocusedContainerColor = tgPanelLight, focusedBorderColor = tgBlue, unfocusedBorderColor = Color.Transparent, focusedTextColor = tgText, unfocusedTextColor = tgText))
        if (contacts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Контакты не найдены", color = tgMuted)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(contacts, key = { it.id }) { contact ->
                    Row(Modifier.fillMaxWidth().clickable { onOpen(contact) }.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(path = "", fallback = contact.name.take(1).uppercase(), id = contact.id, size = 48, online = contact.isOnline)
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(contact.name, color = tgText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (contact.isPremium) Text("  ✦", color = Color(0xFFB58CFF), fontSize = 12.sp)
                            }
                            Text(if (contact.isOnline) "в сети" else contact.status.ifBlank { contact.username.takeIf { it.isNotBlank() }?.let { "@$it" } ?: contact.phone }, color = if (contact.isOnline) Color(0xFF35C759) else tgMuted, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = Color(0xFF253341), thickness = 0.6.dp, modifier = Modifier.padding(start = 76.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel() {
    Column(Modifier.fillMaxSize().background(tgBackground).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
            Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = "Animated Telegram", modifier = Modifier.size(68.dp))
            Spacer(Modifier.width(16.dp))
            Column { Text("Мой аккаунт Telegram", color = tgText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text("Подключён через TDLib", color = tgMuted, fontSize = 13.sp) }
        }
        HorizontalDivider(color = Color(0xFF253341))
        SettingsLine("Уведомления", "Настройки уведомлений Telegram")
        SettingsLine("Конфиденциальность", "Управление приватностью аккаунта")
        SettingsLine("Данные и память", "Использование сети и хранилища")
        SettingsLine("Оформление", "Тёмная тема")
        SettingsLine("О приложении", "Animated Telegram · 0.3")
        Spacer(Modifier.height(12.dp))
        Text("Авторская функция", color = tgBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text("Постепенная отправка текста доступна в каждом чате — включай или выключай её переключателем над полем ввода.", color = tgMuted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun SettingsLine(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
        Text(title, color = tgText, fontSize = 15.sp)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = tgMuted, fontSize = 12.sp)
    }
}

@Composable
private fun ChatRow(chat: TgChat, onArchive: () -> Unit, onClick: () -> Unit) {
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
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(path = chat.avatarPath, fallback = initials.ifBlank { "T" }, id = chat.id, size = 56, online = chat.isOnline)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.title, color = tgText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (chat.isPremium) Text("✦", color = Color(0xFFB58CFF), fontSize = 13.sp)
                if (time.isNotBlank()) Text(time, color = if (chat.unreadCount > 0) tgBlue else tgMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.preview.ifBlank { chat.status.ifBlank { "Нет сообщений" } }, color = if (chat.isOnline) Color(0xFF35C759) else tgMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (chat.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.defaultMinSize(minWidth = 21.dp, minHeight = 21.dp).clip(CircleShape).background(tgBlue).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                        Text(if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Text("⋮", color = tgMuted, fontSize = 20.sp) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(if (chat.archived) "Вернуть из архива" else "В архив") }, onClick = { menu = false; onArchive() })
            }
        }
    }
}

@Composable
private fun Avatar(path: String, fallback: String, id: Long, size: Int, online: Boolean = false) {
    val bitmap = remember(path) { if (path.isNotBlank()) runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() else null }
    Box(Modifier.size(size.dp).clip(CircleShape).background(avatarColor(id)), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap = bitmap, contentDescription = "Аватар", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(fallback, color = Color.White, fontSize = (size / 3).sp, fontWeight = FontWeight.SemiBold)
        if (online) Box(Modifier.align(Alignment.BottomEnd).size((size / 4).dp).clip(CircleShape).background(Color(0xFF35C759)).padding(2.dp).background(tgPanel, CircleShape).padding(2.dp).background(Color(0xFF35C759), CircleShape))
    }
}

@Composable
private fun ConversationScreen(
    chat: TgChat, messages: List<TgMessage>, profile: TgProfile?, onOpenProfile: () -> Unit, onCloseProfile: () -> Unit, onBack: () -> Unit,
    onSend: (String, Boolean) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var animatedMode by remember { mutableStateOf(true) }
    var showProfile by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    Column(Modifier.fillMaxSize().background(tgBackground)) {
        if (showProfile) AlertDialog(
            onDismissRequest = { showProfile = false; onCloseProfile() },
            title = { Text(profile?.name ?: chat.title, color = tgText) },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Avatar(path = profile?.avatarPath ?: chat.avatarPath, fallback = (profile?.name ?: chat.title).take(1).uppercase(), id = chat.id, size = 104, online = profile?.isOnline == true)
                    Text(profile?.status ?: "Загружаем статус…", color = if (profile?.isOnline == true) Color(0xFF35C759) else tgMuted, fontSize = 13.sp)
                    if (profile?.isVerified == true) Text("✓ Подтверждённый аккаунт", color = tgBlue, fontSize = 12.sp)
                    if (profile?.isPremium == true) Text("✦ Telegram Premium", color = Color(0xFFB58CFF), fontWeight = FontWeight.SemiBold)
                    if (!profile?.username.isNullOrBlank()) Text("@${profile?.username}", color = tgBlue)
                    Text(profile?.kind ?: chat.kind, color = tgMuted)
                    if (!profile?.bio.isNullOrBlank()) Text(profile?.bio ?: "", color = tgText)
                    if (!profile?.phone.isNullOrBlank()) ProfileInfoLine("Телефон", profile?.phone ?: "")
                    if ((profile?.commonGroups ?: 0) > 0) ProfileInfoLine("Общие группы", (profile?.commonGroups ?: 0).toString())
                    ProfileInfoLine("ID", chat.id.toString())
                }
            },
            confirmButton = { TextButton(onClick = { showProfile = false; onCloseProfile() }) { Text("Закрыть") } }
        )
        Row(
            Modifier.fillMaxWidth().background(tgPanel).clickable { showProfile = true; onOpenProfile() }.padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", color = tgBlue, fontSize = 34.sp, modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 9.dp))
            Avatar(path = chat.avatarPath, fallback = chat.title.take(1).uppercase(), id = chat.id, size = 42)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(chat.title, color = tgText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(profile?.status ?: "Telegram", color = if (profile?.isOnline == true) Color(0xFF35C759) else tgMuted, fontSize = 12.sp)
            }
            Text("⋮", color = tgText, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 10.dp))
        }

        if (messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Пока нет сообщений", color = tgMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(messages.sortedBy { it.date }, key = { it.id }) { message ->
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 })) {
                        MessageBubble(message)
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(tgPanel).padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(tgPanelLight).padding(horizontal = 11.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Постепенная отправка", color = tgText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(if (animatedMode) "Текст появляется по частям" else "Обычная отправка", color = tgMuted, fontSize = 11.sp)
                }
                Switch(checked = animatedMode, onCheckedChange = { animatedMode = it })
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение", color = tgMuted) }, maxLines = 4,
                    shape = RoundedCornerShape(23.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tgPanelLight, unfocusedContainerColor = tgPanelLight,
                        focusedBorderColor = tgBlue, unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = tgText, unfocusedTextColor = tgText, cursorColor = tgBlue
                    )
                )
                Spacer(Modifier.width(7.dp))
                val sendScale by animateFloatAsState(if (draft.isBlank()) 0.92f else 1f, label = "send-button")
                Surface(
                    modifier = Modifier.size(49.dp).clip(CircleShape).clickable(enabled = draft.isNotBlank()) {
                        val value = draft.trim()
                        draft = ""
                        onSend(value, animatedMode)
                    },
                    color = if (draft.isBlank()) tgPanelLight else tgBlue,
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
        Text(label, color = tgMuted, fontSize = 12.sp)
        Text(value, color = tgText, fontSize = 12.sp)
    }
}

@Composable
private fun MessageBubble(message: TgMessage) {
    val time = remember(message.date) {
        try { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.date.toLong() * 1000L)) }
        catch (_: Exception) { "" }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 310.dp).clip(
                RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (message.outgoing) 16.dp else 4.dp,
                    bottomEnd = if (message.outgoing) 4.dp else 16.dp
                )
            ).background(if (message.outgoing) tgOutgoing else tgIncoming).padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
            Text(message.text, color = tgText, fontSize = 15.sp, lineHeight = 20.sp)
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Text(time, color = Color(0xFFB5C9D8), fontSize = 10.sp)
                if (message.outgoing) Text("  ✓✓", color = Color(0xFF79C6F5), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun AuthScreen(
    title: String, subtitle: String, value: String, onValue: (String) -> Unit,
    button: String, onSubmit: () -> Unit, secret: Boolean = false
) {
    Column(
        Modifier.fillMaxSize().background(tgBackground).padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = "Animated Telegram", modifier = Modifier.size(92.dp))
        Spacer(Modifier.height(25.dp))
        Text(title, color = tgText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = tgMuted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = value, onValueChange = onValue, modifier = Modifier.fillMaxWidth(),
            singleLine = true, visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = tgBlue, unfocusedBorderColor = tgMuted,
                focusedTextColor = tgText, unfocusedTextColor = tgText
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
        Modifier.fillMaxSize().background(tgBackground).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = tgBlue, strokeWidth = 3.dp, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(22.dp))
        Text(title, color = tgText, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = tgMuted, fontSize = 13.sp)
    }
}

private fun avatarColor(id: Long): Color {
    val colors = listOf(Color(0xFF5B8DEF), Color(0xFF8E6AC8), Color(0xFF26A69A), Color(0xFFE28B55), Color(0xFFCC6688))
    return colors[kotlin.math.abs(id % colors.size).toInt()]
}
