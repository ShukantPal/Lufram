package com.zexfer.lufram.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.zexfer.lufram.database.models.DiscreteWallpaper

@Dao
interface DiscreteWallpaperDao {
    @Query("SELECT * FROM discrete_wallpaper")
    fun all(): LiveData<List<DiscreteWallpaper>>

    @Query("SELECT * FROM discrete_wallpaper WHERE id = (:id)")
    fun byId(id: Int): DiscreteWallpaper

    @Delete
    fun delete(wallpaper: DiscreteWallpaper)

    @Query("DELETE FROM discrete_wallpaper WHERE id = (:id)")
    fun deleteById(id: Int)

    @Query("DELETE FROM discrete_wallpaper WHERE id in (:ids)")
    fun deleteById(ids: Array<Int>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun put(wallpaper: DiscreteWallpaper): Long
}