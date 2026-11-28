package com.dikster.specialgram.data;

import android.content.Context;

import androidx.room.Room;

import com.dikster.specialgram.TgChat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Disk cache for the chat list. All database work is serialized off the UI/TDLib
 * callback threads. Writes are coalesced so a burst of TDLib updates persists
 * only the newest complete snapshot.
 */
public final class ChatCacheRepository implements AutoCloseable {
    public interface LoadCallback {
        void onLoaded(List<TgChat> chats);
    }

    private final SpecialgramDatabase database;
    private final ExecutorService io = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "specialgram-chat-cache");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicReference<List<ChatEntity>> pendingSnapshot = new AtomicReference<>();
    private final AtomicBoolean writeScheduled = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ChatCacheRepository(Context context) {
        database = Room.databaseBuilder(context.getApplicationContext(),
                SpecialgramDatabase.class, "specialgram-cache.db").build();
    }

    public void load(LoadCallback callback) {
        if (callback == null || closed.get()) return;
        io.execute(() -> {
            if (closed.get()) return;
            List<ChatEntity> rows = database.chatDao().getAll();
            List<TgChat> result = new ArrayList<>(rows.size());
            for (ChatEntity row : rows) result.add(row.toModel());
            callback.onLoaded(Collections.unmodifiableList(result));
        });
    }

    /** Replaces the snapshot atomically; rapid updates collapse to the newest one. */
    public void replaceAll(List<TgChat> chats) {
        if (closed.get()) return;
        List<ChatEntity> snapshot = new ArrayList<>();
        if (chats != null) for (TgChat chat : chats)
            if (chat != null) snapshot.add(ChatEntity.from(chat));
        pendingSnapshot.set(Collections.unmodifiableList(snapshot));
        scheduleWrite();
    }

    private void scheduleWrite() {
        if (closed.get() || !writeScheduled.compareAndSet(false, true)) return;
        try {
            io.execute(() -> {
                while (!closed.get()) {
                    List<ChatEntity> snapshot = pendingSnapshot.getAndSet(null);
                    if (snapshot != null) {
                        database.runInTransaction(() -> {
                            database.chatDao().deleteAll();
                            if (!snapshot.isEmpty()) database.chatDao().insertAll(snapshot);
                        });
                    }
                    writeScheduled.set(false);
                    if (pendingSnapshot.get() == null
                            || !writeScheduled.compareAndSet(false, true)) return;
                }
                writeScheduled.set(false);
            });
        } catch (RuntimeException rejected) {
            writeScheduled.set(false);
        }
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        pendingSnapshot.set(null);
        try {
            io.execute(database::close);
        } catch (RuntimeException ignored) {
            database.close();
        }
        io.shutdown();
    }
}
