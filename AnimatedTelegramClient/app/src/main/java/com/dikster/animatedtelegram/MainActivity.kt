package com.dikster.animatedtelegram

import android.os.Bundle
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    val messages by td.messages.collectAsState()
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
                            query = query,
                            onQuery = { query = it },
                            sortNewest = sortNewest,
                            onSort = { sortNewest = it },
                            onOpen = { selectedChat = it; td.openChat(it.id) }
                        )
                    } else {
                        ConversationScreen(
                            chat = selectedChat!!,
                            messages = messages,
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
private fun ChatsScreen(
    chats: List<TgChat>, query: String, onQuery: (String) -> Unit,
    sortNewest: Boolean, onSort: (Boolean) -> Unit, onOpen: (TgChat) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val filtered = remember(chats, query, sortNewest) {
        val found = chats.filter { it.title.contains(query, ignoreCase = true) }
        if (sortNewest) found else found.sortedBy { it.title.lowercase(Locale.getDefault()) }
    }
    Column(Modifier.fillMaxSize().background(tgBackground)) {
        Row(
            Modifier.fillMaxWidth().background(tgPanel).padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(tgBlue), contentAlignment = Alignment.Center) {
                Text("✈", color = Color.White, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Telegram", color = tgText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("${chats.size} чатов", color = tgMuted, fontSize = 12.sp)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Text("⋮", color = tgText, fontSize = 25.sp) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Порядок Telegram") }, onClick = { onSort(true); menuOpen = false })
                    DropdownMenuItem(text = { Text("По названию") }, onClick = { onSort(false); menuOpen = false })
                }
            }
        }
        OutlinedTextField(
            value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            singleLine = true, placeholder = { Text("Поиск") }, leadingIcon = { Text("⌕", fontSize = 24.sp, color = tgMuted) },
            shape = RoundedCornerShape(24.dp), colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = tgPanelLight, unfocusedContainerColor = tgPanelLight,
                focusedBorderColor = tgBlue, unfocusedBorderColor = Color.Transparent,
                focusedTextColor = tgText, unfocusedTextColor = tgText
            )
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Все чаты", color = tgBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text(if (sortNewest) "Порядок Telegram" else "По алфавиту", color = tgMuted, fontSize = 12.sp)
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (chats.isEmpty()) "Загружаем чаты…" else "Ничего не найдено", color = tgMuted)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(filtered, key = { it.id }) { chat ->
                    ChatRow(chat, onClick = { onOpen(chat) })
                    HorizontalDivider(color = Color(0xFF253341), thickness = 0.6.dp, modifier = Modifier.padding(start = 82.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: TgChat, onClick: () -> Unit) {
    val initials = chat.title.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString("").uppercase()
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(avatarColor(chat.id)), contentAlignment = Alignment.Center) {
            Text(initials.ifBlank { "T" }, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(chat.title, color = tgText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("Открыть переписку", color = tgMuted, fontSize = 13.sp, maxLines = 1)
        }
        Text("›", color = tgMuted, fontSize = 25.sp)
    }
}

@Composable
private fun ConversationScreen(
    chat: TgChat, messages: List<TgMessage>, onBack: () -> Unit,
    onSend: (String, Boolean) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var animatedMode by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    Column(Modifier.fillMaxSize().background(tgBackground)) {
        Row(
            Modifier.fillMaxWidth().background(tgPanel).clickable { }.padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", color = tgBlue, fontSize = 34.sp, modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 9.dp))
            Box(Modifier.size(42.dp).clip(CircleShape).background(avatarColor(chat.id)), contentAlignment = Alignment.Center) {
                Text(chat.title.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(chat.title, color = tgText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Telegram", color = tgMuted, fontSize = 12.sp)
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
        Box(Modifier.size(82.dp).clip(CircleShape).background(tgBlue), contentAlignment = Alignment.Center) {
            Text("✈", color = Color.White, fontSize = 40.sp)
        }
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
