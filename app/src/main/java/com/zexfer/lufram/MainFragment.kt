package com.zexfer.lufram

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.navigation.Navigation
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

class MainFragment : Fragment(), View.OnClickListener, ViewPager.OnPageChangeListener {

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
            tabsPager = it.findViewById<ViewPager>(R.id.main_pager)
                .also { pager -> pager.addOnPageChangeListener(this) }
            fabMain = it.findViewById<FloatingActionButton>(R.id.fab_main)
                .also { fab -> fab.setOnClickListener(this) }

            tabsPager!!.adapter = MainTabsAdapter(childFragmentManager)
            fabShown = true
        }

    override fun onStart() {
        super.onStart()
        tabsLayout = activity!!.findViewById(R.id.tabs)
        tabsLayout!!.setupWithViewPager(tabsPager)
        tabsLayout!!.tabGravity = TabLayout.GRAVITY_CENTER
        tabsLayout!!.visibility = View.VISIBLE
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
                FRAG_LIBRARY -> return WCPreviewFragment()
                FRAG_CONFIG -> return ConfigFragment()
                else -> throw IllegalArgumentException("position is invalid!")
            }
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return "LIBRARY"
                1 -> return "CONFIG"
            }

            return super.getPageTitle(position)
        }
    }

    companion object {
        val FRAG_LIBRARY = 0
        val FRAG_CONFIG = 1
    }
}