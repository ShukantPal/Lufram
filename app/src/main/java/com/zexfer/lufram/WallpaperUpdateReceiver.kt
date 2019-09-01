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
import com.koushikdutta.ion.Ion
import com.zexfer.lufram.Lufram.Companion.EXTRA_UPDATER_ID
import com.zexfer.lufram.Lufram.Companion.LUFRAM_PREFS
import com.zexfer.lufram.Lufram.Companion.PREF_UPDATER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_STATE
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_SUBTYPE
import com.zexfer.lufram.Lufram.Companion.WALLPAPER_DISCRETE
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.database.tasks.WallpaperTask

class WallpaperUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val luframPrefs = context.getSharedPreferences(LUFRAM_PREFS, 0)

        // Check if we are supposed to stop!
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

        when (luframPrefs.getString(PREF_WALLPAPER_SUBTYPE, "null")) {
            WALLPAPER_DISCRETE -> {
                Log.d("Lufram", "UpdateDiscrete")
                UpdateDiscreteWallpaperTask(context).execute(id)
            }
            else -> {// TODO: Fix illegal subtype
                Log.w("Lufram", "Invalid wallpaper subtype detected!")
            }
        }
    }

    @SuppressWarnings("StaticFieldLeak")
    class UpdateDiscreteWallpaperTask(private var context: Context?) :
        WallpaperTask() {

        override fun onPostExecute(result: WallpaperCollection?) {
            if (result === null) {
                Log.w("Lufram", "User deleted wallpaper externally (probably); updater found no result!")
                return
            }
            if (context == null) {
                Log.d("Lufram", "Updater has no context")
                return
            }

            val luframPrefs = (context as Context).getSharedPreferences(LUFRAM_PREFS, 0)
            val newOffset: Int = run {
                luframPrefs.getInt(PREF_WALLPAPER_STATE, 0) + 1
            }

            (context?.getSystemService(WALLPAPER_SERVICE) as WallpaperManager)
                .setBitmap(
                    Ion.with(context)
                        .load(result.sources[newOffset].toString())
                        .asBitmap()
                        .get()
                )

            luframPrefs.edit().apply {
                putInt(PREF_WALLPAPER_STATE, newOffset)
            }.apply()

            context = null
        }
    }
}
