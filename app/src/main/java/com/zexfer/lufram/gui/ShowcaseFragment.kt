package com.zexfer.lufram.gui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import com.zexfer.lufram.Lufram
import com.zexfer.lufram.R
import com.zexfer.lufram.adapters.ShowcaseViewAdapter
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.databinding.FragmentShowcaseBinding
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
 *
 * <p>
 * You can supply a drawable resource id as the "Nothing Here" icon.
 */
class ShowcaseFragment : Fragment() {

    /** Adapter for recycler-view */
    private var showcaseViewAdapter: ShowcaseViewAdapter? = null

    /** View-binding for the layout */
    private lateinit var viewBinding: FragmentShowcaseBinding

    /** Live-data provider for wallpaper collection(s) */
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
    ): View? {
        viewBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_showcase,
            container,
            false
        )
        showcaseViewAdapter = ShowcaseViewAdapter(
            inflater,
            if (showcaseProvider != null) {
                showcaseProvider!!.editorNavAction
            } else {
                R.id.action_mainFragment_to_WCEditorFragment
            }
        )

        // Set the nothing here icon, of course!
        if (arguments != null && arguments!!.getInt(ARG_NOTHING_HERE_ICON, -1) != -1)
            viewBinding.imageNothingHere.setImageResource(arguments!!.getInt(ARG_NOTHING_HERE_ICON))

        // Initialize RecyclerView properties
        viewBinding.rvRoot.adapter = showcaseViewAdapter
        viewBinding.rvRoot.isVerticalScrollBarEnabled = true
        if (Lufram.instance.defaultPrefs.getString("preview_layout", "Grid") == "List") {
            viewBinding.rvRoot.setPadding(0, 0, 0, 240)
        }
        when (Lufram.instance.defaultPrefs.getString("preview_layout", "Grid")) {
            "Cards", "List" -> {
                viewBinding.rvRoot.layoutManager = LinearLayoutManager(context)
            }
            "Grid" -> {
                viewBinding.rvRoot.layoutManager = StaggeredGridLayoutManager(2, VERTICAL)
            }
        }

        // If is saved-instance, no need to observe() again
        if (savedInstanceState !== null) {
            //  return viewBinding.root
        }

        // Observes the wallpaper-collection list live-data.
        val showcaseObserver = Observer<List<WallpaperCollection>> {
            if (it.isEmpty()) {
                viewBinding.frameNothingHere.visibility = View.VISIBLE
            } else {
                viewBinding.frameNothingHere.visibility = View.GONE
            }

            showcaseViewAdapter!!.submitList(ArrayList(it))
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

        return viewBinding.root
    }

    override fun onResume() {
        super.onResume()
        showcaseViewAdapter?.enableShowcaseAnimation()
    }

    override fun onPause() {
        super.onPause()
        showcaseViewAdapter?.disableShowcaseAnimation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showcaseViewAdapter = null
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

        /**
         * Navigation action to reach editor from showcase.
         */
        val editorNavAction: Int
    }

    companion object {
        private const val ARG_NOTHING_HERE_ICON = "nothing_here_icon_id"

        /**
         * Creates a showcase-fragment with a custom "nothing here"
         * icon.
         *
         * @param nothingHereDrawableResourceId - resource id for the icon
         */
        fun newInstance(nothingHereDrawableResourceId: Int) =
            ShowcaseFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_NOTHING_HERE_ICON, nothingHereDrawableResourceId)
                }
            }
    }
}