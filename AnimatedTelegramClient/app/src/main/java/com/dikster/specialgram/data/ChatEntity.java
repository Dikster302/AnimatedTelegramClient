package com.dikster.specialgram.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.dikster.specialgram.TgChat;

/** Persisted, UI-ready chat snapshot. TDLib remains the source of truth. */
@Entity(tableName = "cached_chats")
public class ChatEntity {
    @PrimaryKey public long chatId;
    public String title;
    public String preview;
    public int lastDate;
    public int unreadCount;
    public String avatarPath;
    public String kind;
    public boolean archived;
    public String status;
    public boolean online;
    public boolean premium;
    public long lastReadOutboxMessageId;
    public boolean hasStory;
    public boolean hasUnviewedStory;
    public boolean pinned;
    public boolean markedAsUnread;
    public boolean muted;
    public long pinnedOrder;

    public ChatEntity() {}

    public static ChatEntity from(TgChat chat) {
        ChatEntity row = new ChatEntity();
        row.chatId = chat.getId();
        row.title = chat.getTitle();
        row.preview = chat.getPreview();
        row.lastDate = chat.getLastDate();
        row.unreadCount = chat.getUnreadCount();
        row.avatarPath = chat.getAvatarPath();
        row.kind = chat.getKind();
        row.archived = chat.getArchived();
        row.status = chat.getStatus();
        row.online = chat.isOnline();
        row.premium = chat.isPremium();
        row.lastReadOutboxMessageId = chat.getLastReadOutboxMessageId();
        row.hasStory = chat.getHasStory();
        row.hasUnviewedStory = chat.getHasUnviewedStory();
        row.pinned = chat.isPinned();
        row.markedAsUnread = chat.isMarkedAsUnread();
        row.muted = chat.isMuted();
        row.pinnedOrder = chat.getPinnedOrder();
        return row;
    }

    public TgChat toModel() {
        return new TgChat(chatId, title, preview, lastDate, unreadCount, avatarPath, kind,
                archived, status, online, premium, lastReadOutboxMessageId, hasStory,
                hasUnviewedStory, pinned, markedAsUnread, muted, pinnedOrder);
    }
}
