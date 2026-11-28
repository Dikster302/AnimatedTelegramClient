package com.dikster.specialgram.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {ChatEntity.class, MessageEntity.class}, version = 2, exportSchema = false)
public abstract class SpecialgramDatabase extends RoomDatabase {
    public abstract ChatDao chatDao();
}
