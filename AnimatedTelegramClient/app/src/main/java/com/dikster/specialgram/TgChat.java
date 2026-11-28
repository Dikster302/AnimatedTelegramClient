package com.dikster.specialgram;

import java.util.Objects;

/** Immutable chat-list row model shared by TDLib and native Java UI. */
public final class TgChat {
    private final long id,lastReadOutboxMessageId,pinnedOrder;
    private final String title,preview,avatarPath,kind,status;
    private final int lastDate,unreadCount;
    private final boolean archived,online,premium,hasStory,hasUnviewedStory,pinned,markedAsUnread,muted;

    public TgChat(long id,String title,String preview,int lastDate,int unreadCount,String avatarPath,String kind,
                  boolean archived,String status,boolean online,boolean premium,long lastReadOutboxMessageId,
                  boolean hasStory,boolean hasUnviewedStory,boolean pinned,boolean markedAsUnread,boolean muted){
        this(id,title,preview,lastDate,unreadCount,avatarPath,kind,archived,status,online,premium,
                lastReadOutboxMessageId,hasStory,hasUnviewedStory,pinned,markedAsUnread,muted,0L);
    }

    public TgChat(long id,String title,String preview,int lastDate,int unreadCount,String avatarPath,String kind,
                  boolean archived,String status,boolean online,boolean premium,long lastReadOutboxMessageId,
                  boolean hasStory,boolean hasUnviewedStory,boolean pinned,boolean markedAsUnread,boolean muted,
                  long pinnedOrder){
        this.id=id;this.title=safe(title);this.preview=safe(preview);this.lastDate=lastDate;
        this.unreadCount=unreadCount;this.avatarPath=safe(avatarPath);this.kind=safe(kind);
        this.archived=archived;this.status=safe(status);this.online=online;this.premium=premium;
        this.lastReadOutboxMessageId=lastReadOutboxMessageId;this.hasStory=hasStory;
        this.hasUnviewedStory=hasUnviewedStory;this.pinned=pinned;this.markedAsUnread=markedAsUnread;
        this.muted=muted;this.pinnedOrder=pinnedOrder;
    }
    public long getId(){return id;}public String getTitle(){return title;}public String getPreview(){return preview;}
    public int getLastDate(){return lastDate;}public int getUnreadCount(){return unreadCount;}
    public String getAvatarPath(){return avatarPath;}public String getKind(){return kind;}
    public boolean getArchived(){return archived;}public String getStatus(){return status;}
    public boolean isOnline(){return online;}public boolean isPremium(){return premium;}
    public long getLastReadOutboxMessageId(){return lastReadOutboxMessageId;}
    public boolean getHasStory(){return hasStory;}public boolean getHasUnviewedStory(){return hasUnviewedStory;}
    public boolean isPinned(){return pinned;}public boolean isMarkedAsUnread(){return markedAsUnread;}
    public boolean isMuted(){return muted;}public long getPinnedOrder(){return pinnedOrder;}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof TgChat))return false;
        TgChat v=(TgChat)o;return id==v.id&&lastDate==v.lastDate&&unreadCount==v.unreadCount&&archived==v.archived
                &&online==v.online&&premium==v.premium&&lastReadOutboxMessageId==v.lastReadOutboxMessageId
                &&hasStory==v.hasStory&&hasUnviewedStory==v.hasUnviewedStory&&pinned==v.pinned
                &&markedAsUnread==v.markedAsUnread&&muted==v.muted&&pinnedOrder==v.pinnedOrder
                &&title.equals(v.title)&&preview.equals(v.preview)&&avatarPath.equals(v.avatarPath)
                &&kind.equals(v.kind)&&status.equals(v.status);}
    @Override public int hashCode(){return Objects.hash(id,title,preview,lastDate,unreadCount,avatarPath,kind,
            archived,status,online,premium,lastReadOutboxMessageId,hasStory,hasUnviewedStory,pinned,
            markedAsUnread,muted,pinnedOrder);}
    private static String safe(String v){return v==null?"":v;}
}
