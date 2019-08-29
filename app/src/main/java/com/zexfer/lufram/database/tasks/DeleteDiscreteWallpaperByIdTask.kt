package com.zexfer.lufram.database.tasks

import android.os.AsyncTask
import com.zexfer.lufram.database.LuframDatabase

class DeleteDiscreteWallpaperByIdTask : AsyncTask<Int, Void, Void>() {
    override fun doInBackground(vararg ids: Int?): Void? {
        LuframDatabase.instance
            .discreteWallpaperDao()
            .deleteById(ids as Array<Int>)
        return null
    }
}