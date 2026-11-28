package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Registers TDLib chat snapshots and hands off related user, story and avatar requests. */
public final class TdChatRegistrationController {
    public interface Listener {
        List<TgChat> chats();
        void setChats(List<TgChat> chats);
        void enqueueChatStories(long chatId);
        void onPrivateChatUser(long userId, long chatId);
        void onChatPhoto(int fileId, long chatId);
    }

    private final Listener listener;

    public TdChatRegistrationController(Listener listener) {
        this.listener = listener;
    }

    public TgChat putChat(TdApi.Chat source, boolean archived) {
        TgChat previous = null;
        for (TgChat chat : listener.chats()) {
            if (chat.getId() == source.id) {
                previous = chat;
                break;
            }
        }
        TgChat item = TdChatMapper.map(source, archived, previous);
        List<TgChat> chats = new ArrayList<>();
        for (TgChat chat : listener.chats()) {
            if (chat.getId() != source.id) chats.add(chat);
        }
        chats.add(item);
        chats.sort(Comparator.comparingInt(TgChat::getLastDate).reversed());
        listener.setChats(chats);

        listener.enqueueChatStories(source.id);
        Long userId = TdChatMapper.privateUserId(source);
        if (userId != null) listener.onPrivateChatUser(userId, source.id);

        int photoId = TdChatMapper.photoFileId(source);
        if (photoId > 0 && item.getAvatarPath().trim().isEmpty()) {
            listener.onChatPhoto(photoId, source.id);
        }
        return item;
    }
}
