package com.zexfer.lufram

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.zexfer.lufram.gui.ConfigFragment
import com.zexfer.lufram.gui.ShowcaseFragment
import com.zexfer.lufram.gui.dialogs.TimelineUnavailableDialog

class MainFragment : Fragment(), View.OnClickListener, OnPageChangeListener {

    private var tabsPager: ViewPager? = null
    private var tabsLayout: TabLayout? = null
    private var fabMain: FloatingActionButton? = null

    private var fabShown: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_main, container, false).also {
            tabsPager = it.findViewById<ViewPager>(R.id.main_pager).also { pager ->
                pager.addOnPageChangeListener(this)
            }
            fabMain = it.findViewById<FloatingActionButton>(R.id.fab_main)
                .also { fab ->
                    fab.setOnClickListener(this)
                    fab.animate()
                }

            tabsPager!!.adapter = MainTabsAdapter(childFragmentManager)
            tabsPager!!.currentItem = 1
            fabShown = true

            setHasOptionsMenu(true)
        }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_toolbar, menu)
    }

    override fun onStart() {
        super.onStart()
        tabsLayout = activity!!.findViewById(R.id.tabs)
        tabsLayout!!.setupWithViewPager(tabsPager)
        tabsLayout!!.visibility = View.VISIBLE

        if (!fabShown) {
            fabMain!!.hide()
        }
    }

    override fun onStop() {
        super.onStop()
        tabsLayout!!.visibility = View.GONE
        tabsLayout = null
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.fab_main -> {
                val fragPos = tabsPager!!.currentItem

                if (fragPos == FRAG_LIBRARY) {
                    Navigation.findNavController(activity!!, R.id.nav_host_fragment)
                        .navigate(R.id.action_mainFragment_to_WCEditorFragment)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.option_search -> {
                findNavController().navigate(R.id.action_mainFragment_to_searchFragment)
            }
            R.id.option_timeline -> {
                if (LuframRepository.isRandomized) {
                    fragmentManager!!.beginTransaction()
                        .add(
                            TimelineUnavailableDialog.newInstance(TL_UNAVAILABLE_RANDOM),
                            "frag_timeline_unavailable"
                        )
                        .commitNow()
                    return true
                }

                findNavController().navigate(
                    R.id.action_mainFragment_to_timelineFragment,
                    Bundle().apply {
                        putInt("id", LuframRepository.preferredWallpaperId())
                    }
                )
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageScrollStateChanged(state: Int) {
        when (state) {
            SCROLL_STATE_IDLE -> {
                fabMain!!.apply { if (fabShown) show() else hide() }
            }
            SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING -> {
                fabMain!!.hide()
            }
        }
    }

    override fun onPageSelected(position: Int) {
        fabShown = (position == FRAG_LIBRARY)
    }

    class MainTabsAdapter(fm: FragmentManager) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            when (position) {
                FRAG_CONFIG -> return ConfigFragment()
                FRAG_LIBRARY -> return ShowcaseFragment()
                else -> throw IllegalArgumentException("position is invalid!")
            }
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                FRAG_CONFIG -> return "CONFIG"
                FRAG_LIBRARY -> return "LIBRARY"
            }

            return super.getPageTitle(position)
        }
    }

    companion object {
        val FRAG_CONFIG = 0
        val FRAG_LIBRARY = 1

        const val TL_UNAVAILABLE_RANDOM =
            "A timeline cannot be shown because you have set wallpapers to be updated in a randomized order."
    }
}