package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Applies TDLib chat-list and read-marker updates to immutable UI state. */
public final class TdChatStateController {
    public static final class State {
        public final List<TgChat> chats;
        public final List<TgMessage> messages;
        State(List<TgChat> chats, List<TgMessage> messages) {
            this.chats = chats;
            this.messages = messages;
        }
    }

    private TdChatStateController() { }

    public static List<TgChat> updateTitle(List<TgChat> source, long chatId, String title) {
        List<TgChat> result = new ArrayList<>(source.size());
        for (TgChat chat : source) result.add(chat.getId() == chatId
                ? copyChat(chat, title, chat.getPreview(), chat.getLastDate(), chat.getUnreadCount(),
                    chat.getLastReadOutboxMessageId(), chat.isPinned(), chat.isMarkedAsUnread(), chat.isMuted())
                : chat);
        return result;
    }

    public static List<TgChat> updatePreview(List<TgChat> source, long chatId, TdApi.Message message) {
        List<TgChat> result = new ArrayList<>(source.size());
        for (TgChat chat : source) {
            if (chat.getId() == chatId) {
                String preview = message == null ? chat.getPreview()
                        : TdMessageTextFormatter.preview(message.content);
                int date = message == null ? chat.getLastDate() : message.date;
                result.add(copyChat(chat, chat.getTitle(), preview, date, chat.getUnreadCount(),
                        chat.getLastReadOutboxMessageId(), chat.isPinned(), chat.isMarkedAsUnread(), chat.isMuted()));
            } else result.add(chat);
        }
        sortChats(result);
        return result;
    }

    public static List<TgChat> updateUnreadCount(List<TgChat> source, long chatId, int unreadCount) {
        List<TgChat> result = new ArrayList<>(source.size());
        for (TgChat chat : source) result.add(chat.getId() == chatId
                ? copyChat(chat, chat.getTitle(), chat.getPreview(), chat.getLastDate(), unreadCount,
                    chat.getLastReadOutboxMessageId(), chat.isPinned(), chat.isMarkedAsUnread(), chat.isMuted())
                : chat);
        return result;
    }

    public static List<TgChat> updatePinned(List<TgChat> source, long chatId, TdApi.ChatPosition position) {
        if (!(position.list instanceof TdApi.ChatListMain)
                && !(position.list instanceof TdApi.ChatListArchive)) return source;
        boolean archived = position.list instanceof TdApi.ChatListArchive;
        List<TgChat> result = new ArrayList<>(source.size());
        for (TgChat chat : source) result.add(chat.getId() == chatId
                ? copyChatWithPosition(chat, archived, position.isPinned, position.order)
                : chat);
        sortChats(result);
        return result;
    }

    /** TDLib order is descending: preserve its exact order among pinned chats. */
    static void sortChats(List<TgChat> chats) {
        chats.sort(Comparator.comparing(TgChat::isPinned).reversed()
                .thenComparing(Comparator.comparingLong(TgChat::getPinnedOrder).reversed())
                .thenComparing(Comparator.comparingInt(TgChat::getLastDate).reversed()));
    }

    private static TgChat copyChatWithPosition(TgChat c, boolean archived, boolean pinned, long order) {
        return new TgChat(c.getId(), c.getTitle(), c.getPreview(), c.getLastDate(),
                c.getUnreadCount(), c.getAvatarPath(), c.getKind(), archived, c.getStatus(),
                c.isOnline(), c.isPremium(), c.getLastReadOutboxMessageId(), c.getHasStory(),
                c.getHasUnviewedStory(), pinned, c.isMarkedAsUnread(), c.isMuted(),
                pinned ? order : 0L);
    }

    public static List<TgChat> updateMarkedUnread(List<TgChat> source, long chatId, boolean unread) {
        List<TgChat> result = new ArrayList<>(source.size());
        for (TgChat chat : source) result.add(chat.getId() == chatId
                ? copyChat(chat, chat.getTitle(), chat.getPreview(), chat.getLastDate(), chat.getUnreadCount(),
                    chat.getLastReadOutboxMessageId(), chat.isPinned(), unread, chat.isMuted())
                : chat);
        return result;
    }

    public static List<TgChat> updateMuted(List<TgChat> source, long chatId, TdApi.ChatNotificationSettings settings) {
        boolean muted = settings != null && settings.muteFor > 0;
        List<TgChat> result = new ArrayList<>(source.size());
        for (TgChat chat : source) result.add(chat.getId() == chatId
                ? copyChat(chat, chat.getTitle(), chat.getPreview(), chat.getLastDate(), chat.getUnreadCount(),
                    chat.getLastReadOutboxMessageId(), chat.isPinned(), chat.isMarkedAsUnread(), muted)
                : chat);
        return result;
    }

    public static State updateOutboxRead(List<TgChat> chats, List<TgMessage> messages,
                                         Long activeChatId, long chatId, long lastReadId) {
        List<TgChat> updatedChats = new ArrayList<>(chats.size());
        for (TgChat chat : chats) updatedChats.add(chat.getId() == chatId
                ? copyChat(chat, chat.getTitle(), chat.getPreview(), chat.getLastDate(), chat.getUnreadCount(),
                    lastReadId, chat.isPinned(), chat.isMarkedAsUnread(), chat.isMuted())
                : chat);

        List<TgMessage> updatedMessages = messages;
        if (activeChatId != null && activeChatId == chatId) {
            updatedMessages = new ArrayList<>(messages.size());
            for (TgMessage message : messages) {
                if (message.getOutgoing()) {
                    updatedMessages.add(copyRead(message, message.getId() <= lastReadId));
                } else updatedMessages.add(message);
            }
        }
        return new State(updatedChats, updatedMessages);
    }

    private static TgChat copyChat(TgChat c, String title, String preview, int lastDate,
                                   int unreadCount, long readId, boolean pinned,
                                   boolean markedUnread, boolean muted) {
        return new TgChat(c.getId(), title, preview, lastDate, unreadCount, c.getAvatarPath(),
                c.getKind(), c.getArchived(), c.getStatus(), c.isOnline(), c.isPremium(), readId,
                c.getHasStory(), c.getHasUnviewedStory(), pinned, markedUnread, muted,c.getPinnedOrder());
    }

    private static TgMessage copyRead(TgMessage m, boolean read) {
        return new TgMessage(m.getId(), m.getText(), m.getOutgoing(), m.getDate(), read,
                m.getVideoShape(), m.getVideoPath(), m.getReplyToMessageId(), m.getReplyToText(),
                m.getReactions(), m.getPhotoPath(), m.getMediaKind(), m.getMediaPath(),
                m.getMediaName(), m.getMediaMimeType(), m.getMediaDuration(), m.getMediaSize(),
                m.isPinned());
    }
}
