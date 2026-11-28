package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converts TDLib message objects into the UI model without Kotlin-side media branching. */
public final class TdMessageMapper {
    public interface ReplyResolver {
        String findText(long messageId);
    }
    public interface MediaListener {
        void onMediaFile(TdApi.File file, long messageId, String kind);
    }

    private TdMessageMapper() { }

    public static List<TgReaction> mapReactions(TdApi.MessageInteractionInfo info) {
        if (info == null || info.reactions == null || info.reactions.reactions == null)
            return Collections.emptyList();
        List<TgReaction> result = new ArrayList<>();
        for (TdApi.MessageReaction reaction : info.reactions.reactions) {
            if (reaction.type instanceof TdApi.ReactionTypeEmoji) {
                String emoji = ((TdApi.ReactionTypeEmoji) reaction.type).emoji;
                result.add(new TgReaction(emoji, reaction.totalCount, reaction.isChosen));
            }
        }
        return result;
    }

    public static TgMessage map(TdApi.Message message, long readMarker,
                                ReplyResolver replies, MediaListener media) {
        TdApi.MessageContent content = message.content;
        List<TgReaction> reactions = mapReactions(message.interactionInfo);
        long replyId = 0L;
        String replyText = "";
        if (message.replyTo instanceof TdApi.MessageReplyToMessage) {
            TdApi.MessageReplyToMessage reply = (TdApi.MessageReplyToMessage) message.replyTo;
            replyId = reply.messageId;
            if (replies != null) replyText = safe(replies.findText(replyId));
            if (replyText.isEmpty() && reply.content instanceof TdApi.MessageText) {
                replyText = safe(((TdApi.MessageText) reply.content).text.text);
            }
        }
        boolean read = message.isOutgoing && message.id <= readMarker;

        if (content instanceof TdApi.MessagePhoto) {
            TdApi.Photo photo = ((TdApi.MessagePhoto) content).photo;
            TdApi.File file = photo.sizes.length == 0 ? null : photo.sizes[photo.sizes.length - 1].photo;
            String path = file == null ? "" : safe(file.local.path);
            if (file != null && !file.local.isDownloadingCompleted && media != null)
                media.onMediaFile(file, message.id, "photo");
            String caption = ((TdApi.MessagePhoto) content).caption == null ? "" :
                    safe(((TdApi.MessagePhoto) content).caption.text);
            return new TgMessage(message.id, caption.isEmpty() ? "📷 Фото" : caption,
                    message.isOutgoing, message.date, read, "circle", "", replyId, replyText,
                    reactions, path, "photo", path, "", "", 0, file == null ? 0L : file.size,
                    message.isPinned);
        }
        if (content instanceof TdApi.MessageDocument) {
            TdApi.Document document = ((TdApi.MessageDocument) content).document;
            TdApi.File file = document.document;
            notifyMedia(media, file, message.id, "document");
            String caption = ((TdApi.MessageDocument) content).caption == null ? "" :
                    safe(((TdApi.MessageDocument) content).caption.text);
            String name = safe(document.fileName);
            return new TgMessage(message.id, caption.isEmpty() ? "📎 " + (name.isEmpty() ? "Файл" : name) : caption,
                    message.isOutgoing, message.date, read, "circle", "", replyId, replyText,
                    reactions, "", "document", safe(file.local.path), name, safe(document.mimeType),
                    0, file.size, message.isPinned);
        }
        if (content instanceof TdApi.MessageSticker) {
            TdApi.Sticker sticker = ((TdApi.MessageSticker) content).sticker;
            TdApi.File file = sticker.sticker;
            notifyMedia(media, file, message.id, "sticker");
            return new TgMessage(message.id, safe(sticker.emoji).isEmpty() ? "Стикер" : sticker.emoji,
                    message.isOutgoing, message.date, read, "circle", "", replyId, replyText,
                    reactions, "", "sticker", safe(file.local.path), "", "image/webp",
                    0, file.size, message.isPinned);
        }
        if (content instanceof TdApi.MessageAnimation) {
            TdApi.Animation animation = ((TdApi.MessageAnimation) content).animation;
            TdApi.File file = animation.animation;
            notifyMedia(media, file, message.id, "animation");
            String caption = ((TdApi.MessageAnimation) content).caption == null ? "" :
                    safe(((TdApi.MessageAnimation) content).caption.text);
            return new TgMessage(message.id, caption.isEmpty() ? "GIF" : caption,
                    message.isOutgoing, message.date, read, "circle", "", replyId, replyText,
                    reactions, "", "animation", safe(file.local.path), "", safe(animation.mimeType),
                    0, file.size, message.isPinned);
        }
        if (content instanceof TdApi.MessageAudio) {
            TdApi.Audio audio = ((TdApi.MessageAudio) content).audio;
            TdApi.File file = audio.audio;
            notifyMedia(media, file, message.id, "audio");
            String title = safe(audio.title);
            String performer = safe(audio.performer);
            String name = performer.isEmpty() ? title : title.isEmpty() ? performer : performer + " — " + title;
            return new TgMessage(message.id, "🎵 " + (title.isEmpty() ? "Аудио" : title),
                    message.isOutgoing, message.date, read, "circle", "", replyId, replyText,
                    reactions, "", "audio", safe(file.local.path), name, safe(audio.mimeType),
                    audio.duration, file.size, message.isPinned);
        }
        if (content instanceof TdApi.MessageVoiceNote) {
            TdApi.VoiceNote voice = ((TdApi.MessageVoiceNote) content).voiceNote;
            TdApi.File file = voice.voice;
            notifyMedia(media, file, message.id, "voice");
            return new TgMessage(message.id, "🎤 Голосовое сообщение", message.isOutgoing,
                    message.date, read, "circle", "", replyId, replyText, reactions, "", "voice",
                    safe(file.local.path), "Голосовое сообщение", safe(voice.mimeType),
                    voice.duration, file.size, message.isPinned);
        }
        if (content instanceof TdApi.MessageVideo) {
            TdApi.Video video = ((TdApi.MessageVideo) content).video;
            TdApi.File file = video.video;
            String caption = ((TdApi.MessageVideo) content).caption == null ? "" :
                    safe(((TdApi.MessageVideo) content).caption.text);
            String shape = TdMessageActionsController.decodeVideoShape(caption);
            notifyMedia(media, file, message.id, "video");
            return new TgMessage(message.id, "🎥 Видео · " + TdMessageTextFormatter.videoShapeTitle(shape),
                    message.isOutgoing, message.date, read, shape, safe(file.local.path), replyId,
                    replyText, reactions, "", "", "", "", "", 0, 0L, message.isPinned);
        }
        if (content instanceof TdApi.MessageVideoNote) {
            TdApi.File file = ((TdApi.MessageVideoNote) content).videoNote.video;
            notifyMedia(media, file, message.id, "videoNote");
            return new TgMessage(message.id, "🎥 Видеокружок", message.isOutgoing, message.date,
                    read, "circle", safe(file.local.path), replyId, replyText, reactions,
                    "", "", "", "", "", 0, 0L, message.isPinned);
        }
        return new TgMessage(message.id, TdMessageTextFormatter.display(content),
                message.isOutgoing, message.date, read, "circle", "", replyId, replyText,
                reactions, "", "", "", "", "", 0, 0L, message.isPinned);
    }

    private static void notifyMedia(MediaListener listener, TdApi.File file,
                                    long messageId, String kind) {
        if (listener != null && file != null && !file.local.isDownloadingCompleted)
            listener.onMediaFile(file, messageId, kind);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
