package com.zexfer.lufram

import android.animation.LayoutTransition
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatImageButton
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.expanders.Expander
import java.util.*

class WCPreviewFragment : Fragment() {

    private var rvRoot: RecyclerView? = null
    private var frameNothingHere: View? = null
    private var wcListAdapter: WCListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_wc_preview, container, false).also {
            rvRoot = it.findViewById(R.id.rv_root)
            frameNothingHere = it.findViewById(R.id.frame_none)
            wcListAdapter = WCListAdapter(inflater)

            rvRoot!!.adapter = wcListAdapter
            rvRoot!!.isVerticalScrollBarEnabled = false
            if (Lufram.instance.defaultPrefs.getString("preview_layout", "Grid") == "List") {
                rvRoot!!.setPadding(0, 0, 0, 240)
            }

            when (Lufram.instance.defaultPrefs.getString("preview_layout", "Grid")) {
                "Cards", "List" -> rvRoot!!.layoutManager = LinearLayoutManager(context)
                "Grid" -> rvRoot!!.layoutManager = StaggeredGridLayoutManager(2, VERTICAL)
            }

            WallpaperUpdateController.estimatedWallpaperAsync { wp ->
                if (wp === null) {
                    return@estimatedWallpaperAsync
                }

                val blurredWp = Bitmap.createBitmap(wp.width, wp.height, Bitmap.Config.ARGB_8888)
                val paint = Paint()
                paint.colorFilter =
                    PorterDuffColorFilter(0xeeffffff.toInt(), PorterDuff.Mode.SRC_OVER)

                Canvas(blurredWp).drawBitmap(wp, 0.toFloat(), 0.toFloat(), paint)

                it.background = BitmapDrawable(resources, blurredWp)
                Log.d("Lufram", " setwp")
            }

            if (savedInstanceState === null) {
                LuframDatabase.instance
                    .wcDao()
                    .allSorted()
                    .observe(this, Observer<List<WallpaperCollection>> {
                        if (it.size == 0) {
                            frameNothingHere!!.visibility = View.VISIBLE
                        } else {
                            frameNothingHere!!.visibility = View.GONE
                        }

                        wcListAdapter!!.submitList(ArrayList(it))
                    })
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        rvRoot = null
        frameNothingHere = null
        wcListAdapter = null
    }

    class WCListAdapter(private val inflater: LayoutInflater) :
        ListAdapter<WallpaperCollection, ViewHolder>(DIFF_CALLBACK) {

        private var rvs: MutableList<RecyclerView> = mutableListOf()

        private var attachCount = 0
        private var animateTimer: Timer? = null
        private val animateTask = SwitchPreviewImageTask()

        private val animateStartupListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING)
                        animateTask.clear()

                    return
                }

                val layoutManager = recyclerView.layoutManager

                if (layoutManager is LinearLayoutManager) {
                    val startPosition = layoutManager.findFirstVisibleItemPosition()
                    val endPosition = layoutManager.findLastVisibleItemPosition()

                    for (i in startPosition..endPosition) {
                        animateTask.bind(
                            (recyclerView.findViewHolderForAdapterPosition(i) as ViewHolder?)
                                ?: continue
                        )
                    }
                } else if (layoutManager is StaggeredGridLayoutManager) {
                    val startPositions = IntArray(3).also {
                        layoutManager.findFirstCompletelyVisibleItemPositions(it)
                    }

                    val endPositions = IntArray(3).also {
                        layoutManager.findLastVisibleItemPositions(it)
                    }

                    for (i in 0 until 3) {
                        for (j in startPositions[i]..endPositions[i]) {
                            animateTask.bind(
                                (recyclerView.findViewHolderForAdapterPosition(j)
                                        as ViewHolder?) ?: continue
                            )
                        }
                    }
                }
            }
        }

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            ++attachCount

            if (animateTimer === null) {
                animateTimer = Timer()
                animateTimer!!.scheduleAtFixedRate(animateTask, 1500, 2500)
            }

            rvs.add(recyclerView)
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
                ) as CardView
            )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindTo(getItem(position))

            if (position == 0) {
                holder.expand()
            }
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
            rvs.remove(recyclerView)
            recyclerView.removeOnScrollListener(animateStartupListener)
            --attachCount

            if (attachCount == 0) {
                animateTask.clear()
                animateTimer?.cancel()
                animateTimer = null
            }
        }
    }

    class ViewHolder(private val rootCardView: CardView) : RecyclerView.ViewHolder(rootCardView),
        SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener {

        private val previewPager: ViewPager = rootCardView.findViewById(R.id.image_preview_pager)
        private val nameView: TextView = rootCardView.findViewById(R.id.text_name)
        private val expansionSection: RelativeLayout = rootCardView.findViewById(R.id.section_more)

        private val btnExpand: AppCompatImageButton = rootCardView.findViewById(R.id.btn_expand)
        private val btnApply: Button = rootCardView.findViewById(R.id.btn_apply)
        private val btnEdit: AppCompatImageButton = rootCardView.findViewById(R.id.btn_edit)
        private val btnDelete: AppCompatImageButton = rootCardView.findViewById(R.id.btn_delete)

        private var shownPreviewImageIndex: Int = 0
        private var imageExpander: Expander? = null
        private var boundWallpaper: WallpaperCollection? = null
        private var boundWallpaperId: Int = -1

        init {
            when (Lufram.instance.defaultPrefs.getString("preview_layout", "Grid")) {
                "List" -> {
                    rootCardView.cardElevation = 0.toFloat()
                    rootCardView.radius = 0.toFloat()
                    (rootCardView.layoutParams as ViewGroup.MarginLayoutParams).let {
                        it.marginStart = 0
                        it.topMargin = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_PX,
                            16.toFloat(),
                            Lufram.instance.resources.displayMetrics
                        ).toInt()
                    }
                }
            }
        }

        private val previewAdapter = object : PagerAdapter() {
            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                return ImageView(rootCardView.context).apply {
                    imageExpander!!.load(context, position) {
                        setImageBitmap(it)
                    }

                    isClickable = true
                    setOnClickListener { _ -> openEditor() }
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
            rootCardView.findViewById<LinearLayout>(R.id.content_frame)
                .layoutTransition
                .enableTransitionType(LayoutTransition.CHANGING)

            previewPager.adapter = previewAdapter
            btnExpand.setOnClickListener(this)
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

        fun expand() {
            if (expansionSection.visibility == View.GONE)
                onClick(btnExpand)
        }

        fun openEditor() {
            Navigation.findNavController(rootCardView)
                .navigate(
                    R.id.action_mainFragment_to_WCEditorFragment,
                    Bundle().apply {
                        putParcelable("source", boundWallpaper)
                    })
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
                R.id.btn_expand -> {
                    expansionSection.visibility = if (expansionSection.visibility == View.GONE)
                        View.VISIBLE else View.GONE
                    btnExpand.setImageResource(
                        if (expansionSection.visibility == View.GONE)
                            R.drawable.ic_expand_more_black_24dp else
                            R.drawable.ic_expand_less_black_24dp
                    )
                }
                R.id.btn_apply -> {
                    if (boundWallpaperId != -1) {
                        if (btnApply.text == "Apply") {
                            WallpaperUpdateController.setTargetIdAsync(boundWallpaperId)
                        } else {
                            LuframRepository.stopWallpaper()
                        }
                    }
                }
                R.id.btn_edit -> {
                    openEditor()
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
                return oldItem.label == newItem.label &&
                        oldItem.sources.equals(newItem.sources)
            }
        }
    }
}