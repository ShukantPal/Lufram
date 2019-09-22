package com.zexfer.lufram.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.zexfer.lufram.expanders.Expander

class WallpaperPreviewAdapter(
    private val context: Context,
    private val expander: Expander
) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return ImageView(context).apply {
            expander.load(context, position) {
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
        expander.size
}