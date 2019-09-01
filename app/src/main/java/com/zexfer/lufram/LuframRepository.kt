package com.zexfer.lufram

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.zexfer.lufram.Lufram.Companion.EXTRA_UPDATER_ID
import com.zexfer.lufram.Lufram.Companion.LUFRAM_PREFS
import com.zexfer.lufram.Lufram.Companion.PREF_UPDATER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_SUBTYPE
import com.zexfer.lufram.Lufram.Companion.PREF_WAS_STOPPED
import com.zexfer.lufram.Lufram.Companion.WALLPAPER_DISCRETE
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.database.tasks.DeleteWallpaperByIdTask
import com.zexfer.lufram.database.tasks.DeleteWallpaperTask
import com.zexfer.lufram.database.tasks.PutWallpaperTask
import com.zexfer.lufram.database.tasks.WallpaperTask

object LuframRepository {

    val luframPrefs: SharedPreferences = Lufram.instance.getSharedPreferences(LUFRAM_PREFS, 0)

    fun preferredWallpaperId() =
        luframPrefs.getInt(PREF_WALLPAPER_ID, -1)

    fun preferredWallpaperSubtype() =
        luframPrefs.getString(PREF_WALLPAPER_SUBTYPE, "null")

    fun deleteWallpaper(wallpaper: WallpaperCollection) {
        if (preferredWallpaperId() == wallpaper.id)
            stopWallpaper()
        DeleteWallpaperTask().execute(wallpaper)
    }

    fun deleteWallpaper(id: Int) {
        if (preferredWallpaperId() == id)
            stopWallpaper()
        DeleteWallpaperByIdTask().execute(id)
    }

    fun putWallpaper(wallpaper: WallpaperCollection) {
        PutWallpaperTask().execute(wallpaper)
    }

    /**
     * Applies the (discrete) wallpaper and activates alarms for switching
     * through each input.
     */
    fun applyWallpaper(wallpaper: WallpaperCollection) {
        val oldUpdaterId = luframPrefs.getInt(PREF_UPDATER_ID, 0)
        val intervalMillis = 60000.toLong() // wallpaper.interval

        Log.d("Lufram", "Interval=${intervalMillis}")
        stopWallpaper(false)

        luframPrefs.edit().apply {
            putInt(
                PREF_WALLPAPER_ID,
                wallpaper.id ?: throw IllegalStateException("Cannot apply a wallpaper with invalid id!")
            )
            putString(PREF_WALLPAPER_SUBTYPE, WALLPAPER_DISCRETE)
            putInt(PREF_UPDATER_ID, oldUpdaterId + 1) // stop any previous updaters!
            putBoolean(PREF_WAS_STOPPED, false)
        }.apply()

        (Lufram.instance.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                50,
                intervalMillis,
                PendingIntent.getBroadcast(
                    Lufram.instance,
                    oldUpdaterId + 1,
                    Intent(
                        Lufram.instance,
                        WallpaperUpdateReceiver::class.java
                    ).apply {
                        putExtra(EXTRA_UPDATER_ID, oldUpdaterId + 1)
                    },
                    0
                )
            )
    }

    /**
     * Prevents all future wallpaper updates
     *
     * @param writeUpdaterId - whether to update the updaterId in shared
     *      preferences. If false, it is expected you will do that on
     *      your own.
     */
    fun stopWallpaper(writeUpdaterId: Boolean = true) {
        val oldUpdaterId = luframPrefs.getInt(PREF_UPDATER_ID, 0)

        if (writeUpdaterId) {
            luframPrefs.edit().apply {
                putInt(PREF_UPDATER_ID, oldUpdaterId + 1)
                putInt(PREF_WALLPAPER_ID, -1)
                putBoolean(PREF_WAS_STOPPED, true)
            }.apply()
        }

        // Also prevent updater beforehand!
        (Lufram.instance.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .cancel(
                PendingIntent.getBroadcast(
                    Lufram.instance,
                    oldUpdaterId,
                    Intent(
                        Lufram.instance,
                        WallpaperUpdateReceiver::class.java
                    ),
                    0
                )
            )
    }

    /**
     * Checks if the currently preferred (if it exists) is being updated
     * or if the updater is dead.
     *
     * @return whether wallpaper updates are still running. If wallpaper,
     *  was stopped, it returns true (wallpaper doesn't exist)
     */
    fun isUpdaterAlive() =
        luframPrefs.getBoolean(PREF_WAS_STOPPED, true) ||
                PendingIntent.getBroadcast(
                    Lufram.instance,
                    luframPrefs.getInt(PREF_UPDATER_ID, 0),
                    Intent(
                        Lufram.instance,
                        WallpaperUpdateReceiver::class.java
                    ),
                    PendingIntent.FLAG_NO_CREATE
                ) != null

    fun applyWallpaper(id: Int) {
        ApplyWallpaperTask().execute(id)
    }

    class ApplyWallpaperTask : WallpaperTask() {
        override fun onPostExecute(result: WallpaperCollection?) {
            if (result !== null)
                LuframRepository.applyWallpaper(result)
            else {
                Log.e("Lufram", "Couldn't apply wallpaper; none found")
            }
        }
    }
}