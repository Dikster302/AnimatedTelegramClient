package com.dikster.specialgram;

import android.content.Context;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import com.dikster.specialgram.data.ChatCacheRepository;

/** Native Java TDLib client: owns state, update routing, request dispatch and all controllers. */
public final class TdLibClient implements AutoCloseable {
    private final Context app;
    private final ChatCacheRepository chatCacheRepository;
    private final AtomicBoolean chatStateInitialized = new AtomicBoolean(false);
    private final TdLibClientState<Auth> auth = new TdLibClientState<>(Auth.Loading);
    private final TdLibClientState<List<TgChat>> chats = new TdLibClientState<>(Collections.emptyList());
    private final TdLibClientState<List<TgMessage>> messages = new TdLibClientState<>(Collections.emptyList());
    private final TdLibClientState<List<TgMessage>> searchResults = new TdLibClientState<>(Collections.emptyList());
    private final TdLibClientState<List<TgStory>> stories = new TdLibClientState<>(Collections.emptyList());
    private final TdLibClientState<List<TgContact>> contacts = new TdLibClientState<>(Collections.emptyList());
    private final TdLibClientState<TgProfile> profile = new TdLibClientState<>(null);
    private final TdLibClientState<TgAccount> account = new TdLibClientState<>(null);
    private final TdLibClientState<String> privacyLastSeen = new TdLibClientState<>("Загружаем…");
    private final TdLibClientState<String> privacyPhone = new TdLibClientState<>("Загружаем…");
    private final TdLibClientState<String> privacyPhoto = new TdLibClientState<>("Загружаем…");
    private final TdLibClientState<String> operationError = new TdLibClientState<>(null);

    private final TdChatSessionController chatSession = new TdChatSessionController();
    private final TdProfileSessionController profileSession = new TdProfileSessionController();
    private final Map<Long, List<String>> pendingEdits = new ConcurrentHashMap<>();
    private final Map<Integer, Long> fileToChat = new ConcurrentHashMap<>();
    private final Map<Integer, TdPair<Long, Long>> videoFileToMessage = new ConcurrentHashMap<>();
    private final Map<Integer, TdPair<Long, Long>> mediaFileToMessage = new ConcurrentHashMap<>();
    private final Map<Integer, TdPair<Long, Integer>> storyFileToKey = new ConcurrentHashMap<>();
    private final Map<Long, Long> userToChat = new ConcurrentHashMap<>();
    private final Map<Long, Long> confirmedIds = new ConcurrentHashMap<>();

    private volatile Long myUserId;
    private volatile Integer myPhotoFileId;
    private final Client client;
    private final TdRequestGateway requestGateway;
    private final TdFileDownloadQueue fileDownloadQueue;
    private final ChatStoryRequestQueue storyQueue;
    private final TdAnimatedMessageController animatedMessageController;
    private final TdUserFullInfoQueue userFullInfoQueue;
    private final TdMessageViewQueue messageViewQueue;
    private final TdUserActionsController userActions;
    private final TdProfileController profileController;
    private final TdAccountStateController accountStateController;
    private final TdUserUpdateStateController userUpdateStateController;
    private final TdUserUpdateRouter userUpdateRouter;
    private final TdAccountActionsController accountActions;
    private final TdChatHistoryController chatHistoryController;
    private final TdChatConversationController chatConversationController;
    private final TdStoryContentQueue storyContentQueue;
    private final TdStoryStateController storyStateController;
    private final TdStoryActionsController storyActions;
    private final TdMessageActionsController messageActions;
    private final TdMessageStateController messageStateController;
    private final TdFileStateController fileStateController;
    private final TdPushRegistrationController pushRegistrationController;
    private final TdPrivacyController privacyController;
    private final TdChatActions chatActions;
    private final TdContactsController contactsController;
    private final TdChatRegistrationController chatRegistrationController;
    private final TdChatListLoader chatListLoader;
    private final TdAuthorizationStateRouter authorizationStateRouter;
    private final TdUpdateStateController updateStateController;
    private final TdUpdateDispatchQueue updateDispatchQueue;
    private final TdOperationErrorController operationErrorController;
    private final TdAnimatedMessageSendController animatedMessageSendController;
    private final TdClientLifecycleController lifecycleController;
    private final TdClientOperationsController operationsController;

    public TdLibClient(Context context) {
        app = context.getApplicationContext();
        chatCacheRepository = new ChatCacheRepository(app);
        chatCacheRepository.load(cached -> {
            // Apply the disk snapshot only if no TDLib update has arrived first.
            if (!cached.isEmpty() && chatStateInitialized.compareAndSet(false, true)) {
                chats.setValue(cached);
            }
        });
        updateDispatchQueue = new TdUpdateDispatchQueue(this::dispatchUpdate);
        final TdStoryStateController[] storyStateRef = new TdStoryStateController[1];

        animatedMessageController = new TdAnimatedMessageController((function, callback) ->
                send(function, callback == null ? null : callback::onResult, null, true));

        fileDownloadQueue = new TdFileDownloadQueue(new TdFileDownloadQueue.Requester() {
            @Override public void send(TdApi.Function<?> function, TdFileDownloadQueue.Callback callback) {
                TdLibClient.this.send(function, callback::onResult, callback::onError, false);
            }
        });

        storyQueue = new ChatStoryRequestQueue(new ChatStoryRequestQueue.Requester() {
            @Override public void request(long chatId, ChatStoryRequestQueue.Callback callback) {
                TdLibClient.this.send(new TdApi.GetChatActiveStories(chatId),
                        callback::onResult, callback::onError, false);
            }
        }, result -> {
            if (result instanceof TdApi.ChatActiveStories)
                storyStateRef[0].updateChatStories((TdApi.ChatActiveStories) result);
        });

        TdMessageMappingController mapping = new TdMessageMappingController(
                new TdMessageMappingController.Listener() {
                    @Override public String replyText(long messageId) {
                        for (TgMessage item : messages.getValue())
                            if (item.getId() == messageId) return item.getText();
                        return "";
                    }
                    @Override public void enqueueMediaFile(TdApi.File file, long chatId,
                                                            long messageId, String kind) {
                        fileDownloadQueue.enqueue(file.id);
                    }
                }, videoFileToMessage, mediaFileToMessage);

        operationErrorController = new TdOperationErrorController(new TdOperationErrorController.Listener() {
            @Override public void onOperationError(String value) { operationError.setValue(value); }
            @Override public void onClearOperationError() { operationError.setValue(null); }
        });
        animatedMessageSendController = new TdAnimatedMessageSendController(
                animatedMessageController, confirmedIds, pendingEdits, this::clearOperationError);

        client = TdClientFactory.create(new TdClientFactory.Listener() {
            @Override public void onUpdate(TdApi.Object update) { updateDispatchQueue.accept(update); }
            @Override public void onUpdateError(Throwable error) {
                auth.setValue(new Auth.Error(error.getMessage() == null ? "TDLib error" : error.getMessage()));
            }
            @Override public void onFatalError(Throwable error) {
                auth.setValue(new Auth.Error(error.getMessage() == null ? "TDLib error" : error.getMessage()));
            }
        });
        TdRequestDispatcher dispatcher = new TdRequestDispatcher(app, client);
        requestGateway = new TdRequestGateway(dispatcher, operationError::setValue);

        userFullInfoQueue = new TdUserFullInfoQueue((function, callback) ->
                send(function, callback::onResult, callback::onError, false),
                (userId, info) -> {
                    if (myUserId != null && myUserId == userId)
                        account.setValue(TgModelStateController.withBio(account.getValue(),
                                info.bio == null ? "" : info.bio.text));
                });

        accountStateController = new TdAccountStateController(new TdAccountStateController.Listener() {
            @Override public TgAccount account() { return TdLibClient.this.account.getValue(); }
            @Override public void setAccount(TgAccount value) { TdLibClient.this.account.setValue(value); }
            @Override public void setMyUserId(long value) { myUserId = value; }
            @Override public void setMyPhotoFileId(Integer value) { myPhotoFileId = value; }
            @Override public void enqueueUserFullInfo(long userId) { userFullInfoQueue.enqueue(userId); }
            @Override public void enqueueFileDownload(int fileId) { fileDownloadQueue.enqueue(fileId); }
        });

        messageViewQueue = new TdMessageViewQueue((function, callback) ->
                send(function, callback::onResult, callback::onError, false));

        userActions = new TdUserActionsController((function, callback) ->
                send(function, callback::onResult, callback::onError, true));

        profileController = new TdProfileController(userActions, profileSession,
                new TdProfileController.Listener() {
                    @Override public TgChat findChat(long chatId) {
                        for (TgChat item : chats.getValue()) if (item.getId() == chatId) return item;
                        return null;
                    }
                    @Override public TgProfile profile() { return profile.getValue(); }
                    @Override public void setProfile(TgProfile value) { profile.setValue(value); }
                    @Override public void onPrivateChatUser(long userId, long chatId) { userToChat.put(userId, chatId); }
                    @Override public void onProfileUser(TdApi.User user, long chatId) {
                        setChats(TdUserStateController.updateChatUser(chats.getValue(), userToChat, user));
                        int photoId = user.profilePhoto == null || user.profilePhoto.small == null
                                ? 0 : user.profilePhoto.small.id;
                        if (photoId > 0) { fileToChat.put(photoId, chatId); fileDownloadQueue.enqueue(photoId); }
                    }
                });

        userUpdateStateController = new TdUserUpdateStateController(new TdUserUpdateStateController.State() {
            @Override public List<TgChat> chats() { return chats.getValue(); }
            @Override public void setChats(List<TgChat> value) { TdLibClient.this.setChats(value); }
            @Override public List<TgContact> contacts() { return contacts.getValue(); }
            @Override public void setContacts(List<TgContact> value) { contacts.setValue(value); }
            @Override public TgProfile profile() { return profile.getValue(); }
            @Override public void setProfile(TgProfile value) { profile.setValue(value); }
            @Override public Long myUserId() { return myUserId; }
            @Override public void setLastSeenPrivacy(String value) { privacyLastSeen.setValue(value); }
            @Override public void setPhonePrivacy(String value) { privacyPhone.setValue(value); }
            @Override public void setPhotoPrivacy(String value) { privacyPhoto.setValue(value); }
        }, userToChat, profileController, accountStateController);
        userUpdateRouter = new TdUserUpdateRouter(userUpdateStateController);

        accountActions = new TdAccountActionsController((function, callback) ->
                send(function, callback::onResult, callback::onError, true),
                new TdAccountActionsController.Listener() {
                    @Override public void onAccountUser(TdApi.User user) { accountStateController.updateAccount(user); }
                    @Override public void onBioUpdated(String bio) {
                        account.setValue(TgModelStateController.withBio(account.getValue(), bio));
                    }
                    @Override public void onValidationError(String message) { operationError.setValue(message); }
                });

        chatHistoryController = new TdChatHistoryController((function, callback) ->
                send(function, callback::onResult, callback::onError, false));
        chatConversationController = new TdChatConversationController(chatSession, chatHistoryController,
                new TdChatConversationController.Listener() {
                    @Override public List<TgChat> chats() { return chats.getValue(); }
                    @Override public void setChats(List<TgChat> value) { TdLibClient.this.setChats(value); }
                    @Override public List<TgMessage> messages() { return messages.getValue(); }
                    @Override public void setMessages(List<TgMessage> value) { setActiveMessages(value); }
                    @Override public void loadCachedMessages(long chatId, TdChatConversationController.CacheCallback callback) {
                        chatCacheRepository.loadMessages(chatId, callback::onLoaded);
                    }
                    @Override public void setSearchResults(List<TgMessage> value) { searchResults.setValue(value); }
                    @Override public TgMessage mapMessage(TdApi.Message value, long marker) {
                        return mapping.map(value, marker);
                    }
                    @Override public void enqueueViewedMessages(long chatId, long[] ids) {
                        messageViewQueue.enqueue(chatId, ids);
                    }
                });

        storyContentQueue = new TdStoryContentQueue((function, callback) ->
                send(function, callback::onResult, callback::onError, false),
                (story, viewed) -> storyStateRef[0].updateStory(story, viewed));

        storyStateController = new TdStoryStateController(new TdStoryStateController.Listener() {
            @Override public List<TgChat> chats() { return chats.getValue(); }
            @Override public List<TgStory> stories() { return stories.getValue(); }
            @Override public void setChats(List<TgChat> value) { TdLibClient.this.setChats(value); }
            @Override public void setStories(List<TgStory> value) { stories.setValue(value); }
            @Override public void enqueueStoryContent(long chatId, int storyId, boolean viewed) {
                storyContentQueue.enqueue(chatId, storyId, viewed);
            }
            @Override public void onStoryMediaFile(TdApi.File file, TgStory story) {
                storyFileToKey.put(file.id, new TdPair<>(story.getChatId(), story.getStoryId()));
                fileDownloadQueue.enqueue(file.id);
            }
        });
        storyStateRef[0] = storyStateController;

        storyActions = new TdStoryActionsController((function, callback) ->
                send(function, callback == null ? null : callback::onResult,
                        callback == null ? null : callback::onError, false),
                new TdStoryActionsController.Listener() {
                    @Override public void onStoryPosted(TdApi.Story story) {
                        storyStateController.updateStory(story, false);
                        Long userId = myUserId;
                        if (userId != null) {
                            List<TgChat> next = new ArrayList<>();
                            for (TgChat item : chats.getValue())
                                next.add(item.getId() == userId
                                        ? TgModelStateController.withStoryFlags(item, true, true) : item);
                            setChats(next);
                        }
                    }
                    @Override public void onValidationError(String message) { reportOperationError(message); }
                });

        messageActions = new TdMessageActionsController((function, callback) ->
                send(function, callback == null ? null : callback::onResult,
                        callback == null ? null : callback::onError, false),
                new TdMessageActionsController.Listener() {
                    @Override public void onMessageDeleted(long id) {
                        Long activeChat = chatSession.getActiveChat();
                        if (activeChat != null) chatCacheRepository.deleteMessages(activeChat, Collections.singletonList(id));
                        List<TgMessage> next = new ArrayList<>();
                        for (TgMessage item : messages.getValue()) if (item.getId() != id) next.add(item);
                        messages.setValue(next);
                    }
                    @Override public void onHistoryCleared(long chatId) {
                        if (Objects.equals(chatSession.getActiveChat(), chatId))
                            messages.setValue(Collections.emptyList());
                        chatCacheRepository.clearMessages(chatId);
                        List<TgChat> next = new ArrayList<>();
                        for (TgChat item : chats.getValue())
                            next.add(item.getId() == chatId ? TgModelStateController.clearHistoryPreview(item) : item);
                        setChats(next);
                    }
                    @Override public void onOperationError(String message) { reportOperationError(message); }
                });

        messageStateController = new TdMessageStateController(new TdMessageStateController.Listener() {
            @Override public Long activeChatId() { return chatSession.getActiveChat(); }
            @Override public List<TgChat> chats() { return chats.getValue(); }
            @Override public List<TgMessage> messages() { return messages.getValue(); }
            @Override public void setMessages(List<TgMessage> value) { setActiveMessages(value); }
            @Override public void deleteMessages(long chatId, List<Long> ids) { chatCacheRepository.deleteMessages(chatId, ids); }
            @Override public TgMessage mapMessage(TdApi.Message value, long marker) { return mapping.map(value, marker); }
            @Override public void enqueueViewedMessages(long chatId, long[] ids) { messageViewQueue.enqueue(chatId, ids); }
        }, animatedMessageController, confirmedIds, pendingEdits);

        fileStateController = new TdFileStateController(new TdFileStateController.Listener() {
            @Override public Long activeChatId() { return chatSession.getActiveChat(); }
            @Override public List<TgChat> chats() { return chats.getValue(); }
            @Override public void setChats(List<TgChat> value) { TdLibClient.this.setChats(value); }
            @Override public TgProfile profile() { return profile.getValue(); }
            @Override public void setProfile(TgProfile value) { profile.setValue(value); }
            @Override public List<TgMessage> messages() { return messages.getValue(); }
            @Override public void setMessages(List<TgMessage> value) { setActiveMessages(value); }
            @Override public List<TgStory> stories() { return stories.getValue(); }
            @Override public void setStories(List<TgStory> value) { stories.setValue(value); }
            @Override public TgAccount account() { return account.getValue(); }
            @Override public void setAccount(TgAccount value) { account.setValue(value); }
            @Override public void markDownloadCompleted(int fileId) { fileDownloadQueue.markCompleted(fileId); }
            @Override public boolean isMyPhotoFile(int fileId) { return myPhotoFileId != null && myPhotoFileId == fileId; }
            @Override public String persistAvatar(int fileId, String sourcePath) {
                return PersistentAvatarStore.persist(app, fileId, sourcePath);
            }
        }, fileToChat, videoFileToMessage, mediaFileToMessage, storyFileToKey);

        pushRegistrationController = new TdPushRegistrationController(app, (function, callback) ->
                send(function, callback::onResult, callback::onError, false));
        privacyController = new TdPrivacyController((function, callback) ->
                send(function, callback::onResult, callback::onError, true),
                (type, value) -> {
                    if (type == TdPrivacyController.STATUS) privacyLastSeen.setValue(value);
                    else if (type == TdPrivacyController.PHONE) privacyPhone.setValue(value);
                    else if (type == TdPrivacyController.PHOTO) privacyPhoto.setValue(value);
                });
        chatActions = new TdChatActions((function, callback) ->
                send(function, callback::onResult, callback::onError, true), this::loadChats);
        contactsController = new TdContactsController((function, callback) ->
                send(function, callback::onResult, callback::onError, false),
                user -> {
                    contacts.setValue(TdUserStateController.updateContact(contacts.getValue(), user));
                    setChats(TdUserStateController.updateChatUser(chats.getValue(), userToChat, user));
                });

        chatRegistrationController = new TdChatRegistrationController(new TdChatRegistrationController.Listener() {
            @Override public List<TgChat> chats() { return chats.getValue(); }
            @Override public void setChats(List<TgChat> value) { TdLibClient.this.setChats(value); }
            @Override public void enqueueChatStories(long chatId) { storyQueue.enqueue(chatId); }
            @Override public void onPrivateChatUser(long userId, long chatId) {
                userToChat.put(userId, chatId); contactsController.enqueueUser(userId);
            }
            @Override public void onChatPhoto(int fileId, long chatId) {
                fileToChat.put(fileId, chatId); fileDownloadQueue.enqueue(fileId);
            }
        });
        chatListLoader = new TdChatListLoader((function, callback) ->
                send(function, callback::onResult, callback::onError, false));

        if (BuildConfig.TELEGRAM_API_ID.isBlank() || BuildConfig.TELEGRAM_API_HASH.isBlank())
            auth.setValue(new Auth.Error("Set TELEGRAM_API_ID and TELEGRAM_API_HASH in GitHub Actions secrets."));

        authorizationStateRouter = new TdAuthorizationStateRouter(app, BuildConfig.TELEGRAM_API_ID,
                BuildConfig.TELEGRAM_API_HASH, new TdAuthorizationStateRouter.Listener() {
            @Override public void onAuthChanged(Auth value) { auth.setValue(value); }
            @Override public void onSend(TdApi.Function<?> function) { send(function); }
            @Override public void onAuthorized() {
                pushRegistrationController.onAuthorized();
                loadChats(); loadContacts(); loadPrivacySettings();
                send(new TdApi.GetMe(), result -> {
                    if (result instanceof TdApi.User) accountStateController.updateAccount((TdApi.User) result);
                }, null, true);
            }
        });

        updateStateController = new TdUpdateStateController(new TdUpdateStateController.State() {
            @Override public List<TgChat> chats() { return chats.getValue(); }
            @Override public void setChats(List<TgChat> value) { TdLibClient.this.setChats(value); }
            @Override public List<TgMessage> messages() { return messages.getValue(); }
            @Override public void setMessages(List<TgMessage> value) { setActiveMessages(value); }
        }, authorizationStateRouter, chatRegistrationController, storyStateController,
                chatSession, userUpdateRouter);
        lifecycleController = new TdClientLifecycleController(client,
                animatedMessageController::close, messageViewQueue::close, userFullInfoQueue::close,
                pushRegistrationController::close, fileDownloadQueue::close, contactsController::close,
                chatListLoader::close, storyContentQueue::close, storyQueue::close);

        operationsController = new TdClientOperationsController(new TdClientOperationsController.Listener() {
            @Override public Long myUserId() { return myUserId; }
            @Override public boolean isAuthorized() { return auth.getValue() == Auth.Ready; }
            @Override public void sendAuthorization(TdApi.Function<?> function) { send(function); }
            @Override public boolean isReactionChosen(long messageId, String emoji) {
                for (TgMessage item : messages.getValue()) if (item.getId() == messageId)
                    for (TgReaction reaction : item.getReactions())
                        if (reaction.getEmoji().equals(emoji) && reaction.getChosen()) return true;
                return false;
            }
            @Override public void clearOperationError() { operationErrorController.clear(); }
            @Override public void reportOperationError(String message) { operationErrorController.report(message); }
        }, pushRegistrationController, chatListLoader, chatActions, profileController, contactsController,
                userActions, chatRegistrationController, privacyController, storyActions, storyStateController,
                chatConversationController, accountActions, messageActions, animatedMessageSendController,
                operationErrorController);

        updateDispatchQueue.markReady();
    }

    private void setActiveMessages(List<TgMessage> value) {
        List<TgMessage> snapshot = value == null ? Collections.emptyList() : value;
        messages.setValue(snapshot);
        Long activeChat = chatSession.getActiveChat();
        if (activeChat != null) chatCacheRepository.upsertMessages(activeChat, snapshot);
    }

    private void setChats(List<TgChat> value) {
        chatStateInitialized.set(true);
        List<TgChat> snapshot = value == null ? Collections.emptyList() : value;
        chats.setValue(snapshot);
        chatCacheRepository.replaceAll(snapshot);
    }

    private void dispatchUpdate(TdApi.Object update) {
        if (messageStateController.handle(update) || fileStateController.handle(update)) return;
        updateStateController.route(update);
    }

    private void send(TdApi.Function<?> function) { send(function, null, null, true); }
    private void send(TdApi.Function<?> function, TdRequestGateway.ResultHandler result,
                      TdRequestGateway.ErrorHandler error, boolean reportError) {
        requestGateway.send(function, result, error, reportError);
    }

    public TdLibClientState<Auth> getAuth() { return auth; }
    public TdLibClientState<List<TgChat>> getChats() { return chats; }
    public TdLibClientState<List<TgMessage>> getMessages() { return messages; }
    public TdLibClientState<List<TgMessage>> getSearchResults() { return searchResults; }
    public TdLibClientState<List<TgStory>> getStories() { return stories; }
    public TdLibClientState<List<TgContact>> getContacts() { return contacts; }
    public TdLibClientState<TgProfile> getProfile() { return profile; }
    public TdLibClientState<TgAccount> getAccount() { return account; }
    public TdLibClientState<String> getPrivacyLastSeen() { return privacyLastSeen; }
    public TdLibClientState<String> getPrivacyPhone() { return privacyPhone; }
    public TdLibClientState<String> getPrivacyPhoto() { return privacyPhoto; }
    public TdLibClientState<String> getOperationError() { return operationError; }

    public void updatePushToken(String token) { operationsController.updatePushToken(token); }
    public void processPushPayload(String payload) { operationsController.processPushPayload(payload); }
    public void phone(String value) { operationsController.phone(value); }
    public void code(String value) { operationsController.code(value); }
    public void password(String value) { operationsController.password(value); }
    public void loadChats() { operationsController.loadChats(); }
    public void archiveChat(long id, boolean archived) { operationsController.archiveChat(id, archived); }
    public void toggleChatPinned(long id, boolean pinned, boolean archived) { operationsController.toggleChatPinned(id, pinned, archived); }
    public void toggleChatUnread(long id, boolean unread) { operationsController.toggleChatUnread(id, unread); }
    public void toggleChatMuted(long id, boolean muted) { operationsController.toggleChatMuted(id, muted); }
    public void openProfile(long id) { operationsController.openProfile(id); }
    public void searchProfileSection(long chatId,String tab,TdChatConversationController.ProfileSectionCallback callback) {
        operationsController.searchProfileSection(chatId,tab,callback);
    }
    public void loadAvailableGifts(TdUserActionsController.AvailableGiftsListener callback) {
        operationsController.loadAvailableGifts(callback);
    }
    public void sendGift(long userId,long giftId,TdUserActionsController.Callback callback) {
        operationsController.sendGift(userId,giftId,callback);
    }
    public void loadContacts() { operationsController.loadContacts(); }
    public void openSavedMessages(TdContactReadyListener callback) {
        operationsController.openSavedMessages(callback == null ? null : callback::onReady);
    }
    public void createBasicGroup(String title, long[] userIds, TdContactReadyListener callback) {
        operationsController.createBasicGroup(title, userIds, callback == null ? null : callback::onReady);
    }
    public void openContact(long id, TdContactReadyListener callback) {
        operationsController.openContact(id, callback == null ? null : callback::onReady);
    }
    public void loadPrivacySettings() { operationsController.loadPrivacySettings(); }
    public void setLastSeenPrivacy(String value) { operationsController.setLastSeenPrivacy(value); }
    public void setPhonePrivacy(String value) { operationsController.setPhonePrivacy(value); }
    public void setPhotoPrivacy(String value) { operationsController.setPhotoPrivacy(value); }
    public void closeProfile() { operationsController.closeProfile(); }
    public void openStory(long chatId, int storyId) { operationsController.openStory(chatId, storyId); }
    public void closeStory(long chatId, int storyId) { operationsController.closeStory(chatId, storyId); }
    public void reactToStory(long chatId, int storyId, String emoji) { operationsController.reactToStory(chatId, storyId, emoji); }
    public void postPhotoStory(String path, String caption) { operationsController.postPhotoStory(path, caption); }
    public void postVideoStory(String path, double duration) { operationsController.postVideoStory(path, duration); }
    public void openChat(long id) { operationsController.openChat(id); }
    public void closeChat() { operationsController.closeChat(); }
    public void searchChatMessages(long id, String query) { operationsController.searchChatMessages(id, query); }
    public void loadOlderMessages(long id) { operationsController.loadOlderMessages(id); }
    public void clearOperationError() { operationsController.clearOperationError(); }
    public void updateMyProfile(String first, String last, String bio) { operationsController.updateMyProfile(first, last, bio); }
    public void setMyProfilePhoto(String path) { operationsController.setMyProfilePhoto(path); }
    public void reportOperationError(String value) { operationsController.reportOperationError(value); }
    public void sendPhoto(long id, String path, String caption) { operationsController.sendPhoto(id, path, caption); }
    public void sendDocument(long id, String path, String caption) { operationsController.sendDocument(id, path, caption); }
    public void sendVideo(long id, String path, String shape) { operationsController.sendVideo(id, path, shape); }
    public void reactToMessage(long id, long messageId, String emoji) { operationsController.reactToMessage(id, messageId, emoji); }
    public void forwardMessage(long from, long to, long id) { operationsController.forwardMessage(from, to, id); }
    public void pinMessage(long chatId, long id, boolean pin) { operationsController.pinMessage(chatId, id, pin); }
    public void deleteMessage(long chatId, long id, boolean revoke) { operationsController.deleteMessage(chatId, id, revoke); }
    public void clearHistory(long chatId, boolean revoke) { operationsController.clearHistory(chatId, revoke); }
    public void editMessage(long chatId, long id, String text) { operationsController.editMessage(chatId, id, text); }
    public void sendPlain(long id, String text, Long reply) { operationsController.sendPlain(id, text, reply); }
    public void replyToStory(long chatId, int storyId, String text) { operationsController.replyToStory(chatId, storyId, text); }
    public void sendAnimated(long id, String text, Long reply) { operationsController.sendAnimated(id, text, reply); }

    @Override public void close() {
        lifecycleController.close();
        chatCacheRepository.close();
    }
}
