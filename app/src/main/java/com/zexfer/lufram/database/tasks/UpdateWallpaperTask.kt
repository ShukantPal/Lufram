package com.zexfer.lufram.database.tasks

import android.os.AsyncTask
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.WallpaperCollection

class UpdateWallpaperTask(private val updater: (WallpaperCollection) -> Unit) :
    AsyncTask<Int, Void, Void>() {

    override fun doInBackground(vararg args: Int?): Void? {
        val wcDao = LuframDatabase.instance.wcDao()

        for (arg in args) {
            if (arg == null)
                continue

            val wc = wcDao.byId(arg)
            updater(wc)
            wcDao.put(wc)
        }

        return null
    }
}