package com.dikster.specialgram.data;

import android.content.Context;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.dikster.specialgram.TgChat;
import com.dikster.specialgram.TgMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Кэш чатов и истории сообщений. Все операции БД выполняются в одном фоновом потоке. */
public final class ChatCacheRepository implements AutoCloseable {
    public interface LoadCallback { void onLoaded(List<TgChat> chats); }
    public interface MessagesCallback { void onLoaded(List<TgMessage> messages); }

    // Кэшируем разумный хвост истории каждого чата и ограничиваем общий объём базы.
    private static final int MAX_MESSAGES_PER_CHAT = 500;
    private static final int MAX_MESSAGES_TOTAL = 20000;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS cached_messages ("
                    + "chatId INTEGER NOT NULL, messageId INTEGER NOT NULL, text TEXT, "
                    + "outgoing INTEGER NOT NULL, date INTEGER NOT NULL, read INTEGER NOT NULL, "
                    + "videoShape TEXT, videoPath TEXT, replyToMessageId INTEGER NOT NULL, "
                    + "replyToText TEXT, reactionsJson TEXT, photoPath TEXT, mediaKind TEXT, "
                    + "mediaPath TEXT, mediaName TEXT, mediaMimeType TEXT, mediaDuration INTEGER NOT NULL, "
                    + "mediaSize INTEGER NOT NULL, pinned INTEGER NOT NULL, "
                    + "PRIMARY KEY(chatId, messageId))");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_cached_messages_chatId_date "
                    + "ON cached_messages (chatId, date)");
        }
    };

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
                SpecialgramDatabase.class, "specialgram-cache.db")
                .addMigrations(MIGRATION_1_2)
                .build();
    }

    public void load(LoadCallback callback) {
        if (callback == null || closed.get()) return;
        execute(() -> {
            List<ChatEntity> rows = database.chatDao().getAll();
            List<TgChat> result = new ArrayList<>(rows.size());
            for (ChatEntity row : rows) result.add(row.toModel());
            callback.onLoaded(Collections.unmodifiableList(result));
        });
    }

    public void loadMessages(long chatId, MessagesCallback callback) {
        if (callback == null || closed.get()) return;
        execute(() -> {
            List<MessageEntity> rows = database.chatDao().getMessages(chatId);
            List<TgMessage> result = new ArrayList<>(rows.size());
            for (MessageEntity row : rows) result.add(row.toModel());
            callback.onLoaded(Collections.unmodifiableList(result));
        });
    }

    /** Сохраняет/обновляет только переданные сообщения, не затрагивая старые страницы. */
    public void upsertMessages(long chatId, List<TgMessage> messages) {
        if (closed.get() || messages == null || messages.isEmpty()) return;
        List<MessageEntity> rows = new ArrayList<>(messages.size());
        for (TgMessage message : messages)
            if (message != null) rows.add(MessageEntity.from(chatId, message));
        if (rows.isEmpty()) return;
        execute(() -> {
            database.chatDao().upsertMessages(rows);
            pruneMessageCache(chatId);
        });
    }

    public void deleteMessages(long chatId, List<Long> messageIds) {
        if (closed.get() || messageIds == null || messageIds.isEmpty()) return;
        List<Long> ids = new ArrayList<>(messageIds);
        execute(() -> database.chatDao().deleteMessages(chatId, ids));
    }

    /** Атомарно заменяет временный ID отправки на подтверждённый TDLib ID. */
    public void replaceMessageId(long chatId, long oldMessageId, TgMessage confirmedMessage) {
        if (closed.get() || confirmedMessage == null) return;
        MessageEntity row = MessageEntity.from(chatId, confirmedMessage);
        execute(() -> database.runInTransaction(() -> {
            database.chatDao().deleteMessages(chatId, Collections.singletonList(oldMessageId));
            database.chatDao().upsertMessages(Collections.singletonList(row));
            pruneMessageCache(chatId);
        }));
    }

    private void pruneMessageCache(long chatId) {
        database.chatDao().pruneMessagesForChat(chatId, MAX_MESSAGES_PER_CHAT);
        // Запускаем глобальную очистку только при заметном превышении лимита.
        if (database.chatDao().getCachedMessageCount() > MAX_MESSAGES_TOTAL + 1000)
            database.chatDao().pruneMessagesGlobally(MAX_MESSAGES_TOTAL);
    }

    public void clearMessages(long chatId) {
        if (closed.get()) return;
        execute(() -> database.chatDao().clearMessages(chatId));
    }

    /** Заменяет полный снимок списка чатов; быстрые обновления объединяются. */
    public void replaceAll(List<TgChat> chats) {
        if (closed.get()) return;
        List<ChatEntity> snapshot = new ArrayList<>();
        if (chats != null) for (TgChat chat : chats)
            if (chat != null) snapshot.add(ChatEntity.from(chat));
        pendingSnapshot.set(Collections.unmodifiableList(snapshot));
        scheduleWrite();
    }

    private void execute(Runnable task) {
        if (closed.get()) return;
        try { io.execute(() -> { if (!closed.get()) task.run(); }); }
        catch (RuntimeException ignored) { }
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
        try { io.execute(database::close); }
        catch (RuntimeException ignored) { database.close(); }
        io.shutdown();
    }
}
