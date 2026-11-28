package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Schedules incremental edits used by the animated-text message effect. */
public final class TdAnimatedMessageController implements AutoCloseable {
    public interface Requester {
        void send(TdApi.Function<?> function);
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

    public void schedule(long chatId, long messageId, List<String> characters) {
        if (characters == null || characters.size() < 2) return;
        for (int i = 1; i < characters.size(); i++) {
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j <= i; j++) builder.append(characters.get(j));
            final String value = builder.toString();
            scheduler.schedule(() -> {
                TdApi.InputMessageText content = new TdApi.InputMessageText(
                        new TdApi.FormattedText(value, null), null, true);
                requester.send(new TdApi.EditMessageText(chatId, messageId, null, content));
            }, i * STEP_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    @Override public void close() {
        scheduler.shutdownNow();
    }
}
