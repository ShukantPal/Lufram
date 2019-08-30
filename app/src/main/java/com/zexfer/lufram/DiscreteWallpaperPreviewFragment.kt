package com.zexfer.lufram

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.koushikdutta.ion.Ion
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_ID
import com.zexfer.lufram.Lufram.Companion.PREF_WALLPAPER_SUBTYPE
import com.zexfer.lufram.Lufram.Companion.WALLPAPER_DISCRETE
import com.zexfer.lufram.database.models.DiscreteWallpaper
import java.util.*

class DiscreteWallpaperPreviewFragment : WallpaperPreviewFragment<DiscreteWallpaper>() {

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

        private var attachCount = 0

        private var animateTimer: Timer? = null

        private val animateTask = SwitchPreviewImageTask()

        private val animateStartupListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val layoutManager = recyclerView.layoutManager

                if (!(layoutManager is LinearLayoutManager)) {
                    return
                }

                val startPos = layoutManager.findFirstVisibleItemPosition()
                val endPos = layoutManager.findLastVisibleItemPosition()

                for (i in startPos..endPos) {
                    animateTask.bind(
                        recyclerView.findViewHolderForAdapterPosition(i)
                                as ViewHolder
                    )
                }
            }
        }

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            ++attachCount

            if (animateTimer === null) {
                animateTimer = Timer()
                animateTimer!!.scheduleAtFixedRate(animateTask, 1500, 2500)
            }

            recyclerView.addOnScrollListener(animateStartupListener)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(inflater.inflate(R.layout.layout_discrete_wallpaper, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindTo(getItem(position))
        }

        override fun onViewAttachedToWindow(holder: ViewHolder) {
            LuframRepository.luframPrefs.registerOnSharedPreferenceChangeListener(holder)
            animateTask.bind(holder)
        }

        override fun onViewDetachedFromWindow(holder: ViewHolder) {
            LuframRepository.luframPrefs.unregisterOnSharedPreferenceChangeListener(holder)
            animateTask.unbind(holder)
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            recyclerView.removeOnScrollListener(animateStartupListener)
            --attachCount

            if (attachCount == 0) {
                animateTask.clear()
                animateTimer?.cancel()
                animateTimer = null
            }
        }
    }

    class ViewHolder(private val rootCardView: View) : RecyclerView.ViewHolder(rootCardView),
        OnSharedPreferenceChangeListener, View.OnClickListener {

        private val previewPager: ViewPager = rootCardView.findViewById(R.id.image_preview_pager)
        private val nameView: TextView = rootCardView.findViewById(R.id.text_name)
        private val btnApply: Button = rootCardView.findViewById(R.id.btn_apply)
        private val btnEdit: AppCompatImageButton = rootCardView.findViewById(R.id.btn_edit)
        private val btnDelete: AppCompatImageButton = rootCardView.findViewById(R.id.btn_delete)

        private var shownPreviewImageIndex: Int = 0
        private var boundWallpaper: DiscreteWallpaper? = null
        private var boundWallpaperId: Int = -1

        private val previewAdapter = object : PagerAdapter() {
            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val uri = boundWallpaper!!.inputURIs[position].toString()

                return ImageView(rootCardView.context).apply {
                    Ion.with(this)
                        .load(uri)

                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }.also {
                    container.addView(it)
                }
            }

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                container.removeView(`object` as View)
            }

            override fun isViewFromObject(view: View, `object`: Any): Boolean =
                view === `object`

            override fun getCount(): Int =
                boundWallpaper?.inputURIs?.size ?: 0
        }

        init {
            previewPager.adapter = previewAdapter
            btnApply.setOnClickListener(this)
            btnEdit.setOnClickListener(this)
            btnDelete.setOnClickListener(this)
        }

        fun bindTo(wallpaper: DiscreteWallpaper) {
            nameView.text = wallpaper.name
            boundWallpaper = wallpaper
            boundWallpaperId = wallpaper.id ?: -1
            shownPreviewImageIndex = 0

            previewAdapter.notifyDataSetChanged()
            onSharedPreferenceChanged(LuframRepository.luframPrefs, PREF_WALLPAPER_ID)
        }

        fun slidePreviewImage() {
            Handler(Looper.getMainLooper()).post {
                if (shownPreviewImageIndex != previewPager.currentItem) {
                    shownPreviewImageIndex = previewPager.currentItem
                    return@post // give the user a little more time!
                }

                shownPreviewImageIndex = (shownPreviewImageIndex + 1) % boundWallpaper!!.inputURIs.size
                previewPager.setCurrentItem(shownPreviewImageIndex, true)
            }
        }

        override fun onClick(view: View?) {
            if (view === null) {
                return
            }

            when (view.id) {
                R.id.btn_apply -> {
                    if (boundWallpaper?.id !== null) {
                        if (btnApply.text.equals("Apply")) {
                            LuframRepository.applyDiscreteWallpaper(boundWallpaperId)
                        } else {
                            LuframRepository.stopWallpaper()
                        }
                    }
                }
                R.id.btn_edit -> {
                    Navigation.findNavController(rootCardView)
                        .navigate(R.id.action_discreteWallpaperPreviewFragment2_to_discreteWallpaperEditorFragment2,
                            Bundle().apply {
                                putParcelable("source", boundWallpaper)
                            })
                }
                R.id.btn_delete -> {
                    LuframRepository.deleteDiscreteWallpaper(boundWallpaperId)
                }
            }
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

    class SwitchPreviewImageTask : TimerTask() {

        private val targetCards: MutableList<ViewHolder> = mutableListOf()

        fun bind(cardHolder: ViewHolder) {
            if (!targetCards.contains(cardHolder))
                targetCards.add(cardHolder)
        }

        fun unbind(cardHolder: ViewHolder) {
            targetCards.remove(cardHolder)
        }

        override fun run() {
            for (cardHolder in targetCards)
                cardHolder.slidePreviewImage()
        }

        fun clear() {
            targetCards.clear()
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

        @JvmStatic
        val EDIT_DISCRETE_WALLPAPER = 102
    }
}