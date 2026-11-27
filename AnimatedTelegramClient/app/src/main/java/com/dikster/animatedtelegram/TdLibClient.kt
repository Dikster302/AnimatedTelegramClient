package com.dikster.animatedtelegram
import android.content.Context
import android.os.Build
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class TgChat(val id:Long,val title:String,val preview:String="",val lastDate:Int=0,val unreadCount:Int=0)
data class TgMessage(val id:Long,val text:String,val outgoing:Boolean,val date:Int)
sealed class Auth { object Loading:Auth(); object Phone:Auth(); object Code:Auth(); object Password:Auth(); object Ready:Auth(); data class Error(val text:String):Auth() }
class TdLibClient(context:Context) {
 private val app=context.applicationContext
 private val _auth=MutableStateFlow<Auth>(Auth.Loading); val auth=_auth.asStateFlow()
 private val _chats=MutableStateFlow<List<TgChat>>(emptyList()); val chats=_chats.asStateFlow()
 private val _messages=MutableStateFlow<List<TgMessage>>(emptyList()); val messages=_messages.asStateFlow()
 private var activeChat:Long?=null
 private val pendingEdits=java.util.concurrent.ConcurrentHashMap<Long,List<String>>()
 private val confirmedIds=java.util.concurrent.ConcurrentHashMap<Long,Long>()
 private val _operationError=MutableStateFlow<String?>(null); val operationError=_operationError.asStateFlow()
 private val client=Client.create({ o -> onUpdate(o) }, { e -> _auth.value=Auth.Error(e.message ?: "TDLib error") }, { e -> _auth.value=Auth.Error(e.message ?: "TDLib error") })
 init { if(BuildConfig.TELEGRAM_API_ID.isBlank() || BuildConfig.TELEGRAM_API_HASH.isBlank()) _auth.value=Auth.Error("Set TELEGRAM_API_ID and TELEGRAM_API_HASH in GitHub Actions secrets.") }
 private fun onUpdate(o:TdApi.Object) { when(o) {
  is TdApi.UpdateAuthorizationState -> onAuth(o.authorizationState)
  is TdApi.UpdateNewChat -> putChat(o.chat)
  is TdApi.UpdateChatTitle -> _chats.value=_chats.value.map { if(it.id==o.chatId) it.copy(title=o.title) else it }
  is TdApi.UpdateChatLastMessage -> updateChatPreview(o.chatId,o.lastMessage)
  is TdApi.UpdateNewMessage -> { val m=o.message; val t=(m.content as? TdApi.MessageText)?.text?.text ?: ""; if(m.chatId==activeChat) _messages.value=(_messages.value+TgMessage(m.id,t,m.isOutgoing,m.date)).distinctBy{it.id}.sortedBy{it.date}; }
  is TdApi.UpdateMessageSendSucceeded -> { val oldId=o.oldMessageId; val newId=o.message.id; confirmedIds[oldId]=newId; pendingEdits.remove(oldId)?.let { scheduleEdits(o.message.chatId,newId,it) } }
 } }
 private fun onAuth(a:TdApi.AuthorizationState) { when(a) {
  is TdApi.AuthorizationStateWaitTdlibParameters -> { val d=File(app.filesDir,"tdlib"); val f=File(app.filesDir,"tdlib-files"); d.mkdirs(); f.mkdirs(); val p=TdApi.SetTdlibParameters(); p.useTestDc=false; p.databaseDirectory=d.absolutePath; p.filesDirectory=f.absolutePath; p.databaseEncryptionKey=databaseKey(); p.useFileDatabase=true; p.useChatInfoDatabase=true; p.useMessageDatabase=true; p.useSecretChats=true; p.apiId=BuildConfig.TELEGRAM_API_ID.toInt(); p.apiHash=BuildConfig.TELEGRAM_API_HASH; p.systemLanguageCode="ru"; p.deviceModel=Build.MANUFACTURER+" "+Build.MODEL; p.systemVersion=Build.VERSION.RELEASE; p.applicationVersion="1.0.0"; send(p) }
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
 private fun putChat(c:TdApi.Chat) {
  val last=c.lastMessage
  val preview=last?.let { messagePreview(it.content) } ?: ""
  val item=TgChat(c.id,c.title,preview,last?.date ?: 0,c.unreadCount)
  _chats.value=(_chats.value.filterNot{it.id==c.id}+item).sortedByDescending{it.lastDate}
 }
 private fun updateChatPreview(id:Long,message:TdApi.Message?) {
  _chats.value=_chats.value.map { if(it.id==id) it.copy(preview=message?.let{m->messagePreview(m.content)} ?: it.preview,lastDate=message?.date ?: it.lastDate) else it }.sortedByDescending{it.lastDate}
 }
 private fun messagePreview(content:TdApi.MessageContent):String = when(content) {
  is TdApi.MessageText -> content.text.text
  is TdApi.MessagePhoto -> "📷 Фото"
  is TdApi.MessageVideo -> "🎥 Видео"
  is TdApi.MessageDocument -> "📎 Файл"
  is TdApi.MessageSticker -> "Стикер"
  is TdApi.MessageVoiceNote -> "🎤 Голосовое сообщение"
  is TdApi.MessageAnimation -> "GIF"
  is TdApi.MessageAudio -> "🎵 Аудио"
  else -> "Сообщение"
 }
 fun openChat(id:Long) { activeChat=id; _messages.value=emptyList(); send(TdApi.GetChatHistory(id,0,0,50,false)) { r -> if(r is TdApi.Messages) _messages.value=r.messages.map{TgMessage(it.id,(it.content as? TdApi.MessageText)?.text?.text ?: "",it.isOutgoing,it.date)}.sortedBy{it.date} } }
 fun sendPlain(id:Long,text:String) { val value=text.trim(); if(value.isEmpty())return; send(TdApi.SendMessage(id,null,null,null,null,TdApi.InputMessageText(TdApi.FormattedText(value,null),null,true))) }
 fun sendAnimated(id:Long,text:String) {
  val words=text.trim().split(Regex("\\s+")).filter{it.isNotBlank()}
  if(words.isEmpty())return
  send(TdApi.SendMessage(id,null,null,null,null,TdApi.InputMessageText(TdApi.FormattedText(words.first(),null),null,true))) { r ->
   if(r is TdApi.Message && words.size>1) {
    val serverId=confirmedIds[r.id]
    if(serverId!=null) scheduleEdits(id,serverId,words)
    else pendingEdits[r.id]=words
   }
  }
 }
 private fun scheduleEdits(chatId:Long,messageId:Long,words:List<String>) {
  if(words.size<2)return
  val scheduler=java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
  for(i in 1 until words.size) {
   val value=words.take(i+1).joinToString(" ")
   scheduler.schedule({
    send(TdApi.EditMessageText(chatId,messageId,null,TdApi.InputMessageText(TdApi.FormattedText(value,null),null,true)))
   },i*850L,java.util.concurrent.TimeUnit.MILLISECONDS)
  }
  scheduler.schedule({scheduler.shutdown()},words.size*850L,java.util.concurrent.TimeUnit.MILLISECONDS)
 }
 private fun send(f:TdApi.Function<*>,handler:((TdApi.Object)->Unit)?=null) { client.send(f,Client.ResultHandler { r -> if(r is TdApi.Error) _operationError.value=r.message else handler?.invoke(r) }) }
 fun close(){client.send(TdApi.Close(),null)}
 private fun databaseKey():ByteArray { val alias="animatedtelegram_tdlib_db"; val store=java.security.KeyStore.getInstance("AndroidKeyStore").apply{load(null)}; var secret=store.getKey(alias,null) as? javax.crypto.SecretKey; if(secret==null){val gen=javax.crypto.KeyGenerator.getInstance("AES","AndroidKeyStore");gen.init(android.security.keystore.KeyGenParameterSpec.Builder(alias,3).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());secret=gen.generateKey()}; val prefs=app.getSharedPreferences("tdlib_secure",Context.MODE_PRIVATE); val saved=prefs.getString("dbkey",null); if(saved!=null){val bytes=android.util.Base64.decode(saved,0);val cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");cipher.init(2,secret,javax.crypto.spec.GCMParameterSpec(128,bytes.copyOfRange(0,12)));return cipher.doFinal(bytes.copyOfRange(12,bytes.size))}; val raw=ByteArray(32).also{java.security.SecureRandom().nextBytes(it)};val cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");cipher.init(1,secret);prefs.edit().putString("dbkey",android.util.Base64.encodeToString(cipher.iv+cipher.doFinal(raw),0)).apply();return raw }

}