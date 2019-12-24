package com.zexfer.lufram.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.zexfer.lufram.expanders.Expander

class WallpaperPreviewAdapter(
    private val context: Context,
    private val expander: Expander,
    initialBase: Int = 0,
    private val clickListener: View.OnClickListener? = null
) : PagerAdapter() {

    var base: Int = initialBase
        set(value) {
            field = value
            this.notifyDataSetChanged()
        }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return ImageView(context).apply {
            expander.load(context, (position + base) % expander.size) {
                setImageBitmap(it)
            }

            adjustViewBounds = true
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            if (clickListener !== null) {
                isClickable = true
                setOnClickListener(clickListener)
            }
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