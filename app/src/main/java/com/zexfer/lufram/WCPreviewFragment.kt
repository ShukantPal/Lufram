package com.zexfer.lufram

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.recyclerview.widget.*
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.expanders.Expander
import java.util.*

class WCPreviewFragment : Fragment() {

    private var rvRoot: RecyclerView? = null
    private var textEmptyLibrary: TextView? = null
    private var wcListAdapter: WCListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_wallpaper_preview, container, false).also {
            rvRoot = it.findViewById(R.id.rv_root)
            textEmptyLibrary = it.findViewById(R.id.text_none)
            wcListAdapter = WCListAdapter(inflater)

            rvRoot!!.adapter = wcListAdapter
            (rvRoot!!.layoutManager as GridLayoutManager).spanCount = 2

            LuframDatabase.instance
                .wcDao()
                .all()
                .observe(this, Observer<List<WallpaperCollection>> {
                    wcListAdapter!!.submitList(ArrayList(it))
                })
        }

    override fun onDestroyView() {
        super.onDestroyView()
        rvRoot = null
        textEmptyLibrary = null
        wcListAdapter = null
    }

    class WCListAdapter(private val inflater: LayoutInflater) :
        ListAdapter<WallpaperCollection, ViewHolder>(DIFF_CALLBACK) {

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

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder =
            ViewHolder(
                inflater.inflate(
                    R.layout.layout_wc_card,
                    parent,
                    false
                )
            )

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
        SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener {

        private val previewPager: ViewPager = rootCardView.findViewById(R.id.image_preview_pager)
        private val nameView: TextView = rootCardView.findViewById(R.id.text_name)
        private val btnApply: Button = rootCardView.findViewById(R.id.btn_apply)
        private val btnEdit: AppCompatImageButton = rootCardView.findViewById(R.id.btn_edit)
        private val btnDelete: AppCompatImageButton = rootCardView.findViewById(R.id.btn_delete)

        private var shownPreviewImageIndex: Int = 0
        private var imageExpander: Expander? = null
        private var boundWallpaper: WallpaperCollection? = null
        private var boundWallpaperId: Int = -1

        private val previewAdapter = object : PagerAdapter() {
            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                return ImageView(rootCardView.context).apply {
                    imageExpander!!.load(context, position) {
                        setImageBitmap(it)
                    }

                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
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
                imageExpander?.size ?: 0
        }

        init {
            previewPager.adapter = previewAdapter
            btnApply.setOnClickListener(this)
            btnEdit.setOnClickListener(this)
            btnDelete.setOnClickListener(this)
        }

        fun bindTo(wallpaper: WallpaperCollection) {
            nameView.text = wallpaper.label
            boundWallpaper = wallpaper
            boundWallpaperId = wallpaper.id!!
            shownPreviewImageIndex = 0
            imageExpander = Expander.open(wallpaper)

            previewAdapter.notifyDataSetChanged()
            onSharedPreferenceChanged(LuframRepository.luframPrefs, Lufram.PREF_WALLPAPER_ID)
        }

        fun slidePreviewImage() {
            Handler(Looper.getMainLooper()).post {
                if (shownPreviewImageIndex != previewPager.currentItem) {
                    shownPreviewImageIndex = previewPager.currentItem
                    return@post // give the user a little more time!
                }

                shownPreviewImageIndex =
                    (shownPreviewImageIndex + 1) % imageExpander!!.size
                previewPager.setCurrentItem(shownPreviewImageIndex, true)
            }
        }

        override fun onClick(view: View?) {
            if (view === null) {
                return
            }

            when (view.id) {
                R.id.btn_apply -> {
                    if (boundWallpaperId != -1) {
                        if (btnApply.text == "Apply") {
                            LuframRepository.applyWallpaper(boundWallpaperId)
                        } else {
                            LuframRepository.stopWallpaper()
                        }
                    }
                }
                R.id.btn_edit -> {
                    Navigation.findNavController(rootCardView)
                        .navigate(
                            R.id.action_mainFragment_to_WCEditorFragment,
                            Bundle().apply {
                                putParcelable("source", boundWallpaper)
                            })
                }
                R.id.btn_delete -> {
                    LuframRepository.deleteWallpaper(boundWallpaperId)
                }
            }
        }

        override fun onSharedPreferenceChanged(
            luframPrefs: SharedPreferences?,
            changedPref: String?
        ) {
            if (changedPref !== Lufram.PREF_WALLPAPER_ID)
                return

            if (luframPrefs?.getInt(changedPref, -2) == boundWallpaperId) {
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
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WallpaperCollection>() {
            override fun areItemsTheSame(
                oldItem: WallpaperCollection,
                newItem: WallpaperCollection
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: WallpaperCollection,
                newItem: WallpaperCollection
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}