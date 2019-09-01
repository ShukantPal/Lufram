package com.zexfer.lufram

import android.app.Application
import android.content.Context

class Lufram : Application() {

    override fun onCreate() {
        super.onCreate()

        INSTANCE = this
        CONTEXT = this.applicationContext
    }

    companion object {
        private lateinit var INSTANCE: Application
        private lateinit var CONTEXT: Context

        @JvmStatic
        val instance: Application
            get() {
                return INSTANCE
            }

        @JvmStatic
        val context: Context
            get() {
                return CONTEXT
            }

        /**
         * Default database key used in Lufram.
         */
        @JvmStatic
        val LUFRAM_DB = "LuframDatabase"

        // Custom intent extras used throughout the codebase
        val EXTRA_WALLPAPER = "Result@TargetWallpaper"
        val EXTRA_UPDATER_ID = "Result@UpdaterId"

        // Ids for different wallpaper-preview adapters

        @JvmStatic
        val ADAPTER_DISCRETE = 1

        // SharedPreferences keys
        val LUFRAM_PREFS = "LuframPrefs"
        val PREF_WALLPAPER_ID = "Id@PreferredWallpaper"
        val PREF_WALLPAPER_INDEX =
            "State@PreferredWallpaper" // used by each type of wallpaper service
        val PREF_WAS_STOPPED = "was_stopped" // if wallpaper was stopped by user
        val PREF_UPDATER_ID =
            "UpdaterId@PreferredWallpaper" // incremented each time preferred wallpaper changes, so old updaters stop

        // SharedPrefs.PREF_WALLPAPER_SUBTYPE values
        val WALLPAPER_DISCRETE = "Type=DiscreteDeprecatedWallpaperCollection"
    }
}