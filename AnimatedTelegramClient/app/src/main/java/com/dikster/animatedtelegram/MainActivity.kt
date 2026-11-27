package com.dikster.animatedtelegram
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
private val bg=Color(0xFF17212B); private val panel=Color(0xFF202B36); private val blue=Color(0xFF229ED9)
class MainActivity:ComponentActivity(){private lateinit var td:TdLibClient;override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);td=TdLibClient(this);setContent{MaterialTheme(colorScheme=darkColorScheme(primary=blue,background=bg,surface=panel)){TelegramApp(td)}}};override fun onDestroy(){if(::td.isInitialized)td.close();super.onDestroy()}}
@Composable private fun TelegramApp(td:TdLibClient){val auth by td.auth.collectAsState();val chats by td.chats.collectAsState();val messages by td.messages.collectAsState();var phone by remember{mutableStateOf("")};var code by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")};var selected by remember{mutableStateOf<TgChat?>(null)};var draft by remember{mutableStateOf("")}
 if(selected!=null){Column(Modifier.fillMaxSize().background(bg)){Text("‹   "+selected!!.title,color=Color.White,fontSize=21.sp,modifier=Modifier.fillMaxWidth().background(panel).clickable{selected=null}.padding(16.dp));LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(8.dp)){items(messages,key={it.id}){m->Text(m.text,color=Color.White,modifier=Modifier.fillMaxWidth().padding(8.dp))}};Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(draft,{draft=it},Modifier.weight(1f),placeholder={Text("Сообщение")});Button(onClick={val s=draft;draft="";td.sendAnimated(selected!!.id,s)},enabled=draft.isNotBlank()){Text("➤")}}};return}
 Column(Modifier.fillMaxSize().background(bg)){Text("Telegram",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth().background(panel).padding(18.dp));when(val s=auth){Auth.Loading->Center("Подключение к Telegram…");Auth.Phone->AuthForm("Номер телефона",phone,{phone=it},"Продолжить",{td.phone(phone)});Auth.Code->AuthForm("Код подтверждения",code,{code=it},"Подтвердить",{td.code(code)});Auth.Password->AuthForm("Облачный пароль",pass,{pass=it},"Войти",{td.password(pass)},true);is Auth.Error->Center(s.text);Auth.Ready->if(chats.isEmpty())Center("Загружаем чаты…")else LazyColumn{items(chats,key={it.id}){c->Column(Modifier.fillMaxWidth().clickable{selected=c;td.openChat(c.id)}.padding(16.dp)){Text(c.title,color=Color.White,fontWeight=FontWeight.SemiBold)};HorizontalDivider(color=Color.DarkGray)}}}}
 }}
@Composable private fun AuthForm(label:String,value:String,change:(String)->Unit,button:String,submit:()->Unit,secret:Boolean=false){Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(label,color=Color.White,fontSize=22.sp);Spacer(Modifier.height(16.dp));OutlinedTextField(value,change,Modifier.fillMaxWidth(),singleLine=true,visualTransformation=if(secret)PasswordVisualTransformation()else androidx.compose.ui.text.input.VisualTransformation.None);Button(submit,enabled=value.isNotBlank(),modifier=Modifier.fillMaxWidth().padding(top=12.dp)){Text(button)}}}
@Composable private fun Center(t:String){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(t,color=Color.LightGray,modifier=Modifier.padding(24.dp))}}
