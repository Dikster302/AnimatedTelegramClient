package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Java application command facade over focused TDLib controllers. */
public final class TdClientOperationsController {
    public interface ContactListener { void onReady(TgChat chat); }

    public interface Listener {
        Long myUserId();
        boolean isAuthorized();
        void sendAuthorization(TdApi.Function<?> function);
        boolean isReactionChosen(long messageId, String emoji);
        void clearOperationError();
        void reportOperationError(String message);
    }

    private final Listener listener;
    private final TdPushRegistrationController push;
    private final TdChatListLoader chatList;
    private final TdChatActions chatActions;
    private final TdProfileController profile;
    private final TdContactsController contacts;
    private final TdUserActionsController userActions;
    private final TdChatRegistrationController chatRegistration;
    private final TdPrivacyController privacy;
    private final TdStoryActionsController storyActions;
    private final TdStoryStateController storyState;
    private final TdChatConversationController conversation;
    private final TdAccountActionsController accountActions;
    private final TdMessageActionsController messageActions;
    private final TdAnimatedMessageSendController animatedMessages;
    private final TdOperationErrorController operationErrors;

    public TdClientOperationsController(Listener listener,
            TdPushRegistrationController push, TdChatListLoader chatList,
            TdChatActions chatActions, TdProfileController profile,
            TdContactsController contacts, TdUserActionsController userActions,
            TdChatRegistrationController chatRegistration, TdPrivacyController privacy,
            TdStoryActionsController storyActions, TdStoryStateController storyState,
            TdChatConversationController conversation, TdAccountActionsController accountActions,
            TdMessageActionsController messageActions, TdAnimatedMessageSendController animatedMessages,
            TdOperationErrorController operationErrors) {
        this.listener = listener;
        this.push = push;
        this.chatList = chatList;
        this.chatActions = chatActions;
        this.profile = profile;
        this.contacts = contacts;
        this.userActions = userActions;
        this.chatRegistration = chatRegistration;
        this.privacy = privacy;
        this.storyActions = storyActions;
        this.storyState = storyState;
        this.conversation = conversation;
        this.accountActions = accountActions;
        this.messageActions = messageActions;
        this.animatedMessages = animatedMessages;
        this.operationErrors = operationErrors;
    }

    public void updatePushToken(String token) {
        push.updateToken(token, listener.isAuthorized());
    }

    public void processPushPayload(String payload) {
        if (payload != null && !payload.trim().isEmpty())
            sendAuthorization(TdAuthorizationController.processPushNotification(payload));
    }

    public void phone(String value) { sendAuthorization(TdAuthorizationController.setPhoneNumber(value)); }
    public void code(String value) { sendAuthorization(TdAuthorizationController.checkCode(value)); }
    public void password(String value) { sendAuthorization(TdAuthorizationController.checkPassword(value)); }

    private void sendAuthorization(TdApi.Function<?> function) {
        listener.sendAuthorization(function);
    }

    public void loadChats() {
        chatList.load(new TdApi.ChatListMain(), false);
        chatList.load(new TdApi.ChatListArchive(), true);
    }

    public void archiveChat(long id, boolean archived) { chatActions.archive(id, archived); }
    public void toggleChatPinned(long id, boolean pinned, boolean archived) {
        chatActions.togglePinned(id, pinned, archived);
    }
    public void toggleChatUnread(long id, boolean unread) { chatActions.toggleUnread(id, unread); }
    public void toggleChatMuted(long id, boolean muted) { chatActions.toggleMuted(id, muted); }
    public void openProfile(long chatId) { profile.openProfile(chatId); }
    public void loadContacts() { contacts.load(); }

    public void openContact(long userId, ContactListener callback) {
        userActions.createPrivateChat(userId, new TdUserActionsController.Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (!(result instanceof TdApi.Chat)) return;
                TdApi.Chat chat = (TdApi.Chat) result;
                TgChat mapped = chatRegistration.putChat(chat, false);
                if (mapped != null && callback != null) callback.onReady(mapped);
            }
            @Override public void onError(TdApi.Error error) { }
        });
    }

    public void loadPrivacySettings() { privacy.loadAll(); }
    public void setLastSeenPrivacy(String value) { privacy.set(TdPrivacyController.STATUS, value); }
    public void setPhonePrivacy(String value) { privacy.set(TdPrivacyController.PHONE, value); }
    public void setPhotoPrivacy(String value) { privacy.set(TdPrivacyController.PHOTO, value); }
    public void closeProfile() { profile.closeProfile(); }

    public void openStory(long chatId, int storyId) {
        storyActions.open(chatId, storyId);
        storyState.markViewed(chatId, storyId);
    }
    public void closeStory(long chatId, int storyId) { storyActions.close(chatId, storyId); }
    public void reactToStory(long chatId, int storyId, String emoji) {
        storyActions.react(chatId, storyId, emoji == null ? "❤️" : emoji);
    }

    public void postPhotoStory(String path, String caption) {
        listener.clearOperationError();
        Long userId = listener.myUserId();
        if (userId == null) {
            listener.reportOperationError("Не удалось определить аккаунт Telegram");
            return;
        }
        storyActions.postPhoto(userId, path, caption);
    }

    public void postVideoStory(String path, double duration) {
        listener.clearOperationError();
        Long userId = listener.myUserId();
        if (userId == null) {
            listener.reportOperationError("Не удалось определить аккаунт Telegram");
            return;
        }
        storyActions.postVideo(userId, path, duration);
    }

    public void openChat(long id) { conversation.openChat(id); }
    public void closeChat() { conversation.closeChat(); }
    public void searchChatMessages(long chatId, String query) {
        conversation.searchChatMessages(chatId, query);
    }
    public void loadOlderMessages(long chatId) { conversation.loadOlderMessages(chatId); }

    public void updateMyProfile(String firstName, String lastName, String bio) {
        Long id = listener.myUserId();
        accountActions.updateProfile(firstName, lastName, bio, id == null ? 0L : id);
    }
    public void setMyProfilePhoto(String path) {
        Long id = listener.myUserId();
        accountActions.setProfilePhoto(path, id == null ? 0L : id);
    }

    public void sendPhoto(long chatId, String path, String caption) {
        listener.clearOperationError();
        messageActions.sendPhoto(chatId, path, caption);
    }
    public void sendDocument(long chatId, String path, String caption) {
        listener.clearOperationError();
        messageActions.sendDocument(chatId, path, caption);
    }
    public void sendVideo(long chatId, String path, String shape) {
        listener.clearOperationError();
        messageActions.sendVideo(chatId, path, shape);
    }
    public void reactToMessage(long chatId, long messageId, String emoji) {
        listener.clearOperationError();
        messageActions.react(chatId, messageId, emoji,
                listener.isReactionChosen(messageId, emoji));
    }
    public void forwardMessage(long fromChatId, long toChatId, long messageId) {
        listener.clearOperationError();
        messageActions.forward(fromChatId, toChatId, messageId);
    }
    public void pinMessage(long chatId, long messageId, boolean pin) {
        listener.clearOperationError();
        messageActions.pin(chatId, messageId, pin);
    }
    public void deleteMessage(long chatId, long messageId, boolean revoke) {
        listener.clearOperationError();
        messageActions.delete(chatId, messageId, revoke);
    }
    public void clearHistory(long chatId, boolean revoke) {
        listener.clearOperationError();
        messageActions.clearHistory(chatId, revoke);
    }
    public void editMessage(long chatId, long messageId, String text) {
        listener.clearOperationError();
        messageActions.editText(chatId, messageId, text);
    }
    public void sendPlain(long chatId, String text, Long replyToMessageId) {
        listener.clearOperationError();
        messageActions.sendText(chatId, text, replyToMessageId);
    }
    public void replyToStory(long chatId, int storyId, String text) {
        listener.clearOperationError();
        messageActions.replyToStory(chatId, storyId, text);
    }
    public void sendAnimated(long chatId, String text, Long replyToMessageId) {
        animatedMessages.send(chatId, text, replyToMessageId);
    }

    public void clearOperationError() { operationErrors.clear(); }
    public void reportOperationError(String message) { operationErrors.report(message); }
}
