package com.dikster.specialgram.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {ChatEntity.class}, version = 1, exportSchema = false)
public abstract class SpecialgramDatabase extends RoomDatabase {
    public abstract ChatDao chatDao();
}
