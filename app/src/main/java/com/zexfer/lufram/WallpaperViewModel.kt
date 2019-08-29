package com.zexfer.lufram

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.DiscreteWallpaper

class WallpaperViewModel : ViewModel() {
    /**
     * Used for bi-directional communication on which wallpaper is
     * being used/edited/activated/etc. It is a hack, and should
     * not be encouraged.
     */
    var targetWallpaper: DiscreteWallpaper? = null

    val discreteWallpapers: LiveData<List<DiscreteWallpaper>> by lazy {
        LuframDatabase.instance.allDiscreteWallpapers()
    }

    fun putDiscreteWallpaper(wallpaper: DiscreteWallpaper) {
        LuframRepository.putWallpaper(wallpaper)
    }
}