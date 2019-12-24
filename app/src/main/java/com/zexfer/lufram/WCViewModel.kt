package com.zexfer.lufram

import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_INDEX

class WCViewModel : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {

    val activeWallpaperBitmap: MutableLiveData<Bitmap> = MutableLiveData()

    init {
        refreshActiveWallpaperBitmap()

        LuframRepository.luframPrefs
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        LuframRepository.luframPrefs
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        when (key) {
            PREF_WALLPAPER_ID, PREF_WALLPAPER_INDEX -> {
                refreshActiveWallpaperBitmap()
            }
        }
    }

    private fun refreshActiveWallpaperBitmap() {
        WallpaperUpdateController.estimatedWallpaperAsync {
            activeWallpaperBitmap.value = it
        }
    }
}