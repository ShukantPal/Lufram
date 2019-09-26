package com.zexfer.lufram


import android.app.AlarmManager.INTERVAL_DAY
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.util.set
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.zexfer.lufram.LuframRepository.CONFIG_DYNAMIC
import com.zexfer.lufram.LuframRepository.CONFIG_PERIODIC
import com.zexfer.lufram.adapters.WallpaperPreviewAdapter
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.database.tasks.WallpaperTask
import com.zexfer.lufram.databinding.FragmentTimelineBinding
import com.zexfer.lufram.expanders.Expander
import java.util.*

class TimelineFragment : Fragment(),
    View.OnClickListener,
    ViewPager.OnPageChangeListener,
    PopupMenu.OnMenuItemClickListener {

    private lateinit var wallpaperCollection: WallpaperCollection
    private lateinit var expander: Expander

    private val configMode: Int by lazy {
        LuframRepository.configMode
    }

    private val timestampBase: Long by lazy {
        LuframRepository.lastUpdateTimestamp
    }

    private val wallpaperCount: Int
        get() = expander.size

    private val wallpaperBase: Int by lazy {
        WallpaperUpdateController.estimatedWallpaperIndex()
    }

    private lateinit var viewBinding: FragmentTimelineBinding
    private lateinit var blurCache: SparseArray<BitmapDrawable?>
    private lateinit var moreOptionsMenu: PopupMenu

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = DataBindingUtil.inflate<FragmentTimelineBinding>(
            inflater,
            R.layout.fragment_timeline,
            container,
            false
        )

        viewBinding.btnMore.setOnClickListener(this)
        WallpaperTask.run(arguments!!.getInt(ARG_ID), this::onWallpaperCollectionLoaded)

        moreOptionsMenu = PopupMenu(context!!, viewBinding.btnMore).apply {
            setOnMenuItemClickListener(this@TimelineFragment)
            inflate(R.menu.more_timeline_options)
        }

        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding.wcPager.setPageTransformer(false, null)
        viewBinding.wcPager.removeOnPageChangeListener(this)
        moreOptionsMenu.setOnMenuItemClickListener(null)
    }

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.btn_more -> {
                moreOptionsMenu.show()
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.item_whats_this -> {
                fragmentManager!!.beginTransaction()
                    .add(WhatsThisTimelineDialogFragment(), "whats_this_timeline")
                    .commitNow()
            }
            else -> {
                return false
            }
        }

        return true
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        val ts: Long
        val collIndex = (position + wallpaperBase) % wallpaperCount

        when (configMode) {
            CONFIG_PERIODIC -> {
                ts = timestampBase + LuframRepository.updateIntervalMillis * position
            }
            CONFIG_DYNAMIC -> {
                val tzTS = timestampBase + TimeZone.getDefault().rawOffset
                val tzDayTS = tzTS - tzTS % INTERVAL_DAY

                ts =
                    tzDayTS + (INTERVAL_DAY / wallpaperCount) * ((position + wallpaperBase) % wallpaperCount) - TimeZone.getDefault().rawOffset
            }
            else -> throw IllegalStateException("Illegal config mode")
        }

        val c0 = Calendar.getInstance().also { it.timeInMillis = timestampBase }
        val c = Calendar.getInstance().also { it.timeInMillis = ts }
        val tsHr = c.get(Calendar.HOUR_OF_DAY)
        val tsMin = c.get(Calendar.MINUTE)
        viewBinding.textEstimatedHitTime.text =
            LuframRepository.PeriodicConfig.formattedIntervalString(tsHr.toInt(), tsMin.toInt())

        val ddelt = (c.get(Calendar.DAY_OF_YEAR) - c0.get(Calendar.DAY_OF_YEAR) + 365) % 365

        val textEstimatedHitDay = viewBinding.textEstimatedHitDay
        when (ddelt) {
            0 -> textEstimatedHitDay!!.text = "Estimated Today"
            1 -> textEstimatedHitDay!!.text = "Estimated Tomorrow"
            else -> textEstimatedHitDay!!.text = "Estimated ${ddelt} days from now"
        }

        if (blurCache[collIndex] === null) {
            expander.load(context!!, collIndex) { srcBitmap ->
                val blurBitmap =
                    Bitmap.createBitmap(srcBitmap.width, srcBitmap.height, Bitmap.Config.ARGB_8888)

                Canvas(blurBitmap).drawBitmap(srcBitmap, 0f, 0f, BG_PAINT)

                blurCache.set(
                    collIndex,
                    BitmapDrawable(context!!.resources, blurBitmap)
                )

                viewBinding.root.background = blurCache.get(collIndex)
            }
        } else {
            viewBinding.root.background = blurCache.get(collIndex)
        }
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    private fun onWallpaperCollectionLoaded(wallpaperCollection: WallpaperCollection) {
        this.wallpaperCollection = wallpaperCollection
        expander = Expander.open(wallpaperCollection)
        blurCache = SparseArray(expander.size)

        viewBinding.wcPager.apply {
            adapter = WallpaperPreviewAdapter(context!!, expander, wallpaperBase)
            pageMargin = 16
            isHorizontalScrollBarEnabled = true

            setPageTransformer(false, CarouselEffectTransformer(context!!))
            addOnPageChangeListener(this@TimelineFragment)
        }

        onPageSelected(0)
    }

    class CarouselEffectTransformer(context: Context) : ViewPager.PageTransformer {

        private val maxTranslateOffsetX: Int
        private var viewPager: ViewPager? = null

        init {
            this.maxTranslateOffsetX = dp2px(context, 180f)
        }

        override fun transformPage(view: View, position: Float) {
            if (viewPager == null) {
                viewPager = view.parent as ViewPager
            }

            val leftInScreen = view.left - viewPager!!.scrollX
            val centerXInViewPager = leftInScreen + view.measuredWidth / 2
            val offsetX = centerXInViewPager - viewPager!!.measuredWidth / 2
            val offsetRate = offsetX.toFloat() * 0.38f / viewPager!!.measuredWidth
            val scaleFactor = 1 - Math.abs(offsetRate)

            if (scaleFactor > 0) {
                view.scaleX = scaleFactor
                view.scaleY = scaleFactor
                view.translationX = -maxTranslateOffsetX * offsetRate
            }

            ViewCompat.setElevation(view, scaleFactor)
        }

        private fun dp2px(context: Context, dipValue: Float): Int {
            val m = context.resources.displayMetrics.density
            return (dipValue * m + 0.5f).toInt()
        }
    }

    companion object {
        const val PAGE_MARGIN = 16
        const val ARG_ID = "id"

        val BG_FILTER = PorterDuffColorFilter(0xeeffffff.toInt(), PorterDuff.Mode.SRC_OVER)
        val BG_PAINT = Paint().apply { colorFilter = BG_FILTER }

        fun newInstance(id: Int) =
            TimelineFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, id)
                }
            }
    }
}
