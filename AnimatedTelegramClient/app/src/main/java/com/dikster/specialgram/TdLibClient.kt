package com.dikster.specialgram
import android.content.Context
import android.os.Build
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class TgChat(val id:Long,val title:String,val preview:String="",val lastDate:Int=0,val unreadCount:Int=0,val avatarPath:String="",val kind:String="Личный чат",val archived:Boolean=false,val status:String="",val isOnline:Boolean=false,val isPremium:Boolean=false,val lastReadOutboxMessageId:Long=0,val hasStory:Boolean=false,val hasUnviewedStory:Boolean=false,val isPinned:Boolean=false,val isMarkedAsUnread:Boolean=false,val isMuted:Boolean=false)
data class TgStory(val chatId:Long,val storyId:Int,val ownerName:String,val date:Int,val caption:String="",val isVideo:Boolean=false,val path:String="",val isViewed:Boolean=false)
data class TgReaction(val emoji:String,val count:Int,val chosen:Boolean)
data class TgMessage(val id:Long,val text:String,val outgoing:Boolean,val date:Int,val isRead:Boolean=false,val videoShape:String="circle",val videoPath:String="",val replyToMessageId:Long=0L,val replyToText:String="",val reactions:List<TgReaction> = emptyList(),val photoPath:String="",val mediaKind:String="",val mediaPath:String="",val mediaName:String="",val mediaMimeType:String="",val mediaDuration:Int=0,val mediaSize:Long=0L,val isPinned:Boolean=false)
data class TgContact(val id:Long,val name:String,val username:String="",val phone:String="",val status:String="",val isOnline:Boolean=false,val isPremium:Boolean=false)
data class TgProfile(val chatId:Long,val name:String,val kind:String,val phone:String="",val bio:String="",val avatarPath:String="",val username:String="",val status:String="Статус неизвестен",val isOnline:Boolean=false,val isPremium:Boolean=false,val isVerified:Boolean=false,val commonGroups:Int=0)
data class TgAccount(val name:String,val username:String="",val phone:String="",val isPremium:Boolean=false,val isVerified:Boolean=false,val bio:String="",val avatarPath:String="",val firstName:String="",val lastName:String="")
sealed class Auth { object Loading:Auth(); object Phone:Auth(); object Code:Auth(); object Password:Auth(); object Ready:Auth(); data class Error(val text:String):Auth() }
class TdLibClient(context:Context) {
 private val app=context.applicationContext
 private val _auth=MutableStateFlow<Auth>(Auth.Loading); val auth=_auth.asStateFlow()
 private val _chats=MutableStateFlow<List<TgChat>>(emptyList()); val chats=_chats.asStateFlow()
 private val _messages=MutableStateFlow<List<TgMessage>>(emptyList()); val messages=_messages.asStateFlow()
 private val _searchResults=MutableStateFlow<List<TgMessage>>(emptyList()); val searchResults=_searchResults.asStateFlow()
 private val _stories=MutableStateFlow<List<TgStory>>(emptyList()); val stories=_stories.asStateFlow()
 private val _contacts=MutableStateFlow<List<TgContact>>(emptyList()); val contacts=_contacts.asStateFlow()
 private val _profile=MutableStateFlow<TgProfile?>(null); val profile=_profile.asStateFlow()
 private val _account=MutableStateFlow<TgAccount?>(null); val account=_account.asStateFlow()
 private val _privacyLastSeen=MutableStateFlow("Загружаем…"); val privacyLastSeen=_privacyLastSeen.asStateFlow()
 private val _privacyPhone=MutableStateFlow("Загружаем…"); val privacyPhone=_privacyPhone.asStateFlow()
 private val _privacyPhoto=MutableStateFlow("Загружаем…"); val privacyPhoto=_privacyPhoto.asStateFlow()
 private var activeChat:Long?=null
 private var chatSearchGeneration=0
 private val historyLoadingChats=java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()
 private val historyExhaustedChats=java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()
 private val animatedMessageController = TdAnimatedMessageController(
  TdAnimatedMessageController.Requester { function, callback -> send(function, handler={ callback?.onResult(it) }, reportErrorToUi=true) }
 )
 private val fileDownloadQueue = TdFileDownloadQueue(
  TdFileDownloadQueue.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) },
    reportErrorToUi=false)
  }
 )
 private val storyQueue = ChatStoryRequestQueue(
  ChatStoryRequestQueue.Requester { chatId, callback ->
   send(TdApi.GetChatActiveStories(chatId),
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) },
    reportErrorToUi=false)
  },
  ChatStoryRequestQueue.ResultListener { result ->
   if(result is TdApi.ChatActiveStories) updateChatStories(result)
  }
 )
 private var myUserId:Long?=null
 private var myPhotoFileId:Int?=null
 private var activeProfileUserId:Long?=null
 private val pendingEdits=java.util.concurrent.ConcurrentHashMap<Long,List<String>>()
 private val fileToChat=java.util.concurrent.ConcurrentHashMap<Int,Long>()
 private val videoFileToMessage=java.util.concurrent.ConcurrentHashMap<Int,Long>()
  private val mediaFileToMessage=java.util.concurrent.ConcurrentHashMap<Int,Long>()
 private val storyFileToKey=java.util.concurrent.ConcurrentHashMap<Int,Pair<Long,Int>>()
 private val userToChat=java.util.concurrent.ConcurrentHashMap<Long,Long>()
 private val confirmedIds=java.util.concurrent.ConcurrentHashMap<Long,Long>()
 private val _operationError=MutableStateFlow<String?>(null); val operationError=_operationError.asStateFlow()
 private val client=Client.create(
  { o -> onUpdate(o) },
  { e ->
   FirebaseCrashlytics.getInstance().apply {
    setCustomKey("tdlib_callback", "update_error")
    recordException(IllegalStateException("TDLib update callback failed"))
   }
   _auth.value=Auth.Error(e.message ?: "TDLib error")
  },
  { e ->
   FirebaseCrashlytics.getInstance().apply {
    setCustomKey("tdlib_callback", "fatal_error")
    recordException(IllegalStateException("TDLib fatal callback failed"))
   }
   _auth.value=Auth.Error(e.message ?: "TDLib error")
  }
 )
 private val userActions = TdUserActionsController(
  TdUserActionsController.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) })
  }
 )
 private val accountActions = TdAccountActionsController(
  TdAccountActionsController.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) })
  },
  object : TdAccountActionsController.Listener {
   override fun onAccountUser(user:TdApi.User) { updateAccount(user) }
   override fun onBioUpdated(bio:String) { _account.value=_account.value?.copy(bio=bio) }
   override fun onValidationError(message:String) { _operationError.value=message }
  }
 )
 private val chatHistoryController = TdChatHistoryController(
  TdChatHistoryController.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) },
    reportErrorToUi=false)
  }
 )
 private val storyContentQueue = TdStoryContentQueue(
  TdStoryContentQueue.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) },
    reportErrorToUi=false)
  },
  TdStoryContentQueue.Listener { story, viewed -> updateStory(story,viewed) }
 )
 private val storyActions = TdStoryActionsController(
  TdStoryActionsController.Requester { function, callback ->
   send(function,
    handler={ callback?.onResult(it) },
    onError={ callback?.onError(it) },
    reportErrorToUi=false)
  },
  object : TdStoryActionsController.Listener {
   override fun onStoryPosted(story:TdApi.Story) {
    updateStory(story,false)
    val userId=myUserId
    if(userId!=null) _chats.value=_chats.value.map { if(it.id==userId) it.copy(hasStory=true,hasUnviewedStory=true) else it }
   }
   override fun onValidationError(message:String) { reportOperationError(message) }
  }
 )
 private val messageActions = TdMessageActionsController(
  TdMessageActionsController.Requester { function, callback ->
   send(function,
    handler={ callback?.onResult(it) },
    onError={ callback?.onError(it) },
    reportErrorToUi=false)
  },
  object : TdMessageActionsController.Listener {
   override fun onMessageDeleted(messageId:Long) {
    _messages.value=_messages.value.filterNot { it.id==messageId }
   }
   override fun onHistoryCleared(chatId:Long) {
    if(activeChat==chatId) _messages.value=emptyList()
    _chats.value=_chats.value.map { if(it.id==chatId) it.copy(preview="",lastDate=0,unreadCount=0) else it }
   }
  }
 )
 private val pushRegistrationController = TdPushRegistrationController(
  app,
  TdPushRegistrationController.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) },
    reportErrorToUi=false)
  }
 )
 private val privacyController = TdPrivacyController(
  TdPrivacyController.Requester { function, callback ->
   send(function, handler={ callback.onResult(it) }, onError={ callback.onError(it) })
  },
  TdPrivacyController.Listener { type, value ->
   when(type) {
    TdPrivacyController.STATUS -> _privacyLastSeen.value=value
    TdPrivacyController.PHONE -> _privacyPhone.value=value
    TdPrivacyController.PHOTO -> _privacyPhoto.value=value
   }
  }
 )
 private val chatActions = TdChatActions(
  TdChatActions.Requester { function, callback ->
   send(function, handler={ callback.onResult(it) }, onError={ callback.onError(it) })
  },
  TdChatActions.Listener { loadChats() }
 )
 private val contactsController = TdContactsController(
  TdContactsController.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) },
    reportErrorToUi=false)
  },
  TdContactsController.Listener { user ->
   if(user is TdApi.User) { updateContact(user); updateChatUser(user) }
  }
 )
 private val chatListLoader = TdChatListLoader(
  TdChatListLoader.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) },
    reportErrorToUi=false)
  },
  TdChatListLoader.ChatListener { chat, archived -> putChat(chat,archived) }
 )

 init { if(BuildConfig.TELEGRAM_API_ID.isBlank() || BuildConfig.TELEGRAM_API_HASH.isBlank()) _auth.value=Auth.Error("Set TELEGRAM_API_ID and TELEGRAM_API_HASH in GitHub Actions secrets.") }
 private fun onUpdate(o:TdApi.Object) { when(o) {
  is TdApi.UpdateAuthorizationState -> onAuth(o.authorizationState)
  is TdApi.UpdateNewChat -> putChat(o.chat)
  is TdApi.UpdateChatTitle -> _chats.value=_chats.value.map { if(it.id==o.chatId) it.copy(title=o.title) else it }
  is TdApi.UpdateChatActiveStories -> updateChatStories(o.activeStories)
  is TdApi.UpdateStory -> updateStory(o.story)
  is TdApi.UpdateStoryDeleted -> {
   _stories.value=_stories.value.filterNot { it.chatId==o.storyPosterChatId && it.storyId==o.storyId }
   val remaining=_stories.value.filter { it.chatId==o.storyPosterChatId }
   _chats.value=_chats.value.map { if(it.id==o.storyPosterChatId) it.copy(hasStory=remaining.isNotEmpty(),hasUnviewedStory=remaining.any { story -> !story.isViewed }) else it }
  }
  is TdApi.UpdateUserStatus -> refreshUserStatus(o.userId,o.status)
  is TdApi.UpdateUserPrivacySettingRules -> when(o.setting) {
   is TdApi.UserPrivacySettingShowStatus -> _privacyLastSeen.value=TdPrivacyController.label(o.rules)
   is TdApi.UserPrivacySettingShowPhoneNumber -> _privacyPhone.value=TdPrivacyController.label(o.rules)
   is TdApi.UserPrivacySettingShowProfilePhoto -> _privacyPhoto.value=TdPrivacyController.label(o.rules)
  }
  is TdApi.UpdateUser -> { updateUserProfile(o.user); updateChatUser(o.user); updateContact(o.user); if(myUserId==o.user.id) updateAccount(o.user) }
  is TdApi.UpdateChatLastMessage -> updateChatPreview(o.chatId,o.lastMessage)
  is TdApi.UpdateChatReadOutbox -> updateOutboxRead(o.chatId,o.lastReadOutboxMessageId)
  is TdApi.UpdateDeleteMessages -> if(o.chatId==activeChat) _messages.value=_messages.value.filterNot { it.id in o.messageIds.toSet() }
  is TdApi.UpdateChatReadInbox -> _chats.value=_chats.value.map { if(it.id==o.chatId) it.copy(unreadCount=o.unreadCount) else it }
  is TdApi.UpdateChatPosition -> {
   val relevant = o.position.list is TdApi.ChatListMain || o.position.list is TdApi.ChatListArchive
   if(relevant) _chats.value=_chats.value.map { if(it.id==o.chatId) it.copy(isPinned=o.position.isPinned) else it }
  }
  is TdApi.UpdateChatIsMarkedAsUnread -> _chats.value=_chats.value.map { if(it.id==o.chatId) it.copy(isMarkedAsUnread=o.isMarkedAsUnread) else it }
  is TdApi.UpdateChatNotificationSettings -> _chats.value=_chats.value.map { if(it.id==o.chatId) it.copy(isMuted=o.notificationSettings.muteFor>0) else it }
  is TdApi.UpdateFile -> { val file=o.file; if(file.local.isDownloadingCompleted) { fileDownloadQueue.markCompleted(file.id); val chatId=fileToChat[file.id]; if(chatId!=null) { _chats.value=_chats.value.map { c -> if(c.id==chatId) c.copy(avatarPath=file.local.path) else c }; if(_profile.value?.chatId==chatId) _profile.value=_profile.value?.copy(avatarPath=file.local.path) }; val messageId=videoFileToMessage[file.id]; if(messageId!=null) _messages.value=_messages.value.map { if(it.id==messageId) it.copy(videoPath=file.local.path) else it }; val mediaMessageId=mediaFileToMessage[file.id]; if(mediaMessageId!=null && activeChat!=null) { _messages.value=_messages.value.map { if(it.id==mediaMessageId) it.copy(mediaPath=file.local.path,photoPath=if(it.mediaKind=="photo") file.local.path else it.photoPath,mediaSize=file.size) else it }; mediaFileToMessage.remove(file.id) }; val storyKey=storyFileToKey[file.id]; if(storyKey!=null) _stories.value=_stories.value.map { if(it.chatId==storyKey.first && it.storyId==storyKey.second) it.copy(path=file.local.path) else it }; if(myPhotoFileId==file.id) _account.value=_account.value?.copy(avatarPath=file.local.path) } }
  is TdApi.UpdateNewMessage -> { val m=o.message; if(m.chatId==activeChat) { val readMarker=_chats.value.firstOrNull{it.id==m.chatId}?.lastReadOutboxMessageId ?: 0L; val mapped=toTgMessage(m,readMarker)
   val replyText=_messages.value.firstOrNull { it.id==mapped.replyToMessageId }?.text ?: mapped.replyToText
   _messages.value=(_messages.value+mapped.copy(replyToText=replyText)).distinctBy{it.id}.sortedBy{it.date}; if(!m.isOutgoing) send(TdApi.ViewMessages(m.chatId,longArrayOf(m.id),null,true)) } }
  is TdApi.UpdateMessageContent -> { if(o.chatId==activeChat) { val t=TdMessageTextFormatter.display(o.newContent); _messages.value=_messages.value.map { if(it.id==o.messageId) it.copy(text=t) else it } } }
  is TdApi.UpdateMessageIsPinned -> if(o.chatId==activeChat) _messages.value=_messages.value.map { if(it.id==o.messageId) it.copy(isPinned=o.isPinned) else it }
  is TdApi.UpdateMessageInteractionInfo -> { if(o.chatId==activeChat) { val reactions=toTgReactions(o.interactionInfo); _messages.value=_messages.value.map { if(it.id==o.messageId) it.copy(reactions=reactions) else it } } }
  is TdApi.UpdateMessageSendSucceeded -> { val oldId=o.oldMessageId; val newId=o.message.id; confirmedIds[oldId]=newId; pendingEdits.remove(oldId)?.let { animatedMessageController.schedule(o.message.chatId,newId,it) } }
 } }
 private fun onAuth(a:TdApi.AuthorizationState) { when(a) {
  is TdApi.AuthorizationStateWaitTdlibParameters -> send(TdAuthorizationController.createParameters(app,BuildConfig.TELEGRAM_API_ID,BuildConfig.TELEGRAM_API_HASH))
  is TdApi.AuthorizationStateWaitPhoneNumber -> _auth.value=Auth.Phone
  is TdApi.AuthorizationStateWaitCode -> _auth.value=Auth.Code
  is TdApi.AuthorizationStateWaitPassword -> _auth.value=Auth.Password
  is TdApi.AuthorizationStateReady -> { _auth.value=Auth.Ready; pushRegistrationController.onAuthorized(); loadChats(); loadContacts(); loadPrivacySettings(); send(TdApi.GetMe()) { me -> if(me is TdApi.User) updateAccount(me) } }
  is TdApi.AuthorizationStateClosed -> _auth.value=Auth.Error("TDLib connection closed")
 } }
 fun updatePushToken(token:String) { pushRegistrationController.updateToken(token, _auth.value is Auth.Ready) }
 fun processPushPayload(payload:String) { if(payload.isNotBlank()) send(TdAuthorizationController.processPushNotification(payload)) }
 fun phone(v:String)=send(TdAuthorizationController.setPhoneNumber(v))
 fun code(v:String)=send(TdAuthorizationController.checkCode(v))
 fun password(v:String)=send(TdAuthorizationController.checkPassword(v))
 fun loadChats() {
  chatListLoader.load(TdApi.ChatListMain(),false)
  chatListLoader.load(TdApi.ChatListArchive(),true)
 }
 fun archiveChat(id:Long,archived:Boolean) { chatActions.archive(id,archived) }
 fun toggleChatPinned(id:Long,isPinned:Boolean,archived:Boolean) { chatActions.togglePinned(id,isPinned,archived) }
 fun toggleChatUnread(id:Long,isUnread:Boolean) { chatActions.toggleUnread(id,isUnread) }
 fun toggleChatMuted(id:Long,isMuted:Boolean) { chatActions.toggleMuted(id,isMuted) }
 fun openProfile(chatId:Long) {
  activeProfileUserId=null
  userActions.loadProfile(chatId,object : TdUserActionsController.ProfileListener {
   override fun onChat(chat:TdApi.Chat) {
    val kind=when(chat.type) { is TdApi.ChatTypePrivate -> "Личный чат"; is TdApi.ChatTypeBasicGroup -> "Группа"; is TdApi.ChatTypeSupergroup -> if ((chat.type as TdApi.ChatTypeSupergroup).isChannel) "Канал" else "Группа"; is TdApi.ChatTypeSecret -> "Секретный чат"; else -> "Чат" }
    _profile.value=TgProfile(chatId,chat.title,kind,avatarPath=_chats.value.firstOrNull{it.id==chatId}?.avatarPath ?: "")
    if(chat.type is TdApi.ChatTypePrivate) {
     val userId=(chat.type as TdApi.ChatTypePrivate).userId
     activeProfileUserId=userId
     userToChat[userId]=chatId
    }
   }
   override fun onUser(user:TdApi.User) {
    if(activeProfileUserId!=user.id) return
    updateUserProfile(user); updateChatUser(user)
    val profilePhotoId=user.profilePhoto?.small?.id ?: 0
    if(profilePhotoId>0) { fileToChat[profilePhotoId]=chatId; fileDownloadQueue.enqueue(profilePhotoId) }
   }
   override fun onFullInfo(userId:Long,info:TdApi.UserFullInfo) {
    if(activeProfileUserId==userId) _profile.value=_profile.value?.copy(bio=info.bio?.text ?: "",commonGroups=info.groupInCommonCount)
   }
  })
 }
 private fun updateAccount(user:TdApi.User) {
  myUserId=user.id
  _account.value=TdUserMapper.toAccount(user,_account.value)
  send(TdApi.GetUserFullInfo(user.id)) { info ->
   if(info is TdApi.UserFullInfo) _account.value=_account.value?.copy(bio=info.bio?.text ?: "")
  }
  val photoId=user.profilePhoto?.small?.id ?: 0
  myPhotoFileId=photoId.takeIf { it>0 }
  if(photoId>0) fileDownloadQueue.enqueue(photoId) else _account.value=_account.value?.copy(avatarPath="")
 }
 private fun updateUserProfile(user:TdApi.User) {
  if(activeProfileUserId!=user.id) return
  _profile.value=TdUserMapper.updateProfile(user,_profile.value)
 }
 private fun refreshUserStatus(userId:Long,status:TdApi.UserStatus) {
  _contacts.value=_contacts.value.map { if(it.id==userId) it.copy(status=userStatusText(status),isOnline=status is TdApi.UserStatusOnline) else it }
  val chatId=userToChat[userId]
  if(chatId!=null) _chats.value=_chats.value.map { if(it.id==chatId) it.copy(status=userStatusText(status),isOnline=status is TdApi.UserStatusOnline) else it }
  val current=_profile.value
  if(current!=null && chatId==current.chatId) _profile.value=current.copy(status=userStatusText(status),isOnline=status is TdApi.UserStatusOnline)
 }
 private fun updateContact(user:TdApi.User) {
  val item=TdUserMapper.toContact(user)
  _contacts.value=(_contacts.value.filterNot{it.id==user.id}+item).sortedBy{it.name.lowercase(java.util.Locale.getDefault())}
 }
 fun loadContacts() { contactsController.load() }
 fun openContact(userId:Long,onReady:(TgChat)->Unit) {
  userActions.createPrivateChat(userId,object : TdUserActionsController.Callback {
   override fun onResult(result:TdApi.Object) {
    if(result is TdApi.Chat) { putChat(result); onReady(_chats.value.firstOrNull{it.id==result.id} ?: TgChat(result.id,result.title,kind="Личный чат")) }
   }
   override fun onError(error:TdApi.Error) { }
  })
 }
 private fun updateChatUser(user:TdApi.User) {
  val chatId=userToChat[user.id] ?: return
  _chats.value=_chats.value.map { if(it.id==chatId) it.copy(status=userStatusText(user.status),isOnline=user.status is TdApi.UserStatusOnline,isPremium=user.isPremium) else it }
 }
 private fun userStatusText(status:TdApi.UserStatus):String = TdUserStatusFormatter.format(status)

 fun loadPrivacySettings() { privacyController.loadAll() }
 fun setLastSeenPrivacy(value:String) { privacyController.set(TdPrivacyController.STATUS,value) }
 fun setPhonePrivacy(value:String) { privacyController.set(TdPrivacyController.PHONE,value) }
 fun setPhotoPrivacy(value:String) { privacyController.set(TdPrivacyController.PHOTO,value) }
 fun closeProfile() { _profile.value=null; activeProfileUserId=null }
 private fun putChat(c:TdApi.Chat,archived:Boolean=false) {
  val old=_chats.value.firstOrNull { it.id==c.id }
  val item=TdChatMapper.map(c,archived,old)
  _chats.value=(_chats.value.filterNot { it.id==c.id }+item).sortedByDescending { it.lastDate }
  storyQueue.enqueue(c.id)
  val userId=TdChatMapper.privateUserId(c)
  if(userId!=null) {
   userToChat[userId]=c.id
   contactsController.enqueueUser(userId)
  }
  val photoId=TdChatMapper.photoFileId(c)
  if(photoId>0 && item.avatarPath.isBlank()) {
   fileToChat[photoId]=c.id
   fileDownloadQueue.enqueue(photoId)
  }
 }
 private fun updateChatStories(stories:TdApi.ChatActiveStories) {
  val hasStory=stories.stories.isNotEmpty()
  val hasUnviewed=stories.stories.any { it.storyId>stories.maxReadStoryId }
  _chats.value=_chats.value.map { if(it.id==stories.chatId) it.copy(hasStory=hasStory,hasUnviewedStory=hasUnviewed) else it }
  if(!hasStory) _stories.value=_stories.value.filterNot { it.chatId==stories.chatId }
  else stories.stories.forEach { info -> loadStory(stories.chatId,info.storyId,info.storyId<=stories.maxReadStoryId) }
 }
 private fun loadStory(chatId:Long,storyId:Int,isViewed:Boolean) {
  storyContentQueue.enqueue(chatId,storyId,isViewed)
 }
 private fun updateStory(story:TdApi.Story,isViewed:Boolean?=null) {
  val owner=_chats.value.firstOrNull { it.id==story.posterChatId }?.title ?: "История"
  val viewed=isViewed ?: (_stories.value.firstOrNull { it.chatId==story.posterChatId && it.storyId==story.id }?.isViewed ?: false)
  val mapped=TdStoryMapper.map(story,owner,viewed)
  val item=mapped.story
  _stories.value=(_stories.value.filterNot { it.chatId==item.chatId && it.storyId==item.storyId }+item).sortedBy { it.date }
  val file=mapped.mediaFile
  if(file!=null && !file.local.isDownloadingCompleted) {
   storyFileToKey[file.id]=item.chatId to item.storyId
   fileDownloadQueue.enqueue(file.id)
  }
 }
 fun openStory(chatId:Long,storyId:Int) {
  storyActions.open(chatId,storyId)
  _stories.value=_stories.value.map { if(it.chatId==chatId && it.storyId==storyId) it.copy(isViewed=true) else it }
  val hasUnviewed=_stories.value.any { it.chatId==chatId && !it.isViewed }
  _chats.value=_chats.value.map { if(it.id==chatId) it.copy(hasUnviewedStory=hasUnviewed) else it }
 }
 fun closeStory(chatId:Long,storyId:Int) { storyActions.close(chatId,storyId) }
 fun reactToStory(chatId:Long,storyId:Int,emoji:String="❤️") { storyActions.react(chatId,storyId,emoji) }
 fun postPhotoStory(path:String,caption:String="") {
  clearOperationError()
  val userId=myUserId
  if(userId==null) { reportOperationError("Не удалось определить аккаунт Telegram"); return }
  storyActions.postPhoto(userId,path,caption)
 }
 fun postVideoStory(path:String,duration:Double) {
  clearOperationError()
  val userId=myUserId
  if(userId==null) { reportOperationError("Не удалось определить аккаунт Telegram"); return }
  storyActions.postVideo(userId,path,duration)
 }
 private fun toTgReactions(info:TdApi.MessageInteractionInfo?):List<TgReaction> =
   TdMessageMapper.mapReactions(info)

  private fun updateChatPreview(id:Long,message:TdApi.Message?) {
   _chats.value=_chats.value.map { if(it.id==id) it.copy(preview=message?.let{m->TdMessageTextFormatter.preview(m.content)} ?: it.preview,lastDate=message?.date ?: it.lastDate) else it }.sortedByDescending{it.lastDate}
  }

  private fun toTgMessage(message:TdApi.Message,readMarker:Long):TgMessage =
   TdMessageMapper.map(message, readMarker,
    { replyId -> _messages.value.firstOrNull { it.id==replyId }?.text ?: "" },
    { file, messageId, kind ->
     if(kind=="video" || kind=="videoNote") videoFileToMessage[file.id]=messageId
     mediaFileToMessage[file.id]=messageId
     fileDownloadQueue.enqueue(file.id)
    })

 fun openChat(id:Long) {
  activeChat=id
  chatSearchGeneration++
  _searchResults.value=emptyList()
  historyLoadingChats.remove(id)
  historyExhaustedChats.remove(id)
  _messages.value=emptyList()
  chatHistoryController.loadChat(id, object : TdChatHistoryController.Callback {
   override fun onResult(result:TdApi.Object) {
    if(result is TdApi.Chat) {
     _chats.value=_chats.value.map { if(it.id==id) it.copy(lastReadOutboxMessageId=result.lastReadOutboxMessageId,unreadCount=result.unreadCount) else it }
     if(activeChat==id) _messages.value=_messages.value.map { m -> if(m.outgoing) m.copy(isRead=m.id<=result.lastReadOutboxMessageId) else m }
    }
   }
   override fun onError(error:TdApi.Error) { }
  })
  chatHistoryController.loadInitial(id, object : TdChatHistoryController.Callback {
   override fun onResult(r:TdApi.Object) {
   if(r is TdApi.Messages && activeChat==id) {
    val loaded=r.messages
    if(loaded.size<50) historyExhaustedChats.add(id)
    val readMarker=_chats.value.firstOrNull{it.id==id}?.lastReadOutboxMessageId ?: 0L
    val mapped=loaded.map{toTgMessage(it,readMarker)}
    val byId=mapped.associateBy{it.id}
    _messages.value=mapped.map { item -> if(item.replyToMessageId!=0L) item.copy(replyToText=byId[item.replyToMessageId]?.text ?: item.replyToText) else item }.sortedBy{it.date}
    val incomingIds=loaded.filter{!it.isOutgoing}.map{it.id}.toLongArray()
    if(incomingIds.isNotEmpty()) send(TdApi.ViewMessages(id,incomingIds,null,true))
   }
   }
   override fun onError(error:TdApi.Error) { }
  })
 }
 fun searchChatMessages(chatId:Long,query:String) {
  val value=query.trim()
  val generation=++chatSearchGeneration
  if(value.isBlank()) {
   _searchResults.value=emptyList()
   return
  }
  if(activeChat!=chatId) return
  _searchResults.value=emptyList()
  chatHistoryController.search(chatId,value,object : TdChatHistoryController.Callback {
   override fun onResult(result:TdApi.Object) {
   if(generation!=chatSearchGeneration || activeChat!=chatId) return
   if(result is TdApi.FoundChatMessages) {
    val readMarker=_chats.value.firstOrNull { it.id==chatId }?.lastReadOutboxMessageId ?: 0L
    _searchResults.value=result.messages.map { toTgMessage(it,readMarker) }.sortedBy { it.date }
   }
   }
   override fun onError(error:TdApi.Error) { }
  })
 }
 fun loadOlderMessages(chatId:Long) {
  if(activeChat!=chatId || chatId in historyLoadingChats || chatId in historyExhaustedChats) return
  val oldest=_messages.value.minByOrNull { it.id } ?: return
  historyLoadingChats.add(chatId)
  chatHistoryController.loadOlder(chatId,oldest.id,object : TdChatHistoryController.Callback {
   override fun onResult(result:TdApi.Object) {
   historyLoadingChats.remove(chatId)
   if(activeChat!=chatId || result !is TdApi.Messages) return
   val loaded=result.messages
   if(loaded.isEmpty()) {
    historyExhaustedChats.add(chatId)
    return
   }
   val previous=_messages.value
   val readMarker=_chats.value.firstOrNull{it.id==chatId}?.lastReadOutboxMessageId ?: 0L
   val older=loaded.map { toTgMessage(it,readMarker) }
   val combined=(older+previous).distinctBy { it.id }.sortedBy { it.date }
   val byId=combined.associateBy { it.id }
   _messages.value=combined.map { item ->
    if(item.replyToMessageId!=0L && item.replyToText.isBlank()) item.copy(replyToText=byId[item.replyToMessageId]?.text ?: item.replyToText) else item
   }
   if(loaded.size<50 || _messages.value.size==previous.size) historyExhaustedChats.add(chatId)
   }
   override fun onError(error:TdApi.Error) { historyLoadingChats.remove(chatId) }
  })
 }
 private fun updateOutboxRead(chatId:Long,lastReadId:Long) {
  _chats.value=_chats.value.map { if(it.id==chatId) it.copy(lastReadOutboxMessageId=lastReadId) else it }
  if(activeChat==chatId) _messages.value=_messages.value.map { m -> if(m.outgoing) m.copy(isRead=m.id<=lastReadId) else m }
 }
 fun clearOperationError() { _operationError.value=null }
  fun updateMyProfile(firstName:String,lastName:String,bio:String) {
  accountActions.updateProfile(firstName,lastName,bio,myUserId ?: 0L)
 }
 fun setMyProfilePhoto(path:String) {
  accountActions.setProfilePhoto(path,myUserId ?: 0L)
 }
fun reportOperationError(text:String) {
  FirebaseCrashlytics.getInstance().apply {
   setCustomKey("operation_error_source", "local_ui_or_media")
   recordException(IllegalStateException("Local media or UI operation failed"))
  }
  _operationError.value=text
 }
 fun sendPhoto(chatId:Long,path:String,caption:String="") { clearOperationError(); messageActions.sendPhoto(chatId,path,caption) }
  fun sendDocument(chatId:Long,path:String,caption:String="") { clearOperationError(); messageActions.sendDocument(chatId,path,caption) }
  fun sendVideo(chatId:Long,path:String,shape:String) { clearOperationError(); messageActions.sendVideo(chatId,path,shape) }
 private fun encodeVideoShape(shape:String):String = TdMessageActionsController.encodeVideoShape(shape)
 private fun decodeVideoShape(caption:String):String = TdMessageActionsController.decodeVideoShape(caption)
 fun reactToMessage(chatId:Long,messageId:Long,emoji:String) {
  clearOperationError()
  val chosen=_messages.value.firstOrNull { it.id==messageId }?.reactions?.any { it.emoji==emoji && it.chosen } == true
  messageActions.react(chatId,messageId,emoji,chosen)
 }
 fun forwardMessage(fromChatId:Long,toChatId:Long,messageId:Long) { clearOperationError(); messageActions.forward(fromChatId,toChatId,messageId) }
 fun pinMessage(chatId:Long,messageId:Long,pin:Boolean) { clearOperationError(); messageActions.pin(chatId,messageId,pin) }
 fun deleteMessage(chatId:Long,messageId:Long,revoke:Boolean) { clearOperationError(); messageActions.delete(chatId,messageId,revoke) }
 fun clearHistory(chatId:Long,revoke:Boolean) { clearOperationError(); messageActions.clearHistory(chatId,revoke) }
 fun editMessage(chatId:Long,messageId:Long,text:String) { clearOperationError(); messageActions.editText(chatId,messageId,text) }
 fun sendPlain(id:Long,text:String,replyToMessageId:Long?=null) { clearOperationError(); messageActions.sendText(id,text,replyToMessageId) }
 fun replyToStory(chatId:Long,storyId:Int,text:String) { clearOperationError(); messageActions.replyToStory(chatId,storyId,text) }
 fun sendAnimated(id:Long,text:String,replyToMessageId:Long?=null) {
  clearOperationError()
  animatedMessageController.sendAnimated(id,text,replyToMessageId,
   TdAnimatedMessageController.SendListener { message, characters ->
    if(characters.size>1) {
     val serverId=confirmedIds[message.id]
     if(serverId!=null) animatedMessageController.schedule(id,serverId,characters)
     else pendingEdits[message.id]=characters
    }
   })
 }
 private fun send(f:TdApi.Function<*>,handler:((TdApi.Object)->Unit)?=null,onError:((TdApi.Error)->Unit)?=null,reportErrorToUi:Boolean=true) {
  client.send(f,Client.ResultHandler { r ->
   if(r is TdApi.Error) {
    DiagnosticsJournal.record(app, f.javaClass.simpleName, r.code, r.message)
    FirebaseCrashlytics.getInstance().apply {
     log("TDLib request failed: ${f.javaClass.simpleName}, code=${r.code}")
     setCustomKey("tdlib_function", f.javaClass.simpleName)
     setCustomKey("tdlib_error_code", r.code)
     recordException(IllegalStateException("TDLib request failed: ${f.javaClass.simpleName}, code=${r.code}"))
    }
    onError?.invoke(r)
    if(reportErrorToUi) _operationError.value=r.message
   } else handler?.invoke(r)
  })
 }
 fun close(){animatedMessageController.close();pushRegistrationController.close();fileDownloadQueue.close();contactsController.close();chatListLoader.close();storyContentQueue.close();storyQueue.close();client.send(TdApi.Close(),null)}
 
}