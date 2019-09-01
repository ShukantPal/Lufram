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

        @JvmStatic
        val EXTRA_WALLPAPER = "Result@TargetWallpaper"

        @JvmStatic
        val EXTRA_UPDATER_ID = "Result@UpdaterId"

        // Ids for different wallpaper-preview adapters

        @JvmStatic
        val ADAPTER_DISCRETE = 1

        // SharedPreferences keys

        @JvmStatic
        val LUFRAM_PREFS = "LuframPrefs"

        @JvmStatic
        val PREF_WALLPAPER_SUBTYPE = "Type@PreferredWallpaper"

        @JvmStatic
        val PREF_WALLPAPER_ID = "Id@PreferredWallpaper"

        @JvmStatic
        val PREF_WALLPAPER_STATE = "State@PreferredWallpaper" // used by each type of wallpaper service

        @JvmStatic
        val PREF_WAS_STOPPED = "was_stopped" // if wallpaper was stopped by user

        @JvmStatic
        val PREF_UPDATER_ID =
            "UpdaterId@PreferredWallpaper" // incremented each time preferred wallpaper changes, so old updaters stop

        // SharedPrefs.PREF_WALLPAPER_SUBTYPE values

        @JvmStatic
        val WALLPAPER_DISCRETE = "Type=DiscreteDeprecatedWallpaperCollection"
    }
}