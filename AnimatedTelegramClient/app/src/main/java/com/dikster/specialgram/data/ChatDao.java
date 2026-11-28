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
}
