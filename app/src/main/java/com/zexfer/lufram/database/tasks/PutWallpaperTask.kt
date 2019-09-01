package com.zexfer.lufram.database.tasks

import android.os.AsyncTask
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.WallpaperCollection

class PutWallpaperTask : AsyncTask<WallpaperCollection, Void, Void>() {
    override fun doInBackground(vararg wps: WallpaperCollection?): Void? {
        LuframDatabase.instance
            .wcDao()
            .put(wps[0]!!) // TODO: Allow multiple inseration simultaneously
        return null
    }
}