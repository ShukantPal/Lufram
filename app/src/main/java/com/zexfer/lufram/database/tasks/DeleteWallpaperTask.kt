package com.zexfer.lufram.database.tasks

import android.os.AsyncTask
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.DiscreteWallpaper
import com.zexfer.lufram.database.models.Wallpaper

class DeleteWallpaperTask : AsyncTask<Wallpaper, Void, Void>() {
    override fun doInBackground(vararg wps: Wallpaper?): Void? {
        for (wp in wps) {
            if (wp is DiscreteWallpaper) {
                LuframDatabase.instance.discreteWallpaperDao().delete(wp)
            }
        }

        return null
    }
}