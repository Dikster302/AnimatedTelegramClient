package com.dikster.specialgram
import android.content.Context
import android.os.Build
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private fun TgProfile.copy(
 bio:String=this.bio,avatarPath:String=this.avatarPath,status:String=this.status,
 isOnline:Boolean=this.isOnline,commonGroups:Int=this.commonGroups
):TgProfile = TgProfile(chatId,name,kind,phone,bio,avatarPath,username,status,isOnline,isPremium,isVerified,commonGroups)
private fun TgAccount.copy(bio:String=this.bio,avatarPath:String=this.avatarPath):TgAccount =
 TgAccount(name,username,phone,isPremium,isVerified,bio,avatarPath,firstName,lastName)
private fun TgChat.copy(
 title:String=this.title,preview:String=this.preview,lastDate:Int=this.lastDate,unreadCount:Int=this.unreadCount,
 avatarPath:String=this.avatarPath,kind:String=this.kind,archived:Boolean=this.archived,status:String=this.status,
 isOnline:Boolean=this.isOnline,isPremium:Boolean=this.isPremium,
 lastReadOutboxMessageId:Long=this.lastReadOutboxMessageId,hasStory:Boolean=this.hasStory,
 hasUnviewedStory:Boolean=this.hasUnviewedStory,isPinned:Boolean=this.isPinned,
 isMarkedAsUnread:Boolean=this.isMarkedAsUnread,isMuted:Boolean=this.isMuted
):TgChat = TgChat(id,title,preview,lastDate,unreadCount,avatarPath,kind,archived,status,isOnline,isPremium,
 lastReadOutboxMessageId,hasStory,hasUnviewedStory,isPinned,isMarkedAsUnread,isMuted)
private fun TgMessage.copy(
 text:String=this.text,outgoing:Boolean=this.outgoing,date:Int=this.date,isRead:Boolean=this.isRead,
 videoShape:String=this.videoShape,videoPath:String=this.videoPath,replyToMessageId:Long=this.replyToMessageId,
 replyToText:String=this.replyToText,reactions:List<TgReaction> = this.reactions,photoPath:String=this.photoPath,
 mediaKind:String=this.mediaKind,mediaPath:String=this.mediaPath,mediaName:String=this.mediaName,
 mediaMimeType:String=this.mediaMimeType,mediaDuration:Int=this.mediaDuration,mediaSize:Long=this.mediaSize,
 isPinned:Boolean=this.isPinned
):TgMessage = TgMessage(id,text,outgoing,date,isRead,videoShape,videoPath,replyToMessageId,replyToText,
 reactions,photoPath,mediaKind,mediaPath,mediaName,mediaMimeType,mediaDuration,mediaSize,isPinned)

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
 private val chatSession = TdChatSessionController()
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
   if(result is TdApi.ChatActiveStories) storyStateController.updateChatStories(result)
  }
 )
 private var myUserId:Long?=null
 private var myPhotoFileId:Int?=null
 private val profileSession = TdProfileSessionController()
 private val pendingEdits=java.util.concurrent.ConcurrentHashMap<Long,List<String>>()
 private val fileToChat=java.util.concurrent.ConcurrentHashMap<Int,Long>()
 private val videoFileToMessage=java.util.concurrent.ConcurrentHashMap<Int,Pair<Long,Long>>()
  private val mediaFileToMessage=java.util.concurrent.ConcurrentHashMap<Int,Pair<Long,Long>>()
 private val storyFileToKey=java.util.concurrent.ConcurrentHashMap<Int,Pair<Long,Int>>()
 private val userToChat=java.util.concurrent.ConcurrentHashMap<Long,Long>()
 private val confirmedIds=java.util.concurrent.ConcurrentHashMap<Long,Long>()
 private val _operationError=MutableStateFlow<String?>(null); val operationError=_operationError.asStateFlow()
 private val userUpdateRouter = TdUserUpdateRouter(object : TdUserUpdateRouter.Listener {
  override fun onUserStatus(userId:Long, status:TdApi.UserStatus) {
   val state=TdUserStateController.refreshStatus(_chats.value,_contacts.value,_profile.value,userToChat,userId,status)
   _chats.value=state.chats
   _contacts.value=state.contacts
   _profile.value=state.profile
  }
  override fun onPrivacyRules(setting:TdApi.UserPrivacySetting, rules:TdApi.UserPrivacySettingRules) {
   when(setting) {
    is TdApi.UserPrivacySettingShowStatus -> _privacyLastSeen.value=TdPrivacyController.label(rules)
    is TdApi.UserPrivacySettingShowPhoneNumber -> _privacyPhone.value=TdPrivacyController.label(rules)
    is TdApi.UserPrivacySettingShowProfilePhoto -> _privacyPhoto.value=TdPrivacyController.label(rules)
   }
  }
  override fun onUser(user:TdApi.User) {
   profileController.updateUser(user)
   _chats.value=TdUserStateController.updateChatUser(_chats.value,userToChat,user)
   _contacts.value=TdUserStateController.updateContact(_contacts.value,user)
   if(myUserId==user.id) accountStateController.updateAccount(user)
  }
 })
 private val operationErrorController = TdOperationErrorController(object : TdOperationErrorController.Listener {
  override fun onOperationError(message:String) { _operationError.value=message }
  override fun onClearOperationError() { _operationError.value=null }
 })
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
 private val userFullInfoQueue = TdUserFullInfoQueue(
  TdUserFullInfoQueue.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) },
    reportErrorToUi=false)
  },
  TdUserFullInfoQueue.Listener { userId, info ->
   if(myUserId==userId) _account.value=_account.value?.copy(bio=info.bio?.text ?: "")
  }
 )
 private val accountStateController = TdAccountStateController(object : TdAccountStateController.Listener {
  override fun account():TgAccount? = _account.value
  override fun setAccount(account:TgAccount?) { _account.value=account }
  override fun setMyUserId(userId:Long) { myUserId=userId }
  override fun setMyPhotoFileId(fileId:Int?) { myPhotoFileId=fileId }
  override fun enqueueUserFullInfo(userId:Long) { userFullInfoQueue.enqueue(userId) }
  override fun enqueueFileDownload(fileId:Int) { fileDownloadQueue.enqueue(fileId) }
 })
 private val messageViewQueue = TdMessageViewQueue(
  TdMessageViewQueue.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) },
    reportErrorToUi=false)
  }
 )
 private val userActions = TdUserActionsController(
  TdUserActionsController.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) })
  }
 )
 private val profileController = TdProfileController(userActions,profileSession,
  object : TdProfileController.Listener {
   override fun findChat(chatId:Long):TgChat? = _chats.value.firstOrNull { it.id==chatId }
   override fun profile():TgProfile? = _profile.value
   override fun setProfile(profile:TgProfile?) { _profile.value=profile }
   override fun onPrivateChatUser(userId:Long,chatId:Long) { userToChat[userId]=chatId }
   override fun onProfileUser(user:TdApi.User,chatId:Long) {
    _chats.value=TdUserStateController.updateChatUser(_chats.value,userToChat,user)
    val photoId=user.profilePhoto?.small?.id ?: 0
    if(photoId>0) { fileToChat[photoId]=chatId; fileDownloadQueue.enqueue(photoId) }
   }
  })
 private val accountActions = TdAccountActionsController(
  TdAccountActionsController.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) })
  },
  object : TdAccountActionsController.Listener {
   override fun onAccountUser(user:TdApi.User) { accountStateController.updateAccount(user) }
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
 private val chatConversationController = TdChatConversationController(chatSession,chatHistoryController,
  object : TdChatConversationController.Listener {
   override fun chats():List<TgChat> = _chats.value
   override fun setChats(chats:List<TgChat>) { _chats.value=chats }
   override fun messages():List<TgMessage> = _messages.value
   override fun setMessages(messages:List<TgMessage>) { _messages.value=messages }
   override fun setSearchResults(messages:List<TgMessage>) { _searchResults.value=messages }
   override fun mapMessage(message:TdApi.Message,readMarker:Long):TgMessage = toTgMessage(message,readMarker)
   override fun enqueueViewedMessages(chatId:Long,messageIds:LongArray) {
    messageViewQueue.enqueue(chatId,messageIds)
   }
  })
 private val storyContentQueue = TdStoryContentQueue(
  TdStoryContentQueue.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) },
    reportErrorToUi=false)
  },
  TdStoryContentQueue.Listener { story, viewed -> storyStateController.updateStory(story,viewed) }
 )
 private val storyStateController = TdStoryStateController(object : TdStoryStateController.Listener {
  override fun chats():List<TgChat> = _chats.value
  override fun stories():List<TgStory> = _stories.value
  override fun setChats(chats:List<TgChat>) { _chats.value=chats }
  override fun setStories(stories:List<TgStory>) { _stories.value=stories }
  override fun enqueueStoryContent(chatId:Long,storyId:Int,viewed:Boolean) {
   storyContentQueue.enqueue(chatId,storyId,viewed)
  }
  override fun onStoryMediaFile(file:TdApi.File,story:TgStory) {
   storyFileToKey[file.id]=story.chatId to story.storyId
   fileDownloadQueue.enqueue(file.id)
  }
 })
 private val storyActions = TdStoryActionsController(
  TdStoryActionsController.Requester { function, callback ->
   send(function,
    handler={ callback?.onResult(it) },
    onError={ callback?.onError(it) },
    reportErrorToUi=false)
  },
  object : TdStoryActionsController.Listener {
   override fun onStoryPosted(story:TdApi.Story) {
    storyStateController.updateStory(story,false)
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
    if(chatSession.activeChat==chatId) _messages.value=emptyList()
    _chats.value=_chats.value.map { if(it.id==chatId) it.copy(preview="",lastDate=0,unreadCount=0) else it }
   }
  }
 )
 private val messageStateController = TdMessageStateController(object : TdMessageStateController.Listener {
  override fun activeChatId():Long? = chatSession.activeChat
  override fun chats():List<TgChat> = _chats.value
  override fun messages():List<TgMessage> = _messages.value
  override fun setMessages(messages:List<TgMessage>) { _messages.value=messages }
  override fun mapMessage(message:TdApi.Message,readMarker:Long):TgMessage = toTgMessage(message,readMarker)
  override fun enqueueViewedMessages(chatId:Long,messageIds:LongArray) {
   messageViewQueue.enqueue(chatId,messageIds)
  }
 },animatedMessageController,confirmedIds,pendingEdits)
 private val fileStateController = TdFileStateController(object : TdFileStateController.Listener {
  override fun activeChatId():Long? = chatSession.activeChat
  override fun chats():List<TgChat> = _chats.value
  override fun setChats(chats:List<TgChat>) { _chats.value=chats }
  override fun profile():TgProfile? = _profile.value
  override fun setProfile(profile:TgProfile?) { _profile.value=profile }
  override fun messages():List<TgMessage> = _messages.value
  override fun setMessages(messages:List<TgMessage>) { _messages.value=messages }
  override fun stories():List<TgStory> = _stories.value
  override fun setStories(stories:List<TgStory>) { _stories.value=stories }
  override fun account():TgAccount? = _account.value
  override fun setAccount(account:TgAccount?) { _account.value=account }
  override fun markDownloadCompleted(fileId:Int) { fileDownloadQueue.markCompleted(fileId) }
  override fun isMyPhotoFile(fileId:Int):Boolean = myPhotoFileId==fileId
 },fileToChat,videoFileToMessage,mediaFileToMessage,storyFileToKey)
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
   if(user is TdApi.User) { _contacts.value=TdUserStateController.updateContact(_contacts.value,user); _chats.value=TdUserStateController.updateChatUser(_chats.value,userToChat,user) }
  }
 )
 private val chatRegistrationController = TdChatRegistrationController(object : TdChatRegistrationController.Listener {
  override fun chats():List<TgChat> = _chats.value
  override fun setChats(chats:List<TgChat>) { _chats.value=chats }
  override fun enqueueChatStories(chatId:Long) { storyQueue.enqueue(chatId) }
  override fun onPrivateChatUser(userId:Long,chatId:Long) {
   userToChat[userId]=chatId
   contactsController.enqueueUser(userId)
  }
  override fun onChatPhoto(fileId:Int,chatId:Long) {
   fileToChat[fileId]=chatId
   fileDownloadQueue.enqueue(fileId)
  }
 })
 private val chatListLoader = TdChatListLoader(
  TdChatListLoader.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) },
    reportErrorToUi=false)
  },
  TdChatListLoader.ChatListener { chat, archived -> chatRegistrationController.putChat(chat,archived) }
 )

 init { if(BuildConfig.TELEGRAM_API_ID.isBlank() || BuildConfig.TELEGRAM_API_HASH.isBlank()) _auth.value=Auth.Error("Set TELEGRAM_API_ID and TELEGRAM_API_HASH in GitHub Actions secrets.") }
 private fun onUpdate(o:TdApi.Object) { if(messageStateController.handle(o) || fileStateController.handle(o)) return; when(o) {
  is TdApi.UpdateAuthorizationState -> authorizationStateRouter.onState(o.authorizationState)
  is TdApi.UpdateNewChat -> chatRegistrationController.putChat(o.chat,false)
  is TdApi.UpdateChatTitle -> _chats.value=TdChatStateController.updateTitle(_chats.value,o.chatId,o.title)
  is TdApi.UpdateChatActiveStories -> storyStateController.updateChatStories(o.activeStories)
  is TdApi.UpdateStory -> storyStateController.updateStory(o.story,null)
  is TdApi.UpdateStoryDeleted -> storyStateController.deleteStory(o.storyPosterChatId,o.storyId)
  is TdApi.UpdateChatLastMessage -> _chats.value=TdChatStateController.updatePreview(_chats.value,o.chatId,o.lastMessage)
  is TdApi.UpdateChatReadOutbox -> { val state=TdChatStateController.updateOutboxRead(_chats.value,_messages.value,chatSession.activeChat,o.chatId,o.lastReadOutboxMessageId); _chats.value=state.chats; _messages.value=state.messages }
  is TdApi.UpdateChatReadInbox -> _chats.value=TdChatStateController.updateUnreadCount(_chats.value,o.chatId,o.unreadCount)
  is TdApi.UpdateChatPosition -> _chats.value=TdChatStateController.updatePinned(_chats.value,o.chatId,o.position)
  is TdApi.UpdateChatIsMarkedAsUnread -> _chats.value=TdChatStateController.updateMarkedUnread(_chats.value,o.chatId,o.isMarkedAsUnread)
  is TdApi.UpdateChatNotificationSettings -> _chats.value=TdChatStateController.updateMuted(_chats.value,o.chatId,o.notificationSettings)
  else -> userUpdateRouter.route(o)
 } }
 private val authorizationStateRouter = TdAuthorizationStateRouter(
  app, BuildConfig.TELEGRAM_API_ID, BuildConfig.TELEGRAM_API_HASH,
  object : TdAuthorizationStateRouter.Listener {
   override fun onAuthChanged(state:Auth) { _auth.value=state }
   override fun onSend(function:TdApi.Function<*>) { send(function) }
   override fun onAuthorized() {
    pushRegistrationController.onAuthorized()
    loadChats()
    loadContacts()
    loadPrivacySettings()
    send(TdApi.GetMe()) { me -> if(me is TdApi.User) accountStateController.updateAccount(me) }
   }
  }
 )
 fun updatePushToken(token:String) { pushRegistrationController.updateToken(token, _auth.value === Auth.Ready) }
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
 fun openProfile(chatId:Long) { profileController.openProfile(chatId) }
 fun loadContacts() { contactsController.load() }
 fun openContact(userId:Long,onReady:(TgChat)->Unit) {
  userActions.createPrivateChat(userId,object : TdUserActionsController.Callback {
   override fun onResult(result:TdApi.Object) {
    if(result is TdApi.Chat) { chatRegistrationController.putChat(result,false); onReady(_chats.value.firstOrNull{it.id==result.id} ?: TgChat(result.id,result.title,"",0,0,"","Личный чат",false,"",false,false,0L,false,false,false,false,false)) }
   }
   override fun onError(error:TdApi.Error) { }
  })
 }

 fun loadPrivacySettings() { privacyController.loadAll() }
 fun setLastSeenPrivacy(value:String) { privacyController.set(TdPrivacyController.STATUS,value) }
 fun setPhonePrivacy(value:String) { privacyController.set(TdPrivacyController.PHONE,value) }
 fun setPhotoPrivacy(value:String) { privacyController.set(TdPrivacyController.PHOTO,value) }
 fun closeProfile() { profileController.closeProfile() }
 fun openStory(chatId:Long,storyId:Int) {
  storyActions.open(chatId,storyId)
  storyStateController.markViewed(chatId,storyId)
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
  private fun toTgMessage(message:TdApi.Message,readMarker:Long):TgMessage =
   TdMessageMapper.map(message, readMarker,
    { replyId -> _messages.value.firstOrNull { it.id==replyId }?.text ?: "" },
    { file, messageId, kind ->
     val messageKey=message.chatId to messageId
     if(kind=="video" || kind=="videoNote") videoFileToMessage[file.id]=messageKey
     mediaFileToMessage[file.id]=messageKey
     fileDownloadQueue.enqueue(file.id)
    })

 fun openChat(id:Long) { chatConversationController.openChat(id) }
 fun closeChat() { chatConversationController.closeChat() }
 fun searchChatMessages(chatId:Long,query:String) { chatConversationController.searchChatMessages(chatId,query) }
 fun loadOlderMessages(chatId:Long) { chatConversationController.loadOlderMessages(chatId) }
 fun clearOperationError() { operationErrorController.clear() }
  fun updateMyProfile(firstName:String,lastName:String,bio:String) {
  accountActions.updateProfile(firstName,lastName,bio,myUserId ?: 0L)
 }
 fun setMyProfilePhoto(path:String) {
  accountActions.setProfilePhoto(path,myUserId ?: 0L)
 }
fun reportOperationError(text:String) { operationErrorController.report(text) }
 fun sendPhoto(chatId:Long,path:String,caption:String="") { clearOperationError(); messageActions.sendPhoto(chatId,path,caption) }
  fun sendDocument(chatId:Long,path:String,caption:String="") { clearOperationError(); messageActions.sendDocument(chatId,path,caption) }
  fun sendVideo(chatId:Long,path:String,shape:String) { clearOperationError(); messageActions.sendVideo(chatId,path,shape) }
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
 private val requestDispatcher = TdRequestDispatcher(app, client)
 private fun send(f:TdApi.Function<*>,handler:((TdApi.Object)->Unit)?=null,onError:((TdApi.Error)->Unit)?=null,reportErrorToUi:Boolean=true) {
  requestDispatcher.send(f, object : TdRequestDispatcher.Listener {
   override fun onResult(result:TdApi.Object) { handler?.invoke(result) }
   override fun onError(error:TdApi.Error) {
    onError?.invoke(error)
    if(reportErrorToUi) _operationError.value=error.message
   }
  })
 }
 fun close(){animatedMessageController.close();messageViewQueue.close();userFullInfoQueue.close();pushRegistrationController.close();fileDownloadQueue.close();contactsController.close();chatListLoader.close();storyContentQueue.close();storyQueue.close();client.send(TdApi.Close(),null)}
 
}