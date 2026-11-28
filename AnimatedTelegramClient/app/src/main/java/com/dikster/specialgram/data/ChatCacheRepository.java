package com.dikster.specialgram.data;

import android.content.Context;

import androidx.room.Room;

import com.dikster.specialgram.TgChat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Disk cache for the chat list. All database work is serialized off the UI/TDLib
 * callback threads. A cached list is only a fast first paint; TDLib refreshes it.
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

    public ChatCacheRepository(Context context) {
        database = Room.databaseBuilder(context.getApplicationContext(),
                SpecialgramDatabase.class, "specialgram-cache.db").build();
    }

    public void load(LoadCallback callback) {
        if (callback == null) return;
        io.execute(() -> {
            List<ChatEntity> rows = database.chatDao().getAll();
            List<TgChat> result = new ArrayList<>(rows.size());
            for (ChatEntity row : rows) result.add(row.toModel());
            callback.onLoaded(Collections.unmodifiableList(result));
        });
    }

    /** Replaces the snapshot atomically so deleted chats do not remain in cache. */
    public void replaceAll(List<TgChat> chats) {
        final List<ChatEntity> snapshot = new ArrayList<>();
        if (chats != null) for (TgChat chat : chats)
            if (chat != null) snapshot.add(ChatEntity.from(chat));
        io.execute(() -> database.runInTransaction(() -> {
            database.chatDao().deleteAll();
            if (!snapshot.isEmpty()) database.chatDao().insertAll(snapshot);
        }));
    }

    @Override public void close() {
        io.shutdown();
        database.close();
    }
}
