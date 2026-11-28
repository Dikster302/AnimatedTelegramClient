package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Maps TDLib chat snapshots to the immutable chat-list UI model. */
public final class TdChatMapper {
    private TdChatMapper() { }

    public static TgChat map(TdApi.Chat chat, boolean archived, TgChat previous) {
        TdApi.Message last = chat.lastMessage;
        String preview = last == null ? "" : TdMessageTextFormatter.preview(last.content);
        int photoId = chat.photo == null || chat.photo.small == null ? 0 : chat.photo.small.id;
        String avatar = previous == null ? "" : previous.getAvatarPath();
        String status = previous == null ? "" : previous.getStatus();
        boolean online = previous != null && previous.isOnline();
        boolean premium = previous != null && previous.isPremium();
        boolean hasStory = previous != null && previous.getHasStory();
        boolean hasUnviewed = previous != null && previous.getHasUnviewedStory();
        boolean mainPinned = false, archivePinned = false;
        if (chat.positions != null) for (TdApi.ChatPosition position : chat.positions) {
            if (position.list instanceof TdApi.ChatListMain) mainPinned = position.isPinned;
            else if (position.list instanceof TdApi.ChatListArchive) archivePinned = position.isPinned;
        }
        String kind;
        if (chat.type instanceof TdApi.ChatTypePrivate) kind = "Личный чат";
        else if (chat.type instanceof TdApi.ChatTypeBasicGroup) kind = "Группа";
        else if (chat.type instanceof TdApi.ChatTypeSupergroup)
            kind = ((TdApi.ChatTypeSupergroup) chat.type).isChannel ? "Канал" : "Группа";
        else if (chat.type instanceof TdApi.ChatTypeSecret) kind = "Секретный чат";
        else kind = "Чат";

        return new TgChat(chat.id, safe(chat.title), preview, last == null ? 0 : last.date,
                chat.unreadCount, avatar, kind, archived, status, online, premium,
                chat.lastReadOutboxMessageId, hasStory, hasUnviewed,
                mainPinned || archivePinned, chat.isMarkedAsUnread,
                chat.notificationSettings != null && chat.notificationSettings.muteFor > 0);
    }

    public static String profileKind(TdApi.ChatType type) {
        if (type instanceof TdApi.ChatTypePrivate) return "Личный чат";
        if (type instanceof TdApi.ChatTypeBasicGroup) return "Группа";
        if (type instanceof TdApi.ChatTypeSupergroup)
            return ((TdApi.ChatTypeSupergroup) type).isChannel ? "Канал" : "Группа";
        if (type instanceof TdApi.ChatTypeSecret) return "Секретный чат";
        return "Чат";
    }

    public static int photoFileId(TdApi.Chat chat) {
        return chat.photo == null || chat.photo.small == null ? 0 : chat.photo.small.id;
    }

    public static Long privateUserId(TdApi.Chat chat) {
        return chat.type instanceof TdApi.ChatTypePrivate
                ? ((TdApi.ChatTypePrivate) chat.type).userId : null;
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
