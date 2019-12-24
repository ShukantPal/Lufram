package com.zexfer.lufram

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import androidx.preference.PreferenceManager
import com.zexfer.lufram.billing.IabHelper
import java.util.concurrent.Executors

class Lufram : Application() {

    lateinit var iabHelper: IabHelper
    lateinit var defaultPrefs: SharedPreferences

    val wallpaperTaskExecutor by lazy { Executors.newSingleThreadExecutor() }

    override fun onCreate() {
        super.onCreate()

        INSTANCE = this
        CONTEXT = this.applicationContext
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        iabHelper = IabHelper(context, BuildConfig.LICENSE_KEY).apply {
            startSetup { result ->
                if (result.isFailure) {
                    Log.e("Lufram", "iabHelp fail $result")
                }
            }
        }
    }

    companion object {
        private lateinit var INSTANCE: Lufram
        private lateinit var CONTEXT: Context

        @JvmStatic
        val instance: Lufram
            get() {
                return INSTANCE
            }

        @JvmStatic
        val context: Context
            get() {
                return CONTEXT
            }

        val renderScript by lazy {
            RenderScript.create(context)
        }

        val blurScript by lazy {
            val script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
            script.setRadius(25f)
            script
        }

        fun applyBlur(bitmap: Bitmap, out: Bitmap = bitmap) {
            val inAlloc = Allocation.createFromBitmap(renderScript, bitmap)
            val outAlloc = Allocation.createTyped(renderScript, inAlloc.type)

            blurScript.setInput(inAlloc)
            blurScript.forEach(outAlloc)

            outAlloc.copyTo(out)
        }

        /**
         * Default database key used in Lufram.
         */
        @JvmStatic
        val LUFRAM_DB = "LuframDatabase"

        // Custom intent extras used throughout the codebase
        val EXTRA_WALLPAPER = "Result@TargetWallpaper"
        val EXTRA_UPDATER_ID = "Result@UpdaterId"
        val EXTRA_CONFIG_PREFS = "config_prefs"

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
        val PREF_UPDATER_TIMESTAMP = "Updater_Timestamp"

        val PREF_CONFIG_TYPE = "cfg_type"
        val PREF_CONFIG_INTERVAL_MILLIS = "cfg_interval_millis"
        val PREF_CONFIG_RANDOMIZE_ORDER = "cfg_randomize_order"
        val PREF_CONFIG_DAY_RANGE = "cfg_day_range"
        val PREF_CONFIG_TIMEZONE_ADJUSTED_ENABLED = "cfg_timezone_adjust_enabled"

        // SharedPrefs.PREF_WALLPAPER_SUBTYPE values
        val WALLPAPER_DISCRETE = "Type=DiscreteDeprecatedWallpaperCollection"
    }
}