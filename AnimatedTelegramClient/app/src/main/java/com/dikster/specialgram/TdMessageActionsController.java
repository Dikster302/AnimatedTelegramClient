package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Java implementation of message, media and history commands. */
public final class TdMessageActionsController {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface Requester {
        void send(TdApi.Function<?> function, Callback callback);
    }
    public interface Listener {
        void onMessageDeleted(long messageId);
        void onHistoryCleared(long chatId);
    }

    private final Requester requester;
    private final Listener listener;

    public TdMessageActionsController(Requester requester, Listener listener) {
        this.requester = requester;
        this.listener = listener;
    }

    public void sendPhoto(long chatId, String path, String caption) {
        TdApi.InputPhoto photo = new TdApi.InputPhoto();
        photo.photo = new TdApi.InputFileLocal(path);
        photo.thumbnail = null;
        photo.video = null;
        photo.addedStickerFileIds = new int[0];
        photo.width = 0;
        photo.height = 0;
        TdApi.InputMessagePhoto content = new TdApi.InputMessagePhoto();
        content.photo = photo;
        content.caption = new TdApi.FormattedText(caption, null);
        sendMessage(chatId, content);
    }

    public void sendDocument(long chatId, String path, String caption) {
        TdApi.InputDocument document = new TdApi.InputDocument();
        document.document = new TdApi.InputFileLocal(path);
        document.thumbnail = null;
        document.disableContentTypeDetection = false;
        TdApi.InputMessageDocument content = new TdApi.InputMessageDocument();
        content.document = document;
        content.caption = new TdApi.FormattedText(caption, null);
        sendMessage(chatId, content);
    }

    public void sendVideo(long chatId, String path, String shape) {
        TdApi.InputVideo video = new TdApi.InputVideo();
        video.video = new TdApi.InputFileLocal(path);
        video.supportsStreaming = true;
        TdApi.InputMessageVideo content = new TdApi.InputMessageVideo();
        content.video = video;
        content.caption = new TdApi.FormattedText(encodeVideoShape(shape), null);
        sendMessage(chatId, content);
    }

    public void sendText(long chatId, String text, Long replyToMessageId) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) return;
        TdApi.InputMessageReplyTo reply = replyToMessageId == null ? null
                : new TdApi.InputMessageReplyToMessage(replyToMessageId, null, 0, null);
        TdApi.InputMessageText content = new TdApi.InputMessageText(
                new TdApi.FormattedText(value, null), null, true);
        requester.send(new TdApi.SendMessage(chatId, null, reply, null, null, content), nullCallback());
    }

    public void replyToStory(long chatId, int storyId, String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) return;
        TdApi.InputMessageReplyTo reply = new TdApi.InputMessageReplyToStory(chatId, storyId);
        TdApi.InputMessageText content = new TdApi.InputMessageText(
                new TdApi.FormattedText(value, null), null, true);
        requester.send(new TdApi.SendMessage(chatId, null, reply, null, null, content), nullCallback());
    }

    public void react(long chatId, long messageId, String emoji, boolean alreadyChosen) {
        TdApi.Function<?> function = alreadyChosen
                ? new TdApi.RemoveMessageReaction(chatId, messageId, new TdApi.ReactionTypeEmoji(emoji))
                : new TdApi.AddMessageReaction(chatId, messageId, new TdApi.ReactionTypeEmoji(emoji), false, true);
        requester.send(function, nullCallback());
    }

    public void forward(long fromChatId, long toChatId, long messageId) {
        requester.send(new TdApi.ForwardMessages(toChatId, null, fromChatId,
                new long[]{messageId}, null, false, false), nullCallback());
    }

    public void pin(long chatId, long messageId, boolean pinned) {
        requester.send(pinned
                ? new TdApi.PinChatMessage(chatId, messageId, true, false)
                : new TdApi.UnpinChatMessage(chatId, messageId), nullCallback());
    }

    public void delete(long chatId, long messageId, boolean revoke) {
        requester.send(new TdApi.DeleteMessages(chatId, new long[]{messageId}, revoke),
                new Callback() {
                    @Override public void onResult(TdApi.Object result) {
                        if (result instanceof TdApi.Ok) listener.onMessageDeleted(messageId);
                    }
                    @Override public void onError(TdApi.Error error) { }
                });
    }

    public void clearHistory(long chatId, boolean revoke) {
        requester.send(new TdApi.DeleteChatHistory(chatId, false, revoke), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (result instanceof TdApi.Ok) listener.onHistoryCleared(chatId);
            }
            @Override public void onError(TdApi.Error error) { }
        });
    }

    public void editText(long chatId, long messageId, String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) return;
        TdApi.InputMessageText content = new TdApi.InputMessageText(
                new TdApi.FormattedText(value, null), null, true);
        requester.send(new TdApi.EditMessageText(chatId, messageId, null, content), nullCallback());
    }

    private void sendMessage(long chatId, TdApi.InputMessageContent content) {
        requester.send(new TdApi.SendMessage(chatId, null, null, null, null, content), nullCallback());
    }

    private static Callback nullCallback() {
        return new Callback() {
            @Override public void onResult(TdApi.Object result) { }
            @Override public void onError(TdApi.Error error) { }
        };
    }

    public static String encodeVideoShape(String shape) {
        return NativeCore.encodeVideoShape(shape);
    }

    public static String decodeVideoShape(String caption) {
        return NativeCore.decodeVideoShape(caption);
    }
}
