package com.zexfer.lufram

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlarmManager.INTERVAL_DAY
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import com.zexfer.lufram.Lufram.Companion.LUFRAM_PREFS
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_INTERVAL_MILLIS
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_TYPE
import com.zexfer.lufram.Lufram.Companion.PREF_UPDATER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_INDEX
import com.zexfer.lufram.Lufram.Companion.PREF_WAS_STOPPED
import com.zexfer.lufram.LuframRepository.CONFIG_DYNAMIC
import com.zexfer.lufram.LuframRepository.CONFIG_PERIODIC
import com.zexfer.lufram.LuframRepository.luframPrefs
import com.zexfer.lufram.LuframRepository.preferredWallpaperId
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.database.tasks.UpdateWallpaperTask
import com.zexfer.lufram.database.tasks.WallpaperTask
import com.zexfer.lufram.expanders.Expander
import java.util.concurrent.Future
import java.util.concurrent.FutureTask

object WallpaperUpdateController {

    var targetId: Int = -2
        get() {
            if (field == -2) {
                field = preferredWallpaperId()
            }

            return field
        }
        set(value) {
            field = value
            val oldUpdaterId = luframPrefs.getInt(PREF_UPDATER_ID, -1)
            val updaterId = oldUpdaterId + 1
            val updateMode = luframPrefs.getInt(PREF_CONFIG_TYPE, CONFIG_PERIODIC)
            val updateIntent =
                Intent(Lufram.instance, WallpaperUpdateReceiver::class.java).apply {
                    putExtra(Lufram.EXTRA_UPDATER_ID, updaterId)
                    putExtra(Lufram.EXTRA_CONFIG_PREFS, LUFRAM_PREFS)
                }
            val updatePendingIntent =
                PendingIntent.getBroadcast(
                    Lufram.instance,
                    updaterId,
                    updateIntent,
                    0
                )

            // Stop previous wallpaper-updater "now" via AlarmManager
            LuframRepository.stopWallpaper(false)

            // Save updater-config to preferences
            luframPrefs.edit().apply {
                putInt(PREF_WALLPAPER_INDEX, 0)
                putInt(PREF_WALLPAPER_ID, value)
                putInt(PREF_UPDATER_ID, oldUpdaterId + 1) // stop any previous updaters!
                putBoolean(PREF_WAS_STOPPED, false)
            }.apply()

            val alarmManager =
                (Lufram.instance.getSystemService(Context.ALARM_SERVICE) as AlarmManager)

            // Start alarms for WallpaperUpdateReceiver
            when (updateMode) {
                CONFIG_PERIODIC -> {
                    alarmManager.setInexactRepeating(
                        AlarmManager.ELAPSED_REALTIME,
                        50,
                        luframPrefs.getLong(PREF_CONFIG_INTERVAL_MILLIS, 3600000),
                        updatePendingIntent
                    )
                }
                CONFIG_DYNAMIC -> {
                    WallpaperTask.run(value) {
                        val count = Expander.open(it).size
                        val updateInterval = INTERVAL_DAY / count

                        val time = System.currentTimeMillis() % INTERVAL_DAY
                        val lastUpdateTime = time - time % updateInterval
                        val updateTime = lastUpdateTime + updateInterval

                        alarmManager.setInexactRepeating(
                            AlarmManager.RTC,
                            updateTime - time,
                            luframPrefs.getLong(PREF_CONFIG_INTERVAL_MILLIS, 3600000),
                            updatePendingIntent
                        )

                        // Update the wallpaper now too, only if the next update too
                        // long for the user to wait
                        if (updateTime - time >= 60000) {
                            WallpaperUpdateReceiver.UpdateWallpaperTask(Lufram.instance)
                                .execute(value)
                        }
                    }
                }
            }

            // Update updaterId history for the wallpaper collection
            UpdateWallpaperTask { it.lastUpdaterId = updaterId }.execute(value)
        }

    fun setTargetIdAsync(value: Int) {
        SetTargetIdTask().execute(value)
    }

    fun estimatedWallpaperIndex(): Int =
        luframPrefs.getInt(PREF_WALLPAPER_INDEX, 0)

    fun estimatedWallpaperAsync(): Future<Bitmap> =
        FutureTask<Bitmap> {
            val wallpaperCollection =
                LuframDatabase.instance
                    .wcDao()
                    .byId(preferredWallpaperId())

            val expander = Expander.open(wallpaperCollection)
            val activeIndex = estimatedWallpaperIndex()

            expander.load(Lufram.instance, activeIndex).get()
        }.also {
            Lufram.instance
                .wallpaperTaskExecutor
                .execute(it)
        }

    @SuppressLint("StaticFieldLeak")
    fun estimatedWallpaperAsync(callback: (Bitmap?) -> Unit) {
        object : WallpaperTask() {
            override fun onPostExecute(result: WallpaperCollection?) {
                if (result == null) {
                    return
                }

                val expander = Expander.open(result)
                val activeIndex: Int = luframPrefs.getInt(PREF_WALLPAPER_INDEX, 0)

                expander.load(Lufram.instance, activeIndex, callback)
            }
        }.execute(preferredWallpaperId())
    }

    class SetTargetIdTask : AsyncTask<Int, Void, Void>() {
        override fun doInBackground(vararg idArg: Int?): Void? {
            targetId = idArg[0]!!
            return null
        }
    }
}