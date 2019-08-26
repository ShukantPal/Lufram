package com.zexfer.lufram.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zexfer.lufram.database.models.DiscreteWallpaper

@Dao
interface DiscreteWallpaperDao {
    @Query("SELECT * FROM discrete_wallpaper")
    fun all(): LiveData<List<DiscreteWallpaper>>

    @Query("SELECT * FROM discrete_wallpaper WHERE id = (:id)")
    fun byId(id: Int): DiscreteWallpaper

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun put(wallpaper: DiscreteWallpaper): Long
}