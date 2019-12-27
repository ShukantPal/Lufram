package com.zexfer.lufram.adapters

import android.animation.LayoutTransition
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.recyclerview.widget.*
import com.zexfer.lufram.Lufram
import com.zexfer.lufram.LuframRepository
import com.zexfer.lufram.R
import com.zexfer.lufram.WallpaperUpdateController
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.databinding.LayoutWallpaperPreviewCardBinding
import com.zexfer.lufram.expanders.Expander
import java.util.*

class ShowcaseViewAdapter(
    private val inflater: LayoutInflater,
    private val editorNavAction: Int
) : ListAdapter<WallpaperCollection, ViewHolder>(DIFF_CALLBACK) {

    private var rvs: MutableList<RecyclerView> = mutableListOf()

    private var attachCount = 0
    private var animateTimer: Timer? = null
    private var animateTask = SwitchPreviewImageTask()

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
        enableShowcaseAnimation()

        rvs.add(recyclerView)
        recyclerView.addOnScrollListener(animateStartupListener)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                inflater,
                R.layout.layout_wallpaper_preview_card,
                parent,
                false
            ),
            editorNavAction
        )
    }

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
            disableShowcaseAnimation()
        }
    }

    fun enableShowcaseAnimation() {
        if (this.animateTimer != null) {// already enabled
            return
        }

        animateTimer = Timer()
        animateTask = animateTask.resetClone()
        animateTimer!!.scheduleAtFixedRate(animateTask, 1500, 2500)
    }

    fun disableShowcaseAnimation() {
        animateTimer!!.cancel()
        animateTimer = null
    }

    companion object {
        @JvmStatic
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WallpaperCollection>() {
            override fun areItemsTheSame(
                oldItem: WallpaperCollection,
                newItem: WallpaperCollection
            ): Boolean {
                return oldItem.rowId == newItem.rowId
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

class ViewHolder(
    private val viewBinding: LayoutWallpaperPreviewCardBinding,
    private val editorNavAction: Int
) : RecyclerView.ViewHolder(viewBinding.root),
    SharedPreferences.OnSharedPreferenceChangeListener,
    View.OnClickListener {

    private var shownPreviewImageIndex: Int = 0
    private var imageExpander: Expander? = null
    private var boundWallpaper: WallpaperCollection? = null
    private var boundWallpaperId: Int = -1

    private val previewImageClickListener: View.OnClickListener =
        View.OnClickListener { openEditor() }

    init {
        viewBinding.clickListener = this

        // Smooth animations when the WallpaperCollectionDao is modified
        viewBinding.contentFrame
            .layoutTransition
            .enableTransitionType(LayoutTransition.CHANGING)

        // Apply layout style based on user's layout setting.
        when (Lufram.instance.defaultPrefs.getString("preview_layout", "Grid")) {
            "List" -> {
                viewBinding.parentCard.apply {
                    radius = 0f

                    (layoutParams as ViewGroup.MarginLayoutParams).apply {
                        marginStart = 0
                        topMargin *= 2 // 8dp to 16dp, no computation
                    }
                }
            }
        }
    }

    fun bindTo(wallpaper: WallpaperCollection) {
        viewBinding.textName.text = wallpaper.label
        boundWallpaper = wallpaper
        boundWallpaperId = wallpaper.rowId!!
        shownPreviewImageIndex = 0
        imageExpander = Expander.open(wallpaper)

        viewBinding.imagePreviewPager.adapter = WallpaperPreviewAdapter(
            Lufram.context,
            imageExpander!!,
            0,
            previewImageClickListener
        )

        onSharedPreferenceChanged(LuframRepository.luframPrefs, Lufram.PREF_WALLPAPER_ID)
    }

    fun expand() {
        if (viewBinding.expansionLayout.visibility == View.GONE)
            onClick(viewBinding.descriptionBar)
    }

    fun openEditor() {
        Navigation.findNavController(viewBinding.root)
            .navigate(
                editorNavAction,
                Bundle().apply {
                    putParcelable("source", boundWallpaper)
                })
    }

    fun slidePreviewImage() {
        Handler(Looper.getMainLooper()).post {
            if (shownPreviewImageIndex != viewBinding.imagePreviewPager.currentItem) {
                shownPreviewImageIndex = viewBinding.imagePreviewPager.currentItem
                return@post // give the user a little more time!
            }

            shownPreviewImageIndex =
                (shownPreviewImageIndex + 1) % imageExpander!!.size
            viewBinding.imagePreviewPager.setCurrentItem(shownPreviewImageIndex, true)
        }
    }

    override fun onClick(view: View?) {
        if (view === null) {
            return
        }

        when (view.id) {
            R.id.description_bar -> {
                viewBinding.expansionLayout.visibility =
                    if (viewBinding.expansionLayout.visibility == View.GONE)
                        View.VISIBLE else View.GONE
                viewBinding.btnExpand.setImageResource(
                    if (viewBinding.expansionLayout.visibility == View.GONE)
                        R.drawable.ic_expand_more_black_24dp else
                        R.drawable.ic_expand_less_black_24dp
                )
            }
            R.id.btn_apply -> {
                if (boundWallpaperId != -1) {
                    if (viewBinding.btnApply.text == "Apply") {
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
            viewBinding.btnApply.text = "Stop"
        } else {
            viewBinding.btnApply.text = "Apply"
        }
    }
}

/**
 * Task that is used to switch card preview wallpapers.
 *
 * Usage note: This cannot be reused once cancelled - use {@code resetClone}
 * to get a copy.
 */
class SwitchPreviewImageTask(
    private val targetCards: MutableList<ViewHolder> = mutableListOf()
) : TimerTask() {

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

    fun resetClone() = SwitchPreviewImageTask(targetCards)
}