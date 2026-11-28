package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.List;

/** Applies routed TDLib chat, story and authorization updates to shared state. */
public final class TdUpdateStateController {
    public interface State {
        List<TgChat> chats();
        void setChats(List<TgChat> chats);
        List<TgMessage> messages();
        void setMessages(List<TgMessage> messages);
        void markCachedMessagesRead(long chatId, long lastReadMessageId);
    }

    private final State state;
    private final TdUpdateRouter router;

    public TdUpdateStateController(State state,
                                   TdAuthorizationStateRouter authorization,
                                   TdChatRegistrationController chatRegistration,
                                   TdStoryStateController stories,
                                   TdChatSessionController chatSession,
                                   TdUserUpdateRouter users) {
        this.state = state;
        this.router = new TdUpdateRouter(new TdUpdateRouter.Listener() {
            @Override public void onAuthorizationState(TdApi.AuthorizationState value) {
                authorization.onState(value);
            }
            @Override public void onNewChat(TdApi.Chat chat) {
                chatRegistration.putChat(chat, false);
            }
            @Override public void onChatTitle(long chatId, String title) {
                state.setChats(TdChatStateController.updateTitle(state.chats(), chatId, title));
            }
            @Override public void onChatPhoto(long chatId, TdApi.ChatPhotoInfo photo) {
                chatRegistration.updatePhoto(chatId, photo);
            }
            @Override public void onChatActiveStories(TdApi.ChatActiveStories value) {
                stories.updateChatStories(value);
            }
            @Override public void onStory(TdApi.Story story) {
                stories.updateStory(story, null);
            }
            @Override public void onStoryDeleted(long chatId, int storyId) {
                stories.deleteStory(chatId, storyId);
            }
            @Override public void onChatLastMessage(long chatId, TdApi.Message message) {
                state.setChats(TdChatStateController.updatePreview(state.chats(), chatId, message));
            }
            @Override public void onChatReadOutbox(long chatId, long lastReadMessageId) {
                TdChatStateController.State updated = TdChatStateController.updateOutboxRead(
                        state.chats(), state.messages(), chatSession.getActiveChat(),
                        chatId, lastReadMessageId);
                state.setChats(updated.chats);
                state.setMessages(updated.messages);
                state.markCachedMessagesRead(chatId, lastReadMessageId);
            }
            @Override public void onChatReadInbox(long chatId, int unreadCount) {
                state.setChats(TdChatStateController.updateUnreadCount(state.chats(), chatId, unreadCount));
            }
            @Override public void onChatPosition(long chatId, TdApi.ChatPosition position) {
                state.setChats(TdChatStateController.updatePinned(state.chats(), chatId, position));
            }
            @Override public void onChatMarkedUnread(long chatId, boolean markedUnread) {
                state.setChats(TdChatStateController.updateMarkedUnread(state.chats(), chatId, markedUnread));
            }
            @Override public void onChatNotificationSettings(long chatId,
                                                               TdApi.ChatNotificationSettings settings) {
                state.setChats(TdChatStateController.updateMuted(state.chats(), chatId, settings));
            }
            @Override public void onOtherUpdate(TdApi.Object update) {
                users.route(update);
            }
        });
    }

    public void route(TdApi.Object update) {
        router.route(update);
    }
}
