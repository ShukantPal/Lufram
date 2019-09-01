package com.zexfer.lufram.database.tasks

import android.os.AsyncTask
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.WallpaperCollection

class DeleteWallpaperTask : AsyncTask<WallpaperCollection, Void, Void>() {
    override fun doInBackground(vararg wps: WallpaperCollection?): Void? {
        for (wp in wps) {
            if (wp !== null)
                LuframDatabase.instance
                    .wcDao()
                    .delete(wp)
        }

        return null
    }
}