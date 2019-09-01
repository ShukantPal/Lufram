package com.zexfer.lufram.expanders

import android.content.Context
import android.graphics.Bitmap
import com.koushikdutta.ion.Ion
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.database.models.WallpaperCollection.Companion.FORMAT_IMAGES

class ImagesExpander(private val wc: WallpaperCollection) : Expander {
    init {
        if (wc.formatType != FORMAT_IMAGES) {
            throw IllegalArgumentException(
                "ImagesExpander must get a " +
                        "collection with format FORMAT_IMAGES."
            )
        }
    }

    override val size: Int
        get() = wc.sources.size

    override fun load(context: Context, index: Int, callback: ((Bitmap) -> Unit)?) =
        Ion.with(context)
            .load(wc.sources[index].toString())
            .asBitmap()
            .apply {
                if (callback !== null)
                    setCallback { _, bitmap ->
                        callback(bitmap)
                    }
            }

    override fun cut(index: Int) {
        wc.sources = wc.sources.toMutableList().run {
            removeAt(index)
            toTypedArray()
        }
    }
}