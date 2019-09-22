package com.zexfer.lufram


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.zexfer.lufram.adapters.WallpaperPreviewAdapter
import com.zexfer.lufram.database.tasks.WallpaperTask
import com.zexfer.lufram.expanders.Expander

class TimelineFragment : Fragment(), View.OnClickListener {

    private var wcPager: ViewPager? = null
    private var textEstimatedHitTime: TextView? = null
    private var textEstimatedHitDay: TextView? = null
    private var btnMoreVert: AppCompatImageButton? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_timeline, container, false).also {
            wcPager = it.findViewById(R.id.wc_pager)
            textEstimatedHitTime = it.findViewById(R.id.text_estimated_hit_time)
            textEstimatedHitDay = it.findViewById(R.id.text_estimated_hit_day)
            btnMoreVert = it.findViewById(R.id.btn_more)

            WallpaperTask.run(arguments!!.getInt(ARG_ID)) { wc ->
                wcPager?.adapter = WallpaperPreviewAdapter(
                    context ?: return@run,
                    Expander.open(wc)
                )

                wcPager?.pageMargin = 16
                wcPager?.setPageTransformer(false, CarouselEffectTransformer(context!!))
                wcPager?.isHorizontalScrollBarEnabled = true

                wcPager?.postDelayed({
                    wcPager?.setCurrentItem(
                        WallpaperUpdateController.estimatedWallpaperIndex(),
                        true
                    )
                }, 100)
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        wcPager!!.setPageTransformer(false, null)
    }

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.btn_more -> {

            }
        }
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

        fun newInstance(id: Int) =
            TimelineFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, id)
                }
            }
    }
}
