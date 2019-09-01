package com.zexfer.lufram.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zexfer.lufram.Lufram
import com.zexfer.lufram.Lufram.Companion.LUFRAM_DB
import com.zexfer.lufram.database.models.UriArrayConverter
import com.zexfer.lufram.database.models.WallpaperCollection

@TypeConverters(UriArrayConverter::class)
@Database(entities = [WallpaperCollection::class], version = 1)
abstract class LuframDatabase : RoomDatabase() {
    abstract fun wcDao(): WallpaperCollectionDao

    companion object {
        val instance: LuframDatabase by lazy {
            Room.databaseBuilder(Lufram.context, LuframDatabase::class.java, LUFRAM_DB)
                .build()
        }
    }
}