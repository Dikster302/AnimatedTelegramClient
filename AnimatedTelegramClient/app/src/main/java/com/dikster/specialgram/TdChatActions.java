package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Java command layer for archive, pin, unread and mute chat actions. */
public final class TdChatActions {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface Requester {
        void request(TdApi.Function<?> function, Callback callback);
    }
    public interface Listener {
        void onRefreshChats();
    }

    private final Requester requester;
    private final Listener listener;

    public TdChatActions(Requester requester, Listener listener) {
        this.requester = requester;
        this.listener = listener;
    }

    public void archive(long chatId, boolean archived) {
        TdApi.ChatList target = archived ? new TdApi.ChatListArchive() : new TdApi.ChatListMain();
        requester.request(new TdApi.AddChatToList(chatId, target), new Callback() {
            @Override public void onResult(TdApi.Object result) { listener.onRefreshChats(); }
            @Override public void onError(TdApi.Error error) { }
        });
    }

    public void togglePinned(long chatId, boolean pinned, boolean archived) {
        TdApi.ChatList list = archived ? new TdApi.ChatListArchive() : new TdApi.ChatListMain();
        requester.request(new TdApi.ToggleChatIsPinned(list, chatId, pinned), new Callback() {
            @Override public void onResult(TdApi.Object result) { listener.onRefreshChats(); }
            @Override public void onError(TdApi.Error error) { }
        });
    }

    public void toggleUnread(long chatId, boolean unread) {
        requester.request(new TdApi.ToggleChatIsMarkedAsUnread(chatId, unread), new Callback() {
            @Override public void onResult(TdApi.Object result) { }
            @Override public void onError(TdApi.Error error) { }
        });
    }

    public void toggleMuted(long chatId, boolean muted) {
        requester.request(new TdApi.GetChat(chatId), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (!(result instanceof TdApi.Chat)) return;
                TdApi.ChatNotificationSettings settings = ((TdApi.Chat) result).notificationSettings;
                settings.useDefaultMuteFor = false;
                settings.muteFor = muted ? 366 * 24 * 60 * 60 : 0;
                requester.request(new TdApi.SetChatNotificationSettings(chatId, settings), new Callback() {
                    @Override public void onResult(TdApi.Object ignored) { listener.onRefreshChats(); }
                    @Override public void onError(TdApi.Error error) { }
                });
            }
            @Override public void onError(TdApi.Error error) { }
        });
    }
}
