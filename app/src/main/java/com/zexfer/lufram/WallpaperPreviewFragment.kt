package com.zexfer.lufram


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Generic wallpaper preview fragment that allows wallpaper type-specific
 * customization.
 */
open class WallpaperPreviewFragment<WallpaperType> : Fragment() {

    private var rvRoot: RecyclerView? = null

    private var textNone: TextView? = null

    private var listProvider: WallpaperListProvider? = null

    protected var adapterId: Int = -1

    protected open var listAdapter: ListAdapter<WallpaperType, *>? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (listAdapter === null) {
            if (context is ListAdapter<*, *>) {
                listAdapter = context as ListAdapter<WallpaperType, *>
            }
        }

        if (context is WallpaperListProvider) {
            listProvider = context
        } else {
            throw IllegalArgumentException(
                "Your activity must implement " +
                        "WallpaperPreviewFragment.WallpaperListProvider"
            )
        }
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (listAdapter === null) {
            throw IllegalStateException(
                "Either your activity must implement " +
                        "android.widget.ListAdapter or you must extend WallpaperPreviewFragment " +
                        "and provide a custom list adapter"
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wallpaper_preview, container, false).also {
            rvRoot = it.findViewById(R.id.rv_root)
            rvRoot?.adapter = listAdapter

            textNone = it.findViewById(R.id.text_none)

            listProvider?.visibleWallpapers(adapterId)?.observe(this, object : Observer<Any> {
                override fun onChanged(t: Any?) {
                    if (!(t is List<*>)) {
                        throw AssertionError(
                            "WallpaperPreviewFragment was observing " +
                                    "a LiveData<List<WallpaperType?>> object; found unknown type"
                        )
                    }

                    listAdapter!!.submitList(t as List<WallpaperType>)

                    if (t.size == 0) {
                        textNone!!.visibility = View.VISIBLE
                    } else {
                        textNone!!.visibility = View.INVISIBLE
                    }
                }
            })
        }
    }

    override fun onDetach() {
        listProvider = null
        listAdapter = null
        super.onDetach()
    }

    interface WallpaperListProvider {

        fun visibleWallpapers(adapterId: Int): LiveData<Any>
    }

    companion object {
        @JvmStatic
        val ARG_PREVIEW_FACTORY = "Preview@ViewFactory"
    }
}
