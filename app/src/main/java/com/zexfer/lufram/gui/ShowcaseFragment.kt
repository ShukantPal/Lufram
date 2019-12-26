package com.zexfer.lufram.gui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import com.zexfer.lufram.Lufram
import com.zexfer.lufram.R
import com.zexfer.lufram.adapters.WCListAdapter
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.WallpaperCollection
import java.util.*

/**
 * <p>
 * Fragment that displays a "library" of wallpaper-collections. Its parent
 * fragment or context can supply a list of {@code WallpaperCollection} by
 * implementing {@code ShowcaseFragment.ShowcaseProvider}. By default, however,
 * it will display all the locally-stored wallpaper collections, sorted by
 * their {@code lastUpdaterId}.
 *
 * <p>
 * {@code ShowcaseFragment} automatically adjusts its layout based on the
 * user's preference (via Lufram's default shared-prefs); however, it can
 * be instantiated with arguments to override those settings.
 */
class ShowcaseFragment : Fragment() {

    private var rvRoot: RecyclerView? = null
    private var frameNothingHere: View? = null
    private var wcListAdapter: WCListAdapter? = null

    private var showcaseProvider: ShowcaseProvider? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (parentFragment is ShowcaseProvider) {
            showcaseProvider = parentFragment as ShowcaseProvider
        } else if (context is ShowcaseProvider) {
            showcaseProvider = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_wc_preview, container, false).also {
            rvRoot = it.findViewById(R.id.rv_root)
            frameNothingHere = it.findViewById(R.id.frame_none)
            wcListAdapter =
                WCListAdapter(inflater)

            rvRoot!!.adapter = wcListAdapter
            rvRoot!!.isVerticalScrollBarEnabled = false
            if (Lufram.instance.defaultPrefs.getString("preview_layout", "Grid") == "List") {
                rvRoot!!.setPadding(0, 0, 0, 240)
            }

            when (Lufram.instance.defaultPrefs.getString("preview_layout", "Grid")) {
                "Cards", "List" -> rvRoot!!.layoutManager = LinearLayoutManager(context)
                "Grid" -> rvRoot!!.layoutManager = StaggeredGridLayoutManager(2, VERTICAL)
            }

            if (savedInstanceState !== null) {
                return@also
            }

            // Observes the wallpaper-collection list live-data.
            val showcaseObserver = Observer<List<WallpaperCollection>> {
                if (it.isEmpty()) {
                    frameNothingHere!!.visibility = View.VISIBLE
                } else {
                    frameNothingHere!!.visibility = View.GONE
                }

                wcListAdapter!!.submitList(ArrayList(it))
            }

            if (showcaseProvider == null) {
                // Default behaviour: show local library sorted by lastUpdaterId
                LuframDatabase.instance
                    .wcDao()
                    .allSorted()
                    .observe(this, showcaseObserver)
            } else {
                // Observe supplied library
                showcaseProvider!!.onShowcaseRequired()
                    .observe(this, showcaseObserver)
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        rvRoot = null
        frameNothingHere = null
        wcListAdapter = null
    }

    override fun onDetach() {
        super.onDetach()

        showcaseProvider = null
    }

    /**
     * Interface to which parent-fragment/context/activity must comply to
     * showcase a custom library of wallpaper-collections.
     */
    interface ShowcaseProvider {
        /**
         * This must return a live-data object that updates its self
         * according to the collections to be displayed.
         *
         * NOTE: This function should not return different {@code LiveData}
         * objects at different times.
         */
        fun onShowcaseRequired(): LiveData<List<WallpaperCollection>>
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