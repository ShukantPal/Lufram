package com.zexfer.lufram

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Context.WALLPAPER_SERVICE
import android.content.Intent
import android.util.Log
import com.zexfer.lufram.Lufram.Companion.EXTRA_UPDATER_ID
import com.zexfer.lufram.Lufram.Companion.LUFRAM_PREFS
import com.zexfer.lufram.Lufram.Companion.PREF_UPDATER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_INDEX
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.database.tasks.WallpaperTask
import com.zexfer.lufram.expanders.Expander

class WallpaperUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val luframPrefs = context.getSharedPreferences(LUFRAM_PREFS, 0)

        if (luframPrefs.getInt(PREF_UPDATER_ID, 0) !=
            intent.getIntExtra(EXTRA_UPDATER_ID, -1)
        ) {
            Log.d("Lufram", "WallpaperUpdater stopping!")
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
    class UpdateWallpaperTask(private var context: Context?) :
        WallpaperTask() {

        private var recurseCount = 0

        override fun onPostExecute(result: WallpaperCollection?) {
            if (result === null) {
                Log.w("Lufram", "User deleted wallpaper externally (probably); updater found no result!")
                return
            }
            if (context == null) {
                Log.d("Lufram", "Updater has no context")
                return
            }

            val expander = Expander.open(result)
            val luframPrefs = (context as Context).getSharedPreferences(LUFRAM_PREFS, 0)
            val newOffset: Int =
                run { luframPrefs.getInt(PREF_WALLPAPER_INDEX, 0) + 1 } % expander.size
            val wallpaperBmp = expander.load(context!!, newOffset, null).get()

            var rerunTask = false

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
