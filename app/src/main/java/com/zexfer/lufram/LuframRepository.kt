package com.zexfer.lufram

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.LifecycleObserver
import com.zexfer.lufram.Lufram.Companion.EXTRA_UPDATER_ID
import com.zexfer.lufram.Lufram.Companion.LUFRAM_PREFS
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_DAY_RANGE
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_INTERVAL_MILLIS
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_RANDOMIZE_ORDER
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_TIMEZONE_ADJUSTED_ENABLED
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_TYPE
import com.zexfer.lufram.Lufram.Companion.PREF_UPDATER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WAS_STOPPED
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.database.tasks.DeleteWallpaperByIdTask
import com.zexfer.lufram.database.tasks.DeleteWallpaperTask
import com.zexfer.lufram.database.tasks.PutWallpaperTask
import com.zexfer.lufram.database.tasks.UpdateWallpaperTask

object LuframRepository : LifecycleObserver {

    val luframPrefs: SharedPreferences = Lufram.instance.getSharedPreferences(LUFRAM_PREFS, 0)

    fun preferredWallpaperId() =
        luframPrefs.getInt(PREF_WALLPAPER_ID, -1)

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

    fun commitConfig(config: PeriodicConfig) {
        val isModeDiff = luframPrefs.getInt(PREF_CONFIG_TYPE, CONFIG_PERIODIC) == CONFIG_PERIODIC
        val wasIntervalChanged = (config.intervalMillis != luframPrefs.getLong(
            PREF_CONFIG_INTERVAL_MILLIS, 60000
        ))

        luframPrefs.edit().apply {
            putInt(PREF_CONFIG_TYPE, CONFIG_PERIODIC)
            putLong(PREF_CONFIG_INTERVAL_MILLIS, config.intervalMillis)
            putBoolean(PREF_CONFIG_RANDOMIZE_ORDER, config.randomizeOrder)
        }.apply()

        if (isModeDiff || wasIntervalChanged)
            restartWallpaper() // changing randomize order doesn't need a restart!
    }

    fun commitConfig(config: DynamicConfig) {
        luframPrefs.edit().apply {
            putInt(PREF_CONFIG_TYPE, CONFIG_DYNAMIC)
            putInt(PREF_CONFIG_DAY_RANGE, config.dayRange)
            putBoolean(PREF_CONFIG_TIMEZONE_ADJUSTED_ENABLED, config.timeZoneAdjustEnabled)
        }.apply()
    }

    /**
     * Applies the (discrete) wallpaper and activates alarms for switching
     * through each input.
     */
    fun applyWallpaper(wallpaperId: Int) {
        val oldUpdaterId = luframPrefs.getInt(PREF_UPDATER_ID, 0)
        val intervalMillis =
            luframPrefs.getLong(PREF_CONFIG_INTERVAL_MILLIS, 60000) // wallpaper.interval

        stopWallpaper(false)

        luframPrefs.edit().apply {
            putInt(PREF_WALLPAPER_ID, wallpaperId)
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

        UpdateWallpaperTask { it ->
            it.lastUpdaterId = oldUpdaterId + 1
        }.execute(wallpaperId)
    }

    fun applyWallpaper(wallpaper: WallpaperCollection) {
        applyWallpaper(wallpaper.id!!)
    }

    /**
     * Prevents all future wallpaper updates
     *
     * @param writeUpdaterId - whether to update the updaterId in shared
     *      preferences. If false, it is expected you will do that on
     *      your own.
     */
    fun stopWallpaper(writeUpdaterId: Boolean = true): Int {
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

        return oldUpdaterId
    }

    fun restartWallpaper() {
        if (preferredWallpaperId() != -1)
            applyWallpaper(preferredWallpaperId()) // re-writes the alarms!
    }

    fun safeStopWallpaper(updaterId: Int, writeUpdaterId: Boolean = true) {
        if (preferredWallpaperId() == updaterId)
            stopWallpaper(writeUpdaterId)
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

    interface Config

    data class PeriodicConfig(
        var intervalMillis: Long,
        var randomizeOrder: Boolean
    ) : Config

    data class DynamicConfig(
        var dayRange: Int,
        var timeZoneAdjustEnabled: Boolean
    ) : Config

    val CONFIG_PERIODIC = 0xff1
    val CONFIG_DYNAMIC = 0xff2
}