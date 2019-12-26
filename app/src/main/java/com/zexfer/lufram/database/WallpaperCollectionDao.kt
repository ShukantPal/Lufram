package com.zexfer.lufram.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.zexfer.lufram.database.models.WallpaperCollection

@Dao
interface WallpaperCollectionDao {
    @Query("SELECT * FROM wallpapercollection")
    fun all(): LiveData<List<WallpaperCollection>>

    @Query("SELECT * FROM wallpapercollection ORDER BY last_updater_id DESC")
    fun allSorted(): LiveData<List<WallpaperCollection>>

    @Query("SELECT * FROM wallpapercollection WHERE rowId = (:id)")
    fun byId(id: Int): WallpaperCollection

    @Delete
    fun delete(wallpaper: WallpaperCollection)

    @Query("DELETE FROM wallpapercollection WHERE rowId = (:id)")
    fun deleteById(id: Int)

    @Query("DELETE FROM wallpapercollection WHERE rowId in (:ids)")
    fun deleteById(ids: Array<Int>)

    @Query("SELECT * FROM wallpapercollection WHERE label LIKE (:str) OR sources LIKE (:str)")
    suspend fun search(str: String): Array<WallpaperCollection>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun put(wallpaper: WallpaperCollection): Long
}