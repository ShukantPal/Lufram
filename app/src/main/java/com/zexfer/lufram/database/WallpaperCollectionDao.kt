package com.zexfer.lufram.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.zexfer.lufram.database.models.WallpaperCollection

@Dao
interface WallpaperCollectionDao {
    @Query("SELECT * FROM wallpapercollection")
    fun all(): LiveData<List<WallpaperCollection>>

    @Query("SELECT * FROM wallpapercollection WHERE id = (:id)")
    fun byId(id: Int): WallpaperCollection

    @Delete
    fun delete(wallpaper: WallpaperCollection)

    @Query("DELETE FROM wallpapercollection WHERE id = (:id)")
    fun deleteById(id: Int)

    @Query("DELETE FROM wallpapercollection WHERE id in (:ids)")
    fun deleteById(ids: Array<Int>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun put(wallpaper: WallpaperCollection): Long
}