package com.dikster.specialgram.data;

import androidx.room.Entity;
import androidx.room.Index;

import com.dikster.specialgram.TgMessage;

/** Сохранённая версия сообщения; первичным источником данных остаётся TDLib. */
@Entity(tableName = "cached_messages", primaryKeys = {"chatId", "messageId"},
        indices = {@Index(value = {"chatId", "date"})})
public class MessageEntity {
    public long chatId;
    public long messageId;
    public String text;
    public boolean outgoing;
    public int date;
    public boolean read;
    public String videoShape;
    public String videoPath;
    public long replyToMessageId;
    public String replyToText;
    public String reactionsJson;
    public String photoPath;
    public String mediaKind;
    public String mediaPath;
    public String mediaName;
    public String mediaMimeType;
    public int mediaDuration;
    public long mediaSize;
    public boolean pinned;

    public MessageEntity() { }

    public static MessageEntity from(long chatId, TgMessage message) {
        MessageEntity row = new MessageEntity();
        row.chatId = chatId;
        row.messageId = message.getId();
        row.text = message.getText();
        row.outgoing = message.getOutgoing();
        row.date = message.getDate();
        row.read = message.isRead();
        row.videoShape = message.getVideoShape();
        row.videoPath = message.getVideoPath();
        row.replyToMessageId = message.getReplyToMessageId();
        row.replyToText = message.getReplyToText();
        row.reactionsJson = encodeReactions(message);
        row.photoPath = message.getPhotoPath();
        row.mediaKind = message.getMediaKind();
        row.mediaPath = message.getMediaPath();
        row.mediaName = message.getMediaName();
        row.mediaMimeType = message.getMediaMimeType();
        row.mediaDuration = message.getMediaDuration();
        row.mediaSize = message.getMediaSize();
        row.pinned = message.isPinned();
        return row;
    }

    public TgMessage toModel() {
        return new TgMessage(messageId, safe(text), outgoing, date, read, safe(videoShape),
                safe(videoPath), replyToMessageId, safe(replyToText), decodeReactions(reactionsJson),
                safe(photoPath), safe(mediaKind), safe(mediaPath), safe(mediaName),
                safe(mediaMimeType), mediaDuration, mediaSize, pinned);
    }

    private static String encodeReactions(TgMessage message) {
        org.json.JSONArray array = new org.json.JSONArray();
        for (com.dikster.specialgram.TgReaction reaction : message.getReactions()) {
            org.json.JSONObject item = new org.json.JSONObject();
            try {
                item.put("emoji", reaction.getEmoji());
                item.put("count", reaction.getCount());
                item.put("chosen", reaction.getChosen());
                array.put(item);
            } catch (org.json.JSONException ignored) { }
        }
        return array.toString();
    }

    private static java.util.List<com.dikster.specialgram.TgReaction> decodeReactions(String value) {
        java.util.List<com.dikster.specialgram.TgReaction> result = new java.util.ArrayList<>();
        if (value == null || value.isEmpty()) return result;
        try {
            org.json.JSONArray array = new org.json.JSONArray(value);
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject item = array.optJSONObject(i);
                if (item != null) result.add(new com.dikster.specialgram.TgReaction(
                        item.optString("emoji", ""), item.optInt("count", 0),
                        item.optBoolean("chosen", false)));
            }
        } catch (org.json.JSONException ignored) { }
        return result;
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
