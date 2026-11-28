package com.dikster.specialgram;

import android.content.Context;

/**
 * Java-owned public TDLib client API.
 *
 * Android UI and services depend only on this Java facade. The remaining
 * Kotlin implementation is isolated behind it while its internals are being
 * ported controller-by-controller.
 */
public final class TdLibClient implements AutoCloseable {
    private final TdLibClientLegacy delegate;

    public TdLibClient(Context context) {
        delegate = new TdLibClientLegacy(context);
    }

    public TdLibClientState<Auth> getAuth() { return delegate.getAuth(); }
    public TdLibClientState<java.util.List<TgChat>> getChats() { return delegate.getChats(); }
    public TdLibClientState<java.util.List<TgMessage>> getMessages() { return delegate.getMessages(); }
    public TdLibClientState<java.util.List<TgMessage>> getSearchResults() { return delegate.getSearchResults(); }
    public TdLibClientState<java.util.List<TgStory>> getStories() { return delegate.getStories(); }
    public TdLibClientState<java.util.List<TgContact>> getContacts() { return delegate.getContacts(); }
    public TdLibClientState<TgProfile> getProfile() { return delegate.getProfile(); }
    public TdLibClientState<TgAccount> getAccount() { return delegate.getAccount(); }
    public TdLibClientState<String> getPrivacyLastSeen() { return delegate.getPrivacyLastSeen(); }
    public TdLibClientState<String> getPrivacyPhone() { return delegate.getPrivacyPhone(); }
    public TdLibClientState<String> getPrivacyPhoto() { return delegate.getPrivacyPhoto(); }
    public TdLibClientState<String> getOperationError() { return delegate.getOperationError(); }

    public void updatePushToken(String token) { delegate.updatePushToken(token); }
    public void processPushPayload(String payload) { delegate.processPushPayload(payload); }
    public void phone(String value) { delegate.phone(value); }
    public void code(String value) { delegate.code(value); }
    public void password(String value) { delegate.password(value); }

    public void loadChats() { delegate.loadChats(); }
    public void archiveChat(long id, boolean archived) { delegate.archiveChat(id, archived); }
    public void toggleChatPinned(long id, boolean pinned, boolean archived) {
        delegate.toggleChatPinned(id, pinned, archived);
    }
    public void toggleChatUnread(long id, boolean unread) { delegate.toggleChatUnread(id, unread); }
    public void toggleChatMuted(long id, boolean muted) { delegate.toggleChatMuted(id, muted); }
    public void openProfile(long chatId) { delegate.openProfile(chatId); }
    public void loadContacts() { delegate.loadContacts(); }
    public void openContact(long userId, TdContactReadyListener callback) {
        delegate.openContact(userId, callback);
    }

    public void loadPrivacySettings() { delegate.loadPrivacySettings(); }
    public void setLastSeenPrivacy(String value) { delegate.setLastSeenPrivacy(value); }
    public void setPhonePrivacy(String value) { delegate.setPhonePrivacy(value); }
    public void setPhotoPrivacy(String value) { delegate.setPhotoPrivacy(value); }
    public void closeProfile() { delegate.closeProfile(); }

    public void openStory(long chatId, int storyId) { delegate.openStory(chatId, storyId); }
    public void closeStory(long chatId, int storyId) { delegate.closeStory(chatId, storyId); }
    public void reactToStory(long chatId, int storyId, String emoji) {
        delegate.reactToStory(chatId, storyId, emoji);
    }
    public void postPhotoStory(String path, String caption) { delegate.postPhotoStory(path, caption); }
    public void postVideoStory(String path, double duration) { delegate.postVideoStory(path, duration); }

    public void openChat(long id) { delegate.openChat(id); }
    public void closeChat() { delegate.closeChat(); }
    public void searchChatMessages(long chatId, String query) {
        delegate.searchChatMessages(chatId, query);
    }
    public void loadOlderMessages(long chatId) { delegate.loadOlderMessages(chatId); }

    public void clearOperationError() { delegate.clearOperationError(); }
    public void updateMyProfile(String firstName, String lastName, String bio) {
        delegate.updateMyProfile(firstName, lastName, bio);
    }
    public void setMyProfilePhoto(String path) { delegate.setMyProfilePhoto(path); }
    public void reportOperationError(String message) { delegate.reportOperationError(message); }

    public void sendPhoto(long chatId, String path, String caption) {
        delegate.sendPhoto(chatId, path, caption);
    }
    public void sendDocument(long chatId, String path, String caption) {
        delegate.sendDocument(chatId, path, caption);
    }
    public void sendVideo(long chatId, String path, String shape) {
        delegate.sendVideo(chatId, path, shape);
    }
    public void reactToMessage(long chatId, long messageId, String emoji) {
        delegate.reactToMessage(chatId, messageId, emoji);
    }
    public void forwardMessage(long fromChatId, long toChatId, long messageId) {
        delegate.forwardMessage(fromChatId, toChatId, messageId);
    }
    public void pinMessage(long chatId, long messageId, boolean pin) {
        delegate.pinMessage(chatId, messageId, pin);
    }
    public void deleteMessage(long chatId, long messageId, boolean revoke) {
        delegate.deleteMessage(chatId, messageId, revoke);
    }
    public void clearHistory(long chatId, boolean revoke) { delegate.clearHistory(chatId, revoke); }
    public void editMessage(long chatId, long messageId, String text) {
        delegate.editMessage(chatId, messageId, text);
    }
    public void sendPlain(long chatId, String text, Long replyToMessageId) {
        delegate.sendPlain(chatId, text, replyToMessageId);
    }
    public void replyToStory(long chatId, int storyId, String text) {
        delegate.replyToStory(chatId, storyId, text);
    }
    public void sendAnimated(long chatId, String text, Long replyToMessageId) {
        delegate.sendAnimated(chatId, text, replyToMessageId);
    }

    @Override public void close() { delegate.close(); }
}
