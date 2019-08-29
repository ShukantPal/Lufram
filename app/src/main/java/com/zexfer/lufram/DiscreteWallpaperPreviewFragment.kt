package com.zexfer.lufram

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koushikdutta.ion.Ion
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_SUBTYPE
import com.zexfer.lufram.Lufram.Companion.WALLPAPER_DISCRETE
import com.zexfer.lufram.database.models.DiscreteWallpaper

class DiscreteWallpaperPreviewFragment :
    WallpaperPreviewFragment<DiscreteWallpaper>() {

    init {
        adapterId = Lufram.ADAPTER_DISCRETE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        listAdapter = Adapter(
            activity?.layoutInflater ?: throw IllegalStateException("LayoutInflater is not present, cannot continue!")
        )

        super.onCreate(savedInstanceState)
    }

    class Adapter(private val inflater: LayoutInflater) :
        ListAdapter<DiscreteWallpaper, ViewHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(inflater.inflate(R.layout.layout_discrete_wallpaper, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindTo(getItem(position))
        }

        override fun onViewAttachedToWindow(holder: ViewHolder) {
            super.onViewAttachedToWindow(holder)
            LuframRepository.luframPrefs.registerOnSharedPreferenceChangeListener(holder)
        }

        override fun onViewDetachedFromWindow(holder: ViewHolder) {
            super.onViewDetachedFromWindow(holder)
            LuframRepository.luframPrefs.unregisterOnSharedPreferenceChangeListener(holder)
        }
    }

    class ViewHolder(rootCardView: View) : RecyclerView.ViewHolder(rootCardView), OnSharedPreferenceChangeListener,
        View.OnClickListener, PopupMenu.OnMenuItemClickListener {

        private val nameView: TextView = rootCardView.findViewById(R.id.text_name)
        private val btnMenu: AppCompatImageButton = rootCardView.findViewById(R.id.btn_menu)
        private val imagePreview: ImageView = rootCardView.findViewById(R.id.image_preview)
        private val btnApply: Button = rootCardView.findViewById(R.id.btn_apply)

        private val popupMenu = PopupMenu(rootCardView.context, btnMenu)

        init {
            btnApply.setOnClickListener(this)
            btnMenu.setOnClickListener(this)

            popupMenu.also {
                it.menuInflater.inflate(R.menu.menu_discrete_wallpaper_preview, it.menu)
                it.setOnMenuItemClickListener(this)
            }
        }

        private var boundWallpaperId: Int = -1

        fun bindTo(wallpaper: DiscreteWallpaper) {
            nameView.text = wallpaper.name
            boundWallpaperId = wallpaper.id ?: -1

            Ion.with(imagePreview)
                .load(wallpaper.inputURIs[0].toString())

            // one-time fire for "Stop"/"Apply" check!
            onSharedPreferenceChanged(LuframRepository.luframPrefs, PREF_WALLPAPER_ID)
        }

        override fun onClick(view: View?) {
            if (view === null) {
                return
            }

            when (view.id) {
                R.id.btn_apply -> {
                    if (boundWallpaperId != -1) {
                        if (btnApply.text.equals("Apply")) {
                            LuframRepository.applyDiscreteWallpaper(boundWallpaperId)
                        } else {
                            LuframRepository.stopWallpaper()
                        }
                    }
                }
                R.id.btn_menu ->
                    popupMenu.show()
            }
        }

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            when (item?.order) {
                1 -> { // Delete
                    LuframRepository.deleteDiscreteWallpaper(boundWallpaperId)
                }
            }

            return false
        }

        override fun onSharedPreferenceChanged(luframPrefs: SharedPreferences?, changedPref: String?) {
            if (changedPref !== PREF_WALLPAPER_ID ||
                !luframPrefs?.getString(PREF_WALLPAPER_SUBTYPE, "null").equals(WALLPAPER_DISCRETE)
            )
                return

            if (luframPrefs?.getInt(changedPref, -1) == boundWallpaperId) {
                btnApply.text = "Stop"
            } else {
                btnApply.text = "Apply"
            }
        }
    }

    companion object {
        @JvmStatic
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DiscreteWallpaper>() {
            override fun areItemsTheSame(oldItem: DiscreteWallpaper, newItem: DiscreteWallpaper): Boolean {
                return oldItem.equals(newItem)
            }

            override fun areContentsTheSame(oldItem: DiscreteWallpaper, newItem: DiscreteWallpaper): Boolean {
                return oldItem.equals(newItem)
            }
        }
    }
}