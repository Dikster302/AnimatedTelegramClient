package com.dikster.specialgram.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatDao {
    @Query("SELECT * FROM cached_chats ORDER BY pinnedOrder DESC, lastDate DESC")
    List<ChatEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ChatEntity> chats);

    @Query("DELETE FROM cached_chats")
    void deleteAll();

    @Query("SELECT * FROM cached_messages WHERE chatId = :chatId ORDER BY date ASC, messageId ASC")
    List<MessageEntity> getMessages(long chatId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertMessages(List<MessageEntity> messages);

    @Query("DELETE FROM cached_messages WHERE chatId = :chatId AND messageId IN (:messageIds)")
    void deleteMessages(long chatId, List<Long> messageIds);

    @Query("DELETE FROM cached_messages WHERE chatId = :chatId")
    void clearMessages(long chatId);

    /** Ограничивает историю чата последними сообщениями, не затрагивая другие чаты. */
    @Query("DELETE FROM cached_messages WHERE chatId = :chatId AND messageId NOT IN "
            + "(SELECT messageId FROM cached_messages WHERE chatId = :chatId "
            + "ORDER BY date DESC, messageId DESC LIMIT :maxCount)")
    void pruneMessagesForChat(long chatId, int maxCount);

    /** Общий предохранитель от бесконтрольного роста локальной базы. */
    @Query("SELECT COUNT(*) FROM cached_messages")
    int getCachedMessageCount();

    @Query("DELETE FROM cached_messages WHERE rowid NOT IN "
            + "(SELECT rowid FROM cached_messages ORDER BY date DESC, messageId DESC LIMIT :maxCount)")
    void pruneMessagesGlobally(int maxCount);
}
