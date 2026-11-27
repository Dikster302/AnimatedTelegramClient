package com.dikster.animatedtelegram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TelegramBlue = Color(0xFF229ED9)
private val AppBg = Color(0xFF17212B)
private val Panel = Color(0xFF202B36)
private val Muted = Color(0xFF8B9AA8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = TelegramBlue, background = AppBg, surface = Panel)) { TelegramShell() } }
    }
}

private data class DemoChat(val name: String, val message: String, val time: String, val color: Color)

@Composable
private fun TelegramShell() {
    val chats = listOf(
        DemoChat("Избранное", "Сохраняйте важные сообщения", "18:42", Color(0xFF4C9CE2)),
        DemoChat("Telegram", "Добро пожаловать!", "17:10", Color(0xFF2AABEE)),
        DemoChat("Заметки", "Ваши личные заметки", "Вчера", Color(0xFF8E85EE))
    )
    var selected by remember { mutableStateOf<String?>(null) }
    var phone by remember { mutableStateOf("") }
    var showLogin by remember { mutableStateOf(false) }
    if (selected != null) {
        ChatScreen(title = selected!!, onBack = { selected = null })
    } else {
        Column(Modifier.fillMaxSize().background(AppBg)) {
            Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 18.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("☰", color = Color.White, fontSize = 23.sp)
                Spacer(Modifier.width(22.dp))
                Text("Telegram", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("⌕", color = Muted, fontSize = 27.sp)
            }
            if (showLogin) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Text("Вход в Telegram", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("Введите номер телефона. Авторизация через TDLib будет подключена в следующем этапе сборки.", color = Muted)
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Номер телефона") }, singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { }, enabled = phone.isNotBlank()) { Text("Продолжить") }
                    TextButton(onClick = { showLogin = false }) { Text("Назад") }
                }
            } else {
                LazyColumn {
                    item { Row(Modifier.fillMaxWidth().clickable { showLogin = true }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(52.dp).clip(CircleShape).background(TelegramBlue), contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 28.sp) }; Spacer(Modifier.width(14.dp)); Column { Text("Добавить аккаунт", color = Color.White, fontWeight = FontWeight.Medium); Text("Подключить Telegram", color = Muted, fontSize = 13.sp) } } }
                    items(chats) { chat ->
                        Row(Modifier.fillMaxWidth().clickable { selected = chat.name }.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(54.dp).clip(CircleShape).background(chat.color), contentAlignment = Alignment.Center) { Text(chat.name.take(1), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(13.dp))
                            Column(Modifier.weight(1f)) { Text(chat.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Text(chat.message, color = Muted, fontSize = 14.sp, maxLines = 1) }
                            Text(chat.time, color = Muted, fontSize = 12.sp)
                        }
                        HorizontalDivider(color = Color(0xFF263441), thickness = 0.5.dp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("Демонстрационный интерфейс · TDLib ещё не подключена", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(10.dp))
            }
        }
    }
}

@Composable
private fun ChatScreen(title: String, onBack: () -> Unit) {
    var message by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(Modifier.fillMaxWidth().background(Panel).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = TelegramBlue, fontSize = 30.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(16.dp)); Column { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text("был(а) недавно", color = Muted, fontSize = 12.sp) }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Здесь будут сообщения", color = Muted) }
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.weight(1f), placeholder = { Text("Сообщение") }, maxLines = 4)
            Spacer(Modifier.width(8.dp)); Button(onClick = { message = "" }, enabled = message.isNotBlank()) { Text("➤") }
        }
    }
}
