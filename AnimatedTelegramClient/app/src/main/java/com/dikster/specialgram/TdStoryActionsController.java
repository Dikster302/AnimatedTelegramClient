package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** TDLib story publishing and interaction requests. */
public final class TdStoryActionsController {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface Requester {
        void send(TdApi.Function<?> function, Callback callback);
    }
    public interface Listener {
        void onStoryPosted(TdApi.Story story);
        void onValidationError(String message);
    }

    private final Requester requester;
    private final Listener listener;

    public TdStoryActionsController(Requester requester, Listener listener) {
        this.requester = requester;
        this.listener = listener;
    }

    public void open(long chatId, int storyId) {
        requester.send(new TdApi.OpenStory(chatId, storyId), emptyCallback());
    }

    public void close(long chatId, int storyId) {
        requester.send(new TdApi.CloseStory(chatId, storyId), emptyCallback());
    }

    public void react(long chatId, int storyId, String emoji) {
        requester.send(new TdApi.SetStoryReaction(chatId, storyId,
                new TdApi.ReactionTypeEmoji(emoji), true), emptyCallback());
    }

    public void postPhoto(long userId, String path, String caption) {
        TdApi.InputStoryContentPhoto content =
                new TdApi.InputStoryContentPhoto(new TdApi.InputFileLocal(path), new int[0]);
        TdApi.StoryPrivacySettings privacy = new TdApi.StoryPrivacySettingsContacts(new long[0]);
        String value = caption == null ? "" : caption.trim();
        TdApi.FormattedText text = value.isEmpty() ? null : new TdApi.FormattedText(value, null);
        post(userId, content, text);
    }

    public void postVideo(long userId, String path, double duration) {
        if (duration <= 0.0 || duration > 60.0) {
            listener.onValidationError("Длительность видеоистории должна быть от 1 до 60 секунд");
            return;
        }
        TdApi.InputStoryContentVideo content =
                new TdApi.InputStoryContentVideo(new TdApi.InputFileLocal(path),
                        new int[0], duration, 0.0, false);
        post(userId, content, null);
    }

    private void post(long userId, TdApi.InputStoryContent content, TdApi.FormattedText caption) {
        TdApi.StoryPrivacySettings privacy = new TdApi.StoryPrivacySettingsContacts(new long[0]);
        requester.send(new TdApi.PostStory(userId, content, null, caption, null,
                privacy, new int[0], 86400, null, false, false), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (result instanceof TdApi.Story) listener.onStoryPosted((TdApi.Story) result);
            }
            @Override public void onError(TdApi.Error error) { }
        });
    }

    private static Callback emptyCallback() {
        return new Callback() {
            @Override public void onResult(TdApi.Object result) { }
            @Override public void onError(TdApi.Error error) { }
        };
    }
}
