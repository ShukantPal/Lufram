package com.zexfer.lufram.database.tasks

import android.os.AsyncTask
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.WallpaperCollection

abstract class WallpaperTask : AsyncTask<Int, Void, WallpaperCollection>() {
    override fun doInBackground(vararg wp: Int?): WallpaperCollection {
        return LuframDatabase.instance
            .wcDao()
            .byId(wp[0] ?: -1)
    }

    companion object {
        fun run(wp: Int, callback: (WallpaperCollection) -> Unit) {
            object : WallpaperTask() {
                override fun onPostExecute(result: WallpaperCollection?) {
                    if (result !== null)
                        callback(result)
                }
            }.execute(wp)
        }
    }
}