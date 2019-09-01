package com.zexfer.lufram.database.tasks

import android.os.AsyncTask
import com.zexfer.lufram.database.LuframDatabase

class DeleteWallpaperByIdTask : AsyncTask<Int, Void, Void>() {
    override fun doInBackground(vararg wps: Int?): Void? {
        LuframDatabase.instance
            .wcDao()
            .deleteById(wps as Array<Int>)

        return null
    }
}