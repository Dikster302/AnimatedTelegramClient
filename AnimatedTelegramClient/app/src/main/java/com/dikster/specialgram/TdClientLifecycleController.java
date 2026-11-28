package com.dikster.specialgram;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

/** Performs ordered shutdown of Java controllers and the underlying TDLib client. */
public final class TdClientLifecycleController {
    private final Client client;
    private final Runnable[] shutdownActions;
    private boolean closed;

    public TdClientLifecycleController(Client client, Runnable... shutdownActions) {
        this.client = client;
        this.shutdownActions = shutdownActions == null ? new Runnable[0] : shutdownActions.clone();
    }

    public synchronized void close() {
        if (closed) return;
        closed = true;
        for (Runnable action : shutdownActions) {
            if (action == null) continue;
            try { action.run(); } catch (RuntimeException ignored) { }
        }
        if (client != null) client.send(new TdApi.Close(), null);
    }
}
