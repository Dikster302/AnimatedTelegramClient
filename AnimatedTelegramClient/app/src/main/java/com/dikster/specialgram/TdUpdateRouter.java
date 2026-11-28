package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Routes the top-level TDLib update stream to focused state controllers. */
public final class TdUpdateRouter {
    public interface Listener {
        void onAuthorizationState(TdApi.AuthorizationState state);
        void onNewChat(TdApi.Chat chat);
        void onChatTitle(long chatId, String title);
        void onChatActiveStories(TdApi.ChatActiveStories stories);
        void onStory(TdApi.Story story);
        void onStoryDeleted(long chatId, int storyId);
        void onChatLastMessage(long chatId, TdApi.Message message);
        void onChatReadOutbox(long chatId, long lastReadMessageId);
        void onChatReadInbox(long chatId, int unreadCount);
        void onChatPosition(long chatId, TdApi.ChatPosition position);
        void onChatMarkedUnread(long chatId, boolean markedUnread);
        void onChatNotificationSettings(long chatId, TdApi.ChatNotificationSettings settings);
        void onOtherUpdate(TdApi.Object update);
    }

    private final Listener listener;

    public TdUpdateRouter(Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener == null");
        this.listener = listener;
    }

    public void route(TdApi.Object update) {
        if (update instanceof TdApi.UpdateAuthorizationState) {
            listener.onAuthorizationState(((TdApi.UpdateAuthorizationState) update).authorizationState);
        } else if (update instanceof TdApi.UpdateNewChat) {
            listener.onNewChat(((TdApi.UpdateNewChat) update).chat);
        } else if (update instanceof TdApi.UpdateChatTitle) {
            TdApi.UpdateChatTitle value = (TdApi.UpdateChatTitle) update;
            listener.onChatTitle(value.chatId, value.title);
        } else if (update instanceof TdApi.UpdateChatActiveStories) {
            listener.onChatActiveStories(((TdApi.UpdateChatActiveStories) update).activeStories);
        } else if (update instanceof TdApi.UpdateStory) {
            listener.onStory(((TdApi.UpdateStory) update).story);
        } else if (update instanceof TdApi.UpdateStoryDeleted) {
            TdApi.UpdateStoryDeleted value = (TdApi.UpdateStoryDeleted) update;
            listener.onStoryDeleted(value.storyPosterChatId, value.storyId);
        } else if (update instanceof TdApi.UpdateChatLastMessage) {
            TdApi.UpdateChatLastMessage value = (TdApi.UpdateChatLastMessage) update;
            listener.onChatLastMessage(value.chatId, value.lastMessage);
        } else if (update instanceof TdApi.UpdateChatReadOutbox) {
            TdApi.UpdateChatReadOutbox value = (TdApi.UpdateChatReadOutbox) update;
            listener.onChatReadOutbox(value.chatId, value.lastReadOutboxMessageId);
        } else if (update instanceof TdApi.UpdateChatReadInbox) {
            TdApi.UpdateChatReadInbox value = (TdApi.UpdateChatReadInbox) update;
            listener.onChatReadInbox(value.chatId, value.unreadCount);
        } else if (update instanceof TdApi.UpdateChatPosition) {
            TdApi.UpdateChatPosition value = (TdApi.UpdateChatPosition) update;
            listener.onChatPosition(value.chatId, value.position);
        } else if (update instanceof TdApi.UpdateChatIsMarkedAsUnread) {
            TdApi.UpdateChatIsMarkedAsUnread value = (TdApi.UpdateChatIsMarkedAsUnread) update;
            listener.onChatMarkedUnread(value.chatId, value.isMarkedAsUnread);
        } else if (update instanceof TdApi.UpdateChatNotificationSettings) {
            TdApi.UpdateChatNotificationSettings value = (TdApi.UpdateChatNotificationSettings) update;
            listener.onChatNotificationSettings(value.chatId, value.notificationSettings);
        } else {
            listener.onOtherUpdate(update);
        }
    }
}
