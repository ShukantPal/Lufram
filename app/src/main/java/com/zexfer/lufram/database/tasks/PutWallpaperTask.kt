package com.zexfer.lufram.database.tasks

import android.os.AsyncTask
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.DiscreteWallpaper
import com.zexfer.lufram.database.models.Wallpaper

class PutWallpaperTask : AsyncTask<Wallpaper, Void, Void>() {
    override fun doInBackground(vararg wps: Wallpaper?): Void? {
        for (wp in wps) {
            if (wp is DiscreteWallpaper) {
                val rowId = LuframDatabase.instance.discreteWallpaperDao().put(wp)

                if (wp.id == null)
                    wp.id = rowId.toInt()// TODO: Solve rowId problem
            }
        }

        return null
    }
}