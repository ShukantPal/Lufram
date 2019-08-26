package com.zexfer.lufram.database

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zexfer.lufram.Lufram
import com.zexfer.lufram.Lufram.Companion.LUFRAM_DB
import com.zexfer.lufram.database.models.DiscreteWallpaper
import com.zexfer.lufram.database.models.UriArrayConverter
import com.zexfer.lufram.database.models.Wallpaper

@TypeConverters(UriArrayConverter::class)
@Database(entities = [DiscreteWallpaper::class], version = 1)
abstract class LuframDatabase : RoomDatabase() {
    abstract fun discreteWallpaperDao(): DiscreteWallpaperDao

    fun allDiscreteWallpapers(): LiveData<List<DiscreteWallpaper>> =
        discreteWallpaperDao().all()

    companion object {
        val instance: LuframDatabase by lazy {
            Room.databaseBuilder(Lufram.context, LuframDatabase::class.java, LUFRAM_DB)
                .build()
        }
    }

    abstract class DiscreteWallpaperTask : AsyncTask<Int, Void, DiscreteWallpaper>() {
        override fun doInBackground(vararg id: Int?): DiscreteWallpaper {
            return LuframDatabase.instance.discreteWallpaperDao().byId(
                id[0] ?: throw IllegalArgumentException("DiscreteWallpaperTask requires a valid id")
            )
        }
    }

    class PutWallpaperTask : AsyncTask<Wallpaper, Void, Void>() {
        override fun doInBackground(vararg wps: Wallpaper?): Void? {
            for (wp in wps) {
                if (wp is DiscreteWallpaper) {
                    val rowId = instance.discreteWallpaperDao().put(wp)

                    if (wp.id == null)
                        wp.id = rowId.toInt()// TODO: Solve rowId problem
                }
            }

            return null
        }
    }
}