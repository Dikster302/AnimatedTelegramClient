package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Schedules incremental edits used by the animated-text message effect. */
public final class TdAnimatedMessageController implements AutoCloseable {
    public interface ResultCallback { void onResult(TdApi.Object result); }
    public interface Requester {
        void send(TdApi.Function<?> function, ResultCallback callback);
    }
    public interface SendListener {
        void onSent(TdApi.Message message, List<String> characters);
    }

    private static final long STEP_DELAY_MS = 180L;
    private final Requester requester;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "specialgram-animated-message");
        thread.setDaemon(true);
        return thread;
    });

    public TdAnimatedMessageController(Requester requester) {
        this.requester = requester;
    }

    public void sendAnimated(long chatId, String text, Long replyToMessageId, SendListener listener) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) return;
        int[] codePoints = value.codePoints().toArray();
        java.util.ArrayList<String> characters = new java.util.ArrayList<>(codePoints.length);
        for (int codePoint : codePoints) characters.add(new String(Character.toChars(codePoint)));
        TdApi.InputMessageReplyTo reply = replyToMessageId == null ? null
                : new TdApi.InputMessageReplyToMessage(replyToMessageId, null, 0, null);
        TdApi.InputMessageText content = new TdApi.InputMessageText(
                new TdApi.FormattedText(characters.get(0), null), null, true);
        requester.send(new TdApi.SendMessage(chatId, null, reply, null, null, content),
                result -> {
                    if (result instanceof TdApi.Message && listener != null)
                        listener.onSent((TdApi.Message) result, characters);
                });
    }

    public void schedule(long chatId, long messageId, List<String> characters) {
        if (characters == null || characters.size() < 2) return;
        for (int i = 1; i < characters.size(); i++) {
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j <= i; j++) builder.append(characters.get(j));
            final String value = builder.toString();
            scheduler.schedule(() -> {
                TdApi.InputMessageText content = new TdApi.InputMessageText(
                        new TdApi.FormattedText(value, null), null, true);
                requester.send(new TdApi.EditMessageText(chatId, messageId, null, content), null);
            }, i * STEP_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    @Override public void close() {
        scheduler.shutdownNow();
    }
}
