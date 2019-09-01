package com.zexfer.lufram.expanders

import android.content.Context
import android.graphics.Bitmap
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.database.models.WallpaperCollection.Companion.FORMAT_IMAGES
import java.util.concurrent.Future

interface Expander {
    val size: Int

    fun load(context: Context, index: Int, callback: ((Bitmap) -> Unit)?): Future<Bitmap>

    companion object {
        fun open(wc: WallpaperCollection): Expander {
            when (wc.formatType) {
                FORMAT_IMAGES -> return ImagesExpander(wc)
                else -> throw UnsupportedOperationException("Unknown format type not supported")
            }
        }
    }
}