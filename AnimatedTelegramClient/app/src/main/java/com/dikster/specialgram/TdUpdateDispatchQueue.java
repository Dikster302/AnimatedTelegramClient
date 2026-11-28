package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayDeque;

/**
 * Buffers TDLib updates until all state routers are initialized, then dispatches
 * updates in arrival order. All queue transitions are serialized on one lock.
 */
public final class TdUpdateDispatchQueue {
    public interface Listener {
        void onUpdate(TdApi.Object update);
    }

    private final Object lock = new Object();
    private final ArrayDeque<TdApi.Object> pending = new ArrayDeque<>();
    private final Listener listener;
    private boolean ready;

    public TdUpdateDispatchQueue(Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener == null");
        this.listener = listener;
    }

    public void accept(TdApi.Object update) {
        if (update == null) return;
        synchronized (lock) {
            if (!ready) {
                pending.addLast(update);
                return;
            }
            listener.onUpdate(update);
        }
    }

    /** Marks the update graph ready and drains all updates received during setup. */
    public void markReady() {
        synchronized (lock) {
            if (ready) return;
            ready = true;
            while (!pending.isEmpty()) listener.onUpdate(pending.removeFirst());
        }
    }
}
