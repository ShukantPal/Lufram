package com.zexfer.lufram.database.tasks

import android.os.AsyncTask
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.DiscreteWallpaper

abstract class DiscreteWallpaperTask : AsyncTask<Int, Void, DiscreteWallpaper>() {
    override fun doInBackground(vararg id: Int?): DiscreteWallpaper {
        return LuframDatabase.instance.discreteWallpaperDao().byId(
            id[0] ?: throw IllegalArgumentException("DiscreteWallpaperTask requires a valid id")
        )
    }
}