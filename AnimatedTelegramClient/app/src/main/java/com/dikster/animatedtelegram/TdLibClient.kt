package com.dikster.animatedtelegram
import android.content.Context
import android.os.Build
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat

data class TgChat(val id:Long,val title:String,val preview:String="",val lastDate:Int=0,val unreadCount:Int=0,val avatarPath:String="",val kind:String="Личный чат",val archived:Boolean=false,val status:String="",val isOnline:Boolean=false,val isPremium:Boolean=false,val lastReadOutboxMessageId:Long=0,val hasStory:Boolean=false)
data class TgMessage(val id:Long,val text:String,val outgoing:Boolean,val date:Int,val isRead:Boolean=false)
data class TgContact(val id:Long,val name:String,val username:String="",val phone:String="",val status:String="",val isOnline:Boolean=false,val isPremium:Boolean=false)
data class TgProfile(val chatId:Long,val name:String,val kind:String,val phone:String="",val bio:String="",val avatarPath:String="",val username:String="",val status:String="Статус неизвестен",val isOnline:Boolean=false,val isPremium:Boolean=false,val isVerified:Boolean=false,val commonGroups:Int=0)
data class TgAccount(val name:String,val username:String="",val phone:String="",val isPremium:Boolean=false,val isVerified:Boolean=false)
sealed class Auth { object Loading:Auth(); object Phone:Auth(); object Code:Auth(); object Password:Auth(); object Ready:Auth(); data class Error(val text:String):Auth() }
class TdLibClient(context:Context) {
 private val app=context.applicationContext
 private val _auth=MutableStateFlow<Auth>(Auth.Loading); val auth=_auth.asStateFlow()
 private val _chats=MutableStateFlow<List<TgChat>>(emptyList()); val chats=_chats.asStateFlow()
 private val _messages=MutableStateFlow<List<TgMessage>>(emptyList()); val messages=_messages.asStateFlow()
 private val _contacts=MutableStateFlow<List<TgContact>>(emptyList()); val contacts=_contacts.asStateFlow()
 private val _profile=MutableStateFlow<TgProfile?>(null); val profile=_profile.asStateFlow()
 private val _account=MutableStateFlow<TgAccount?>(null); val account=_account.asStateFlow()
 private var activeChat:Long?=null
 private var myUserId:Long?=null
 private var activeProfileUserId:Long?=null
 private val pendingEdits=java.util.concurrent.ConcurrentHashMap<Long,List<String>>()
 private val fileToChat=java.util.concurrent.ConcurrentHashMap<Int,Long>()
 private val userToChat=java.util.concurrent.ConcurrentHashMap<Long,Long>()
 private val confirmedIds=java.util.concurrent.ConcurrentHashMap<Long,Long>()
 private val _operationError=MutableStateFlow<String?>(null); val operationError=_operationError.asStateFlow()
 private val client=Client.create({ o -> onUpdate(o) }, { e -> _auth.value=Auth.Error(e.message ?: "TDLib error") }, { e -> _auth.value=Auth.Error(e.message ?: "TDLib error") })
 init { if(BuildConfig.TELEGRAM_API_ID.isBlank() || BuildConfig.TELEGRAM_API_HASH.isBlank()) _auth.value=Auth.Error("Set TELEGRAM_API_ID and TELEGRAM_API_HASH in GitHub Actions secrets.") }
 private fun onUpdate(o:TdApi.Object) { when(o) {
  is TdApi.UpdateAuthorizationState -> onAuth(o.authorizationState)
  is TdApi.UpdateNewChat -> putChat(o.chat)
  is TdApi.UpdateChatTitle -> _chats.value=_chats.value.map { if(it.id==o.chatId) it.copy(title=o.title) else it }
  is TdApi.UpdateChatActiveStories -> updateChatStories(o.chatActiveStories)
  is TdApi.UpdateUserStatus -> refreshUserStatus(o.userId,o.status)
  is TdApi.UpdateUser -> { updateUserProfile(o.user); updateChatUser(o.user); updateContact(o.user); if(myUserId==o.user.id) updateAccount(o.user) }
  is TdApi.UpdateChatLastMessage -> updateChatPreview(o.chatId,o.lastMessage)
  is TdApi.UpdateChatReadOutbox -> updateOutboxRead(o.chatId,o.lastReadOutboxMessageId)
  is TdApi.UpdateDeleteMessages -> if(o.chatId==activeChat) _messages.value=_messages.value.filterNot { it.id in o.messageIds.toSet() }
  is TdApi.UpdateChatReadInbox -> _chats.value=_chats.value.map { if(it.id==o.chatId) it.copy(unreadCount=o.unreadCount) else it }
  is TdApi.UpdateFile -> { val file=o.file; if(file.local.isDownloadingCompleted) { val chatId=fileToChat[file.id]; if(chatId!=null) { _chats.value=_chats.value.map { c -> if(c.id==chatId) c.copy(avatarPath=file.local.path) else c }; if(_profile.value?.chatId==chatId) _profile.value=_profile.value?.copy(avatarPath=file.local.path) } } }
  is TdApi.UpdateNewMessage -> { val m=o.message; val t=messageDisplayText(m.content); if(m.chatId==activeChat) { _messages.value=(_messages.value+TgMessage(m.id,t,m.isOutgoing,m.date,m.isOutgoing && m.id<=(_chats.value.firstOrNull{it.id==m.chatId}?.lastReadOutboxMessageId ?: 0L))).distinctBy{it.id}.sortedBy{it.date}; if(!m.isOutgoing) send(TdApi.ViewMessages(m.chatId,longArrayOf(m.id),null,true)) } }
  is TdApi.UpdateMessageContent -> { if(o.chatId==activeChat) { val t=messageDisplayText(o.newContent); _messages.value=_messages.value.map { if(it.id==o.messageId) it.copy(text=t) else it } } }
  is TdApi.UpdateMessageSendSucceeded -> { val oldId=o.oldMessageId; val newId=o.message.id; confirmedIds[oldId]=newId; pendingEdits.remove(oldId)?.let { scheduleEdits(o.message.chatId,newId,it) } }
 } }
 private fun onAuth(a:TdApi.AuthorizationState) { when(a) {
  is TdApi.AuthorizationStateWaitTdlibParameters -> { val d=File(app.filesDir,"tdlib"); val f=File(app.filesDir,"tdlib-files"); d.mkdirs(); f.mkdirs(); val p=TdApi.SetTdlibParameters(); p.useTestDc=false; p.databaseDirectory=d.absolutePath; p.filesDirectory=f.absolutePath; p.databaseEncryptionKey=databaseKey(); p.useFileDatabase=true; p.useChatInfoDatabase=true; p.useMessageDatabase=true; p.useSecretChats=true; p.apiId=BuildConfig.TELEGRAM_API_ID.toInt(); p.apiHash=BuildConfig.TELEGRAM_API_HASH; p.systemLanguageCode="ru"; p.deviceModel=Build.MANUFACTURER+" "+Build.MODEL; p.systemVersion=Build.VERSION.RELEASE; p.applicationVersion="1.0.0"; send(p) }
  is TdApi.AuthorizationStateWaitPhoneNumber -> _auth.value=Auth.Phone
  is TdApi.AuthorizationStateWaitCode -> _auth.value=Auth.Code
  is TdApi.AuthorizationStateWaitPassword -> _auth.value=Auth.Password
  is TdApi.AuthorizationStateReady -> { _auth.value=Auth.Ready; loadChats(); loadContacts(); send(TdApi.GetMe()) { me -> if(me is TdApi.User) updateAccount(me) } }
  is TdApi.AuthorizationStateClosed -> _auth.value=Auth.Error("TDLib connection closed")
 } }
 fun phone(v:String)=send(TdApi.SetAuthenticationPhoneNumber(v.trim(),null))
 fun code(v:String)=send(TdApi.CheckAuthenticationCode(v.trim()))
 fun password(v:String)=send(TdApi.CheckAuthenticationPassword(v))
 fun loadChats() { loadChatList(TdApi.ChatListMain(),false); loadChatList(TdApi.ChatListArchive(),true) }
 private fun loadChatList(list:TdApi.ChatList,archived:Boolean) {
  send(TdApi.GetChats(list,100)) { r -> if(r is TdApi.Chats) r.chatIds.forEach { id -> send(TdApi.GetChat(id)) { c -> if(c is TdApi.Chat) putChat(c,archived) } } }
 }
 fun archiveChat(id:Long,archived:Boolean) {
  if(archived) send(TdApi.AddChatToList(id,TdApi.ChatListArchive())) { loadChats() }
  else send(TdApi.AddChatToList(id,TdApi.ChatListMain())) { loadChats() }
 }
 fun openProfile(chatId:Long) {
  activeProfileUserId=null
  send(TdApi.GetChat(chatId)) { result ->
   if(result is TdApi.Chat) {
    val chat=result
    val kind=when(chat.type) { is TdApi.ChatTypePrivate -> "Личный чат"; is TdApi.ChatTypeBasicGroup -> "Группа"; is TdApi.ChatTypeSupergroup -> "Канал или группа"; is TdApi.ChatTypeSecret -> "Секретный чат"; else -> "Чат" }
    _profile.value=TgProfile(chatId,chat.title,kind,avatarPath=_chats.value.firstOrNull{it.id==chatId}?.avatarPath ?: "")
    if(chat.type is TdApi.ChatTypePrivate) {
     val userId=(chat.type as TdApi.ChatTypePrivate).userId
     activeProfileUserId=userId
     userToChat[userId]=chatId
     send(TdApi.GetUser(userId)) { u -> if(u is TdApi.User) {
      updateUserProfile(u); updateChatUser(u)
      val profilePhotoId=u.profilePhoto?.small?.id ?: 0
      if(profilePhotoId>0) { fileToChat[profilePhotoId]=chatId; send(TdApi.DownloadFile(profilePhotoId,32,0,0,false)) }
     } }
     send(TdApi.GetUserFullInfo(userId)) { info -> if(info is TdApi.UserFullInfo) _profile.value=_profile.value?.copy(bio=info.bio?.text ?: "",commonGroups=info.groupInCommonCount) }
    }
   }
  }
 }
 private fun updateAccount(user:TdApi.User) {
  myUserId=user.id
  val name=listOf(user.firstName,user.lastName).filter{it.isNotBlank()}.joinToString(" ")
  _account.value=TgAccount(name.ifBlank{"Мой аккаунт"},user.usernames?.activeUsernames?.firstOrNull() ?: "",user.phoneNumber,user.isPremium,user.isVerified)
 }
 private fun updateUserProfile(user:TdApi.User) {
  if(activeProfileUserId!=user.id) return
  val name=listOf(user.firstName,user.lastName).filter{it.isNotBlank()}.joinToString(" ")
  _profile.value=_profile.value?.copy(name=name.ifBlank{_profile.value?.name ?: ""},phone=user.phoneNumber,username=user.usernames?.activeUsernames?.firstOrNull() ?: "",status=userStatusText(user.status),isOnline=user.status is TdApi.UserStatusOnline,isPremium=user.isPremium,isVerified=user.isVerified)
 }
 private fun refreshUserStatus(userId:Long,status:TdApi.UserStatus) {
  _contacts.value=_contacts.value.map { if(it.id==userId) it.copy(status=userStatusText(status),isOnline=status is TdApi.UserStatusOnline) else it }
  val chatId=userToChat[userId]
  if(chatId!=null) _chats.value=_chats.value.map { if(it.id==chatId) it.copy(status=userStatusText(status),isOnline=status is TdApi.UserStatusOnline) else it }
  val current=_profile.value
  if(current!=null && chatId==current.chatId) _profile.value=current.copy(status=userStatusText(status),isOnline=status is TdApi.UserStatusOnline)
 }
 private fun updateContact(user:TdApi.User) {
  val name=listOf(user.firstName,user.lastName).filter{it.isNotBlank()}.joinToString(" ")
  val item=TgContact(user.id,name,user.usernames?.activeUsernames?.firstOrNull() ?: "",user.phoneNumber,userStatusText(user.status),user.status is TdApi.UserStatusOnline,user.isPremium)
  _contacts.value=(_contacts.value.filterNot{it.id==user.id}+item).sortedBy{it.name.lowercase(java.util.Locale.getDefault())}
 }
 fun loadContacts() {
  send(TdApi.GetContacts()) { result -> if(result is TdApi.Users) result.userIds.forEach { id -> send(TdApi.GetUser(id)) { u -> if(u is TdApi.User) updateContact(u) } } }
 }
 fun openContact(userId:Long,onReady:(TgChat)->Unit) {
  send(TdApi.CreatePrivateChat(userId,false)) { result -> if(result is TdApi.Chat) { putChat(result); onReady(_chats.value.firstOrNull{it.id==result.id} ?: TgChat(result.id,result.title,kind="Личный чат")) } }
 }
 private fun updateChatUser(user:TdApi.User) {
  val chatId=userToChat[user.id] ?: return
  _chats.value=_chats.value.map { if(it.id==chatId) it.copy(status=userStatusText(user.status),isOnline=user.status is TdApi.UserStatusOnline,isPremium=user.isPremium) else it }
 }
 private fun userStatusText(status:TdApi.UserStatus):String = when(status) {
  is TdApi.UserStatusOnline -> "в сети"
  is TdApi.UserStatusOffline -> "был(а) в сети " + SimpleDateFormat("dd.MM.yyyy в HH:mm",java.util.Locale.getDefault()).format(java.util.Date(status.wasOnline.toLong()*1000L))
  is TdApi.UserStatusRecently -> "был(а) недавно"
  is TdApi.UserStatusLastWeek -> "был(а) на этой неделе"
  is TdApi.UserStatusLastMonth -> "был(а) в этом месяце"
  else -> "давно не был(а) в сети"
 }
 fun closeProfile() { _profile.value=null; activeProfileUserId=null }
 private fun putChat(c:TdApi.Chat,archived:Boolean=false) {
  val last=c.lastMessage
  val preview=last?.let { messagePreview(it.content) } ?: ""
  val photoId=c.photo?.small?.id ?: 0
  val kind=when(c.type) { is TdApi.ChatTypePrivate -> "Личный чат"; is TdApi.ChatTypeBasicGroup -> "Группа"; is TdApi.ChatTypeSupergroup -> "Канал или группа"; is TdApi.ChatTypeSecret -> "Секретный чат"; else -> "Чат" }
  val old=_chats.value.firstOrNull{it.id==c.id}
  val item=TgChat(c.id,c.title,preview,last?.date ?: 0,c.unreadCount,old?.avatarPath ?: "",kind,archived,old?.status ?: "",old?.isOnline ?: false,old?.isPremium ?: false,c.lastReadOutboxMessageId,old?.hasStory ?: false)
  _chats.value=(_chats.value.filterNot{it.id==c.id}+item).sortedByDescending{it.lastDate}
  send(TdApi.GetChatActiveStories(c.id)) { result -> if(result is TdApi.ChatActiveStories) updateChatStories(result) }
  if(c.type is TdApi.ChatTypePrivate) {
   val userId=(c.type as TdApi.ChatTypePrivate).userId
   userToChat[userId]=c.id
   send(TdApi.GetUser(userId)) { u -> if(u is TdApi.User) updateChatUser(u) }
  }
  if(photoId>0 && item.avatarPath.isBlank()) { fileToChat[photoId]=c.id; send(TdApi.DownloadFile(photoId,32,0,0,false)) }
 }
 private fun updateChatStories(stories:TdApi.ChatActiveStories) {
  val hasStory=stories.hasUnexpiredStories || stories.stories.isNotEmpty()
  _chats.value=_chats.value.map { if(it.id==stories.chatId) it.copy(hasStory=hasStory) else it }
 }
 private fun updateChatPreview(id:Long,message:TdApi.Message?) {
  _chats.value=_chats.value.map { if(it.id==id) it.copy(preview=message?.let{m->messagePreview(m.content)} ?: it.preview,lastDate=message?.date ?: it.lastDate) else it }.sortedByDescending{it.lastDate}
 }
 private fun messageDisplayText(content:TdApi.MessageContent):String = when(content) {
  is TdApi.MessageText -> content.text.text
  is TdApi.MessagePhoto -> "📷 Фото"
  is TdApi.MessageVideo -> "🎥 Видео"
  is TdApi.MessageDocument -> "📎 ${content.document.fileName.ifBlank { "Файл" }}"
  is TdApi.MessageSticker -> "Стикер"
  is TdApi.MessageVoiceNote -> "🎤 Голосовое сообщение"
  is TdApi.MessageAnimation -> "GIF"
  is TdApi.MessageAudio -> "🎵 ${content.audio.title.ifBlank { "Аудио" }}"
  else -> "Сообщение"
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
 fun openChat(id:Long) {
  activeChat=id
  _messages.value=emptyList()
  send(TdApi.GetChat(id)) { result ->
   if(result is TdApi.Chat) {
    _chats.value=_chats.value.map { if(it.id==id) it.copy(lastReadOutboxMessageId=result.lastReadOutboxMessageId,unreadCount=result.unreadCount) else it }
    if(activeChat==id) _messages.value=_messages.value.map { m -> if(m.outgoing) m.copy(isRead=m.id<=result.lastReadOutboxMessageId) else m }
   }
  }
  send(TdApi.GetChatHistory(id,0,0,50,false)) { r ->
   if(r is TdApi.Messages) {
    val loaded=r.messages
    val readMarker=_chats.value.firstOrNull{it.id==id}?.lastReadOutboxMessageId ?: 0L
    _messages.value=loaded.map{TgMessage(it.id,messageDisplayText(it.content),it.isOutgoing,it.date,it.isOutgoing && it.id<=readMarker)}.sortedBy{it.date}
    val incomingIds=loaded.filter{!it.isOutgoing}.map{it.id}.toLongArray()
    if(incomingIds.isNotEmpty()) send(TdApi.ViewMessages(id,incomingIds,null,true))
   }
  }
 }
 private fun updateOutboxRead(chatId:Long,lastReadId:Long) {
  _chats.value=_chats.value.map { if(it.id==chatId) it.copy(lastReadOutboxMessageId=lastReadId) else it }
  if(activeChat==chatId) _messages.value=_messages.value.map { m -> if(m.outgoing) m.copy(isRead=m.id<=lastReadId) else m }
 }
 fun clearOperationError() { _operationError.value=null }
 fun deleteMessage(chatId:Long,messageId:Long,revoke:Boolean) {
  clearOperationError()
  send(TdApi.DeleteMessages(chatId,longArrayOf(messageId),revoke)) {
   _messages.value=_messages.value.filterNot { it.id==messageId }
  }
 }
 fun clearHistory(chatId:Long,revoke:Boolean) {
  clearOperationError()
  send(TdApi.DeleteChatHistory(chatId,false,revoke)) {
   if(activeChat==chatId) _messages.value=emptyList()
   _chats.value=_chats.value.map { if(it.id==chatId) it.copy(preview="",lastDate=0,unreadCount=0) else it }
  }
 }
 fun editMessage(chatId:Long,messageId:Long,text:String) {
  val value=text.trim()
  if(value.isEmpty()) return
  clearOperationError()
  send(TdApi.EditMessageText(chatId,messageId,null,TdApi.InputMessageText(TdApi.FormattedText(value,null),null,true)))
 }
 fun sendPlain(id:Long,text:String) { val value=text.trim(); if(value.isEmpty())return; clearOperationError(); send(TdApi.SendMessage(id,null,null,null,null,TdApi.InputMessageText(TdApi.FormattedText(value,null),null,true))) }
 fun sendAnimated(id:Long,text:String) {
  val chars=text.trim().codePoints().toArray().map { String(Character.toChars(it)) }
  if(chars.isEmpty())return
  clearOperationError()
  send(TdApi.SendMessage(id,null,null,null,null,TdApi.InputMessageText(TdApi.FormattedText(chars.first(),null),null,true))) { r ->
   if(r is TdApi.Message && chars.size>1) {
    val serverId=confirmedIds[r.id]
    if(serverId!=null) scheduleEdits(id,serverId,chars)
    else pendingEdits[r.id]=chars
   }
  }
 }
 private fun scheduleEdits(chatId:Long,messageId:Long,words:List<String>) {
  if(words.size<2)return
  val scheduler=java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
  for(i in 1 until words.size) {
   val value=words.take(i+1).joinToString("")
   scheduler.schedule({
    send(TdApi.EditMessageText(chatId,messageId,null,TdApi.InputMessageText(TdApi.FormattedText(value,null),null,true)))
   },i*180L,java.util.concurrent.TimeUnit.MILLISECONDS)
  }
  scheduler.schedule({scheduler.shutdown()},words.size*180L,java.util.concurrent.TimeUnit.MILLISECONDS)
 }
 private fun send(f:TdApi.Function<*>,handler:((TdApi.Object)->Unit)?=null) { client.send(f,Client.ResultHandler { r -> if(r is TdApi.Error) _operationError.value=r.message else handler?.invoke(r) }) }
 fun close(){client.send(TdApi.Close(),null)}
 private fun databaseKey():ByteArray { val alias="animatedtelegram_tdlib_db"; val store=java.security.KeyStore.getInstance("AndroidKeyStore").apply{load(null)}; var secret=store.getKey(alias,null) as? javax.crypto.SecretKey; if(secret==null){val gen=javax.crypto.KeyGenerator.getInstance("AES","AndroidKeyStore");gen.init(android.security.keystore.KeyGenParameterSpec.Builder(alias,3).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());secret=gen.generateKey()}; val prefs=app.getSharedPreferences("tdlib_secure",Context.MODE_PRIVATE); val saved=prefs.getString("dbkey",null); if(saved!=null){val bytes=android.util.Base64.decode(saved,0);val cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");cipher.init(2,secret,javax.crypto.spec.GCMParameterSpec(128,bytes.copyOfRange(0,12)));return cipher.doFinal(bytes.copyOfRange(12,bytes.size))}; val raw=ByteArray(32).also{java.security.SecureRandom().nextBytes(it)};val cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");cipher.init(1,secret);prefs.edit().putString("dbkey",android.util.Base64.encodeToString(cipher.iv+cipher.doFinal(raw),0)).apply();return raw }

}