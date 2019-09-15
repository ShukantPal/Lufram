package com.zexfer.lufram

import android.app.AlarmManager
import android.app.AlarmManager.INTERVAL_DAY
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Context.WALLPAPER_SERVICE
import android.content.Intent
import android.util.Log
import com.zexfer.lufram.Lufram.Companion.EXTRA_CONFIG_PREFS
import com.zexfer.lufram.Lufram.Companion.EXTRA_UPDATER_ID
import com.zexfer.lufram.Lufram.Companion.LUFRAM_PREFS
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_RANDOMIZE_ORDER
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_TYPE
import com.zexfer.lufram.Lufram.Companion.PREF_UPDATER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_INDEX
import com.zexfer.lufram.LuframRepository.CONFIG_PERIODIC
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.database.tasks.WallpaperTask
import com.zexfer.lufram.expanders.Expander

class WallpaperUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val luframPrefs = context.getSharedPreferences(intent.getStringExtra(EXTRA_CONFIG_PREFS), 0)

        // The updaterId saved and for this broadcast must match.
        if (luframPrefs.getInt(PREF_UPDATER_ID, 0) !=
            intent.getIntExtra(EXTRA_UPDATER_ID, -1)
        ) {
            Log.e("Lufram", "Updater was still active after wallpaper was stopped!")
            (context.getSystemService(ALARM_SERVICE) as AlarmManager)
                .cancel(
                    PendingIntent.getBroadcast(
                        context, 0, intent, 0
                    )
                )

            return
        }

        val id = luframPrefs.getInt(PREF_WALLPAPER_ID, -1)
        if (id == -1) {// TODO: Fix illegal id
            Log.w("Lufram", "No id was set for preferred wallpaper")
        }

        UpdateWallpaperTask(context).execute(id)
    }

    @SuppressWarnings("StaticFieldLeak")
    class UpdateWallpaperTask(private var context: Context?) : WallpaperTask() {

        private var recurseCount = 0

        override fun onPostExecute(result: WallpaperCollection?) {
            if (result === null) {
                Log.w("Lufram", "WallpaperUpdateReceiver could not find target wallpaper")
                return
            }
            if (context == null) {
                Log.e("Lufram", "WallpaperUpdateReceiver has no context.")
                return
            }

            val expander = Expander.open(result)
            val luframPrefs = (context as Context).getSharedPreferences(LUFRAM_PREFS, 0)
            val updateMode = luframPrefs.getInt(PREF_CONFIG_TYPE, CONFIG_PERIODIC)
            val randomize = luframPrefs.getBoolean(PREF_CONFIG_RANDOMIZE_ORDER, false)
            val newOffset: Int =
                run {
                    if (updateMode == CONFIG_PERIODIC) {
                        if (!randomize)
                            (luframPrefs.getInt(PREF_WALLPAPER_INDEX, 0) + 1) % expander.size
                        else
                            (Math.random() * expander.size).toInt()
                    } else {
                        ((System.currentTimeMillis() % INTERVAL_DAY) /
                                (INTERVAL_DAY / expander.size)).toInt()
                    }
                }
            val wallpaperBmp = expander.load(context!!, newOffset, null).get()
            var rerunTask = false

            // Try loading and setting the current wallpaper
            if (wallpaperBmp !== null) {
                (context?.getSystemService(WALLPAPER_SERVICE) as WallpaperManager)
                    .setBitmap(wallpaperBmp)
            } else {
                Log.d("Lufram", "User deleted wallpaper!")
                expander.cut(newOffset)

                if (expander.size != 0) {
                    rerunTask = true
                    LuframRepository.putWallpaper(result)
                } else {
                    LuframRepository.deleteWallpaper(result.id!!)
                }
            }

            // If an error occurred, then start recursion. Here, we are
            // capping recursion at 25 calls.
            if (!rerunTask) {
                luframPrefs.edit().apply {
                    putInt(PREF_WALLPAPER_INDEX, newOffset)
                }.apply()
            } else if (recurseCount < 25) {// something is wrong if 25 nulls occur!
                ++recurseCount
                onPostExecute(result)
            } else {
                Log.d("Lufram", "recurseCount is 25! Cancelling updater, too bad!")
                LuframRepository.stopWallpaper()
            }

            context = null
        }
    }
}
