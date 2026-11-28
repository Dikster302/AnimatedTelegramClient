package com.dikster.specialgram
import android.content.Context
import org.drinkless.tdlib.TdApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
 private val updateLock=Any()
 private val pendingUpdates=java.util.ArrayDeque<TdApi.Object>()
 private var updateStateReady=false
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
 private val messageMappingController = TdMessageMappingController(object : TdMessageMappingController.Listener {
  override fun replyText(messageId:Long):String = _messages.value.firstOrNull { it.id==messageId }?.text ?: ""
  override fun enqueueMediaFile(file:TdApi.File,chatId:Long,messageId:Long,kind:String) {
   fileDownloadQueue.enqueue(file.id)
  }
 },videoFileToMessage,mediaFileToMessage)
 private val userToChat=java.util.concurrent.ConcurrentHashMap<Long,Long>()
 private val confirmedIds=java.util.concurrent.ConcurrentHashMap<Long,Long>()
 private val _operationError=MutableStateFlow<String?>(null); val operationError=_operationError.asStateFlow()
 private val operationErrorController = TdOperationErrorController(object : TdOperationErrorController.Listener {
  override fun onOperationError(message:String) { _operationError.value=message }
  override fun onClearOperationError() { _operationError.value=null }
 })
 private val animatedMessageSendController = TdAnimatedMessageSendController(
  animatedMessageController,confirmedIds,pendingEdits,Runnable { clearOperationError() })

 private val client=TdClientFactory.create(object : TdClientFactory.Listener {
  override fun onUpdate(update:TdApi.Object) { onUpdate(update) }
  override fun onUpdateError(error:Throwable) { _auth.value=Auth.Error(error.message ?: "TDLib error") }
  override fun onFatalError(error:Throwable) { _auth.value=Auth.Error(error.message ?: "TDLib error") }
 })
 private val userFullInfoQueue = TdUserFullInfoQueue(
  TdUserFullInfoQueue.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) },
    reportErrorToUi=false)
  },
  TdUserFullInfoQueue.Listener { userId, info ->
   if(myUserId==userId) _account.value=TgModelStateController.withBio(_account.value,info.bio?.text ?: "")
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
   override fun profile():TgProfile? = _profile.value
   override fun setProfile(profile:TgProfile?) { _profile.value=profile }
   override fun onPrivateChatUser(userId:Long,chatId:Long) { userToChat[userId]=chatId }
   override fun onProfileUser(user:TdApi.User,chatId:Long) {
    _chats.value=TdUserStateController.updateChatUser(_chats.value,userToChat,user)
    val photoId=user.profilePhoto?.small?.id ?: 0
    if(photoId>0) { fileToChat[photoId]=chatId; fileDownloadQueue.enqueue(photoId) }
   }
  })
 private val userUpdateStateController = TdUserUpdateStateController(object : TdUserUpdateStateController.State {
  override fun chats():List<TgChat> = _chats.value
  override fun setChats(chats:List<TgChat>) { _chats.value=chats }
  override fun contacts():List<TgContact> = _contacts.value
  override fun setContacts(contacts:List<TgContact>) { _contacts.value=contacts }
  override fun profile():TgProfile? = _profile.value
  override fun setProfile(profile:TgProfile?) { _profile.value=profile }
  override fun myUserId():Long? = myUserId
  override fun setLastSeenPrivacy(value:String) { _privacyLastSeen.value=value }
  override fun setPhonePrivacy(value:String) { _privacyPhone.value=value }
  override fun setPhotoPrivacy(value:String) { _privacyPhoto.value=value }
 },userToChat,profileController,accountStateController)
 private val userUpdateRouter = TdUserUpdateRouter(userUpdateStateController)
 private val accountActions = TdAccountActionsController(
  TdAccountActionsController.Requester { function, callback ->
   send(function,
    handler={ callback.onResult(it) },
    onError={ callback.onError(it) })
  },
  object : TdAccountActionsController.Listener {
   override fun onAccountUser(user:TdApi.User) { accountStateController.updateAccount(user) }
   override fun onBioUpdated(bio:String) { _account.value=TgModelStateController.withBio(_account.value,bio) }
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
   override fun mapMessage(message:TdApi.Message,readMarker:Long):TgMessage = messageMappingController.map(message,readMarker)
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
    if(userId!=null) _chats.value=_chats.value.map { if(it.id==userId) TgModelStateController.withStoryFlags(it,true,true) else it }
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
    _chats.value=_chats.value.map { if(it.id==chatId) TgModelStateController.clearHistoryPreview(it) else it }
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
 private val updateStateController = TdUpdateStateController(object : TdUpdateStateController.State {
  override fun chats():List<TgChat> = _chats.value
  override fun setChats(chats:List<TgChat>) { _chats.value=chats }
  override fun messages():List<TgMessage> = _messages.value
  override fun setMessages(messages:List<TgMessage>) { _messages.value=messages }
 },authorizationStateRouter,chatRegistrationController,storyStateController,chatSession,userUpdateRouter)
 private fun onUpdate(o:TdApi.Object) {
  synchronized(updateLock) {
   if(!updateStateReady) {
    pendingUpdates.addLast(o)
    return
   }
   dispatchUpdate(o)
  }
 }
 private fun dispatchUpdate(o:TdApi.Object) {
  if(messageStateController.handle(o) || fileStateController.handle(o)) return
  updateStateController.route(o)
 }
 fun updatePushToken(token:String) { operationsController.updatePushToken(token) }
 fun processPushPayload(payload:String) { operationsController.processPushPayload(payload) }
 fun phone(v:String) { operationsController.phone(v) }
 fun code(v:String) { operationsController.code(v) }
 fun password(v:String) { operationsController.password(v) }
 fun loadChats() { operationsController.loadChats() }
 fun archiveChat(id:Long,archived:Boolean) { operationsController.archiveChat(id,archived) }
 fun toggleChatPinned(id:Long,isPinned:Boolean,archived:Boolean) { operationsController.toggleChatPinned(id,isPinned,archived) }
 fun toggleChatUnread(id:Long,isUnread:Boolean) { operationsController.toggleChatUnread(id,isUnread) }
 fun toggleChatMuted(id:Long,isMuted:Boolean) { operationsController.toggleChatMuted(id,isMuted) }
 fun openProfile(chatId:Long) { operationsController.openProfile(chatId) }
 fun loadContacts() { operationsController.loadContacts() }
 fun openContact(userId:Long,onReady:(TgChat)->Unit) {
  operationsController.openContact(userId) { chat -> onReady(chat) }
 }
 fun loadPrivacySettings() { operationsController.loadPrivacySettings() }
 fun setLastSeenPrivacy(value:String) { operationsController.setLastSeenPrivacy(value) }
 fun setPhonePrivacy(value:String) { operationsController.setPhonePrivacy(value) }
 fun setPhotoPrivacy(value:String) { operationsController.setPhotoPrivacy(value) }
 fun closeProfile() { operationsController.closeProfile() }
 fun openStory(chatId:Long,storyId:Int) { operationsController.openStory(chatId,storyId) }
 fun closeStory(chatId:Long,storyId:Int) { operationsController.closeStory(chatId,storyId) }
 fun reactToStory(chatId:Long,storyId:Int,emoji:String="❤️") { operationsController.reactToStory(chatId,storyId,emoji) }
 fun postPhotoStory(path:String,caption:String="") { operationsController.postPhotoStory(path,caption) }
 fun postVideoStory(path:String,duration:Double) { operationsController.postVideoStory(path,duration) }
 fun openChat(id:Long) { operationsController.openChat(id) }
 fun closeChat() { operationsController.closeChat() }
 fun searchChatMessages(chatId:Long,query:String) { operationsController.searchChatMessages(chatId,query) }
 fun loadOlderMessages(chatId:Long) { operationsController.loadOlderMessages(chatId) }
 fun clearOperationError() { operationsController.clearOperationError() }
 fun updateMyProfile(firstName:String,lastName:String,bio:String) { operationsController.updateMyProfile(firstName,lastName,bio) }
 fun setMyProfilePhoto(path:String) { operationsController.setMyProfilePhoto(path) }
 fun reportOperationError(text:String) { operationsController.reportOperationError(text) }
 fun sendPhoto(chatId:Long,path:String,caption:String="") { operationsController.sendPhoto(chatId,path,caption) }
 fun sendDocument(chatId:Long,path:String,caption:String="") { operationsController.sendDocument(chatId,path,caption) }
 fun sendVideo(chatId:Long,path:String,shape:String) { operationsController.sendVideo(chatId,path,shape) }
 fun reactToMessage(chatId:Long,messageId:Long,emoji:String) { operationsController.reactToMessage(chatId,messageId,emoji) }
 fun forwardMessage(fromChatId:Long,toChatId:Long,messageId:Long) { operationsController.forwardMessage(fromChatId,toChatId,messageId) }
 fun pinMessage(chatId:Long,messageId:Long,pin:Boolean) { operationsController.pinMessage(chatId,messageId,pin) }
 fun deleteMessage(chatId:Long,messageId:Long,revoke:Boolean) { operationsController.deleteMessage(chatId,messageId,revoke) }
 fun clearHistory(chatId:Long,revoke:Boolean) { operationsController.clearHistory(chatId,revoke) }
 fun editMessage(chatId:Long,messageId:Long,text:String) { operationsController.editMessage(chatId,messageId,text) }
 fun sendPlain(id:Long,text:String,replyToMessageId:Long?=null) { operationsController.sendPlain(id,text,replyToMessageId) }
 fun replyToStory(chatId:Long,storyId:Int,text:String) { operationsController.replyToStory(chatId,storyId,text) }
 fun sendAnimated(id:Long,text:String,replyToMessageId:Long?=null) { operationsController.sendAnimated(id,text,replyToMessageId) }

 private val requestDispatcher = TdRequestDispatcher(app, client)
 private val requestGateway = TdRequestGateway(requestDispatcher,
  TdRequestGateway.Listener { message -> _operationError.value=message })
 private fun send(f:TdApi.Function<*>,handler:((TdApi.Object)->Unit)?=null,onError:((TdApi.Error)->Unit)?=null,reportErrorToUi:Boolean=true) {
  val resultHandler=handler?.let { callback -> TdRequestGateway.ResultHandler { result -> callback(result) } }
  val errorHandler=onError?.let { callback -> TdRequestGateway.ErrorHandler { error -> callback(error) } }
  requestGateway.send(f,resultHandler,errorHandler,reportErrorToUi)
 }

 private val lifecycleController = TdClientLifecycleController(client,
  Runnable { animatedMessageController.close() },
  Runnable { messageViewQueue.close() },
  Runnable { userFullInfoQueue.close() },
  Runnable { pushRegistrationController.close() },
  Runnable { fileDownloadQueue.close() },
  Runnable { contactsController.close() },
  Runnable { chatListLoader.close() },
  Runnable { storyContentQueue.close() },
  Runnable { storyQueue.close() })
 fun close() { lifecycleController.close() }
 private val operationsController = TdClientOperationsController(
  object : TdClientOperationsController.Listener {
   override fun myUserId():Long? = myUserId
   override fun isAuthorized():Boolean = _auth.value === Auth.Ready
   override fun sendAuthorization(function:TdApi.Function<*>) { send(function) }
   override fun isReactionChosen(messageId:Long,emoji:String):Boolean =
    _messages.value.firstOrNull { it.id==messageId }?.reactions?.any { it.emoji==emoji && it.chosen } == true
   override fun clearOperationError() { operationErrorController.clear() }
   override fun reportOperationError(message:String) { operationErrorController.report(message) }
  },
  pushRegistrationController,chatListLoader,chatActions,profileController,contactsController,
  userActions,chatRegistrationController,privacyController,storyActions,storyStateController,
  chatConversationController,accountActions,messageActions,animatedMessageSendController,
  operationErrorController)

 init {
  synchronized(updateLock) {
   updateStateReady=true
   while(!pendingUpdates.isEmpty()) dispatchUpdate(pendingUpdates.removeFirst())
  }
 }

 
}