package com.dikster.animatedtelegram
import android.content.Context
import android.os.Build
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class TgChat(val id:Long,val title:String)
data class TgMessage(val id:Long,val text:String,val outgoing:Boolean)
sealed class Auth { object Loading:Auth(); object Phone:Auth(); object Code:Auth(); object Password:Auth(); object Ready:Auth(); data class Error(val text:String):Auth() }
class TdLibClient(context:Context) {
 private val app=context.applicationContext
 private val _auth=MutableStateFlow<Auth>(Auth.Loading); val auth=_auth.asStateFlow()
 private val _chats=MutableStateFlow<List<TgChat>>(emptyList()); val chats=_chats.asStateFlow()
 private val _messages=MutableStateFlow<List<TgMessage>>(emptyList()); val messages=_messages.asStateFlow()
 private var activeChat:Long?=null
 private val client=Client.create({ o -> onUpdate(o) }, { e -> _auth.value=Auth.Error(e.message ?: "TDLib error") }, { e -> _auth.value=Auth.Error(e.message ?: "TDLib error") })
 init { if(BuildConfig.TELEGRAM_API_ID.isBlank() || BuildConfig.TELEGRAM_API_HASH.isBlank()) _auth.value=Auth.Error("Set TELEGRAM_API_ID and TELEGRAM_API_HASH in GitHub Actions secrets.") }
 private fun onUpdate(o:TdApi.Object) { when(o) {
  is TdApi.UpdateAuthorizationState -> onAuth(o.authorizationState)
  is TdApi.UpdateNewChat -> putChat(o.chat)
  is TdApi.UpdateChatTitle -> _chats.value=_chats.value.map { if(it.id==o.chatId) TgChat(it.id,o.title) else it }
  is TdApi.UpdateNewMessage -> { val m=o.message; val t=(m.content as? TdApi.MessageText)?.text?.text ?: ""; if(m.chatId==activeChat) _messages.value=(_messages.value+TgMessage(m.id,t,m.isOutgoing)).distinctBy{it.id}; }
 } }
 private fun onAuth(a:TdApi.AuthorizationState) { when(a) {
  is TdApi.AuthorizationStateWaitTdlibParameters -> { val d=File(app.filesDir,"tdlib"); val f=File(app.filesDir,"tdlib-files"); d.mkdirs(); f.mkdirs(); send(TdApi.SetTdlibParameters(false,d.absolutePath,f.absolutePath,ByteArray(0),true,true,true,true,BuildConfig.TELEGRAM_API_ID.toInt(),BuildConfig.TELEGRAM_API_HASH,"ru",Build.MANUFACTURER+" "+Build.MODEL,Build.VERSION.RELEASE,"1.0.0",true,false)) }
  is TdApi.AuthorizationStateWaitPhoneNumber -> _auth.value=Auth.Phone
  is TdApi.AuthorizationStateWaitCode -> _auth.value=Auth.Code
  is TdApi.AuthorizationStateWaitPassword -> _auth.value=Auth.Password
  is TdApi.AuthorizationStateReady -> { _auth.value=Auth.Ready; loadChats() }
  is TdApi.AuthorizationStateClosed -> _auth.value=Auth.Error("TDLib connection closed")
 } }
 fun phone(v:String)=send(TdApi.SetAuthenticationPhoneNumber(v.trim(),null))
 fun code(v:String)=send(TdApi.CheckAuthenticationCode(v.trim()))
 fun password(v:String)=send(TdApi.CheckAuthenticationPassword(v))
 fun loadChats()=send(TdApi.GetChats(TdApi.ChatListMain(),100)) { r -> if(r is TdApi.Chats) r.chatIds.forEach { id -> send(TdApi.GetChat(id)) { c -> if(c is TdApi.Chat) putChat(c) } } }
 private fun putChat(c:TdApi.Chat) { _chats.value=(_chats.value.filterNot{it.id==c.id}+TgChat(c.id,c.title)).sortedBy{it.title.lowercase()} }
 fun openChat(id:Long) { activeChat=id; _messages.value=emptyList(); send(TdApi.GetChatHistory(id,0,0,50,false)) { r -> if(r is TdApi.Messages) _messages.value=r.messages.map{TgMessage(it.id,(it.content as? TdApi.MessageText)?.text?.text ?: "",it.isOutgoing)}.sortedBy{it.id} } }
 fun sendAnimated(id:Long,text:String) { val words=text.trim().split(Regex("\\s+")); if(words.isEmpty())return; send(TdApi.SendMessage(id,0,null,null,null,TdApi.InputMessageText(TdApi.FormattedText(text.trim(),null),null,true))) { r -> if(r is TdApi.Message && words.size>1) { val scheduler=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(); var prefix=""; words.dropLast(1).forEachIndexed { i,w -> prefix=if(prefix.isEmpty()) w else prefix+" "+w; val p=prefix; scheduler.schedule({send(TdApi.EditMessageText(id,r.id,null,TdApi.InputMessageText(TdApi.FormattedText(p,null),null,true)))},(i+1)*700L,java.util.concurrent.TimeUnit.MILLISECONDS) }; scheduler.schedule({scheduler.shutdown()},words.size*700L,java.util.concurrent.TimeUnit.MILLISECONDS) } } }
 private fun send(f:TdApi.Function<*>,handler:((TdApi.Object)->Unit)?=null) { client.send(f,Client.ResultHandler { r -> handler?.invoke(r); if(r is TdApi.Error) _auth.value=Auth.Error(r.message) }) }
 fun close(){client.send(TdApi.Close(),null)}
}