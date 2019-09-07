package com.zexfer.lufram

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_DAY_RANGE
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_INTERVAL_MILLIS
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_RANDOMIZE_ORDER
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_TIMEZONE_ADJUSTED_ENABLED
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_TYPE
import com.zexfer.lufram.LuframRepository.CONFIG_PERIODIC

class ConfigFragment : Fragment(), View.OnClickListener,
    SelectIntervalDialogFragment.OnIntervalSelectedListener {

    private var entryMode: LinearLayout? = null
    private var textMode: TextView? = null

    private var modePager: ViewPager? = null

    private var mode: Int = MODE_PERIODIC
    private var periodicConfig: LuframRepository.PeriodicConfig? = null
    private var dynamicConfig: LuframRepository.DynamicConfig? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_config, container, false).also {
            entryMode = it.findViewById<LinearLayout>(R.id.cfg_entry_mode)
                .also { entry -> entry.setOnClickListener(this) }
            textMode = it.findViewById(R.id.text_mode)
            modePager = it.findViewById<ViewPager>(R.id.mode_pager)
                .also { pager ->
                    pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                        override fun onPageScrolled(
                            position: Int,
                            positionOffset: Float,
                            positionOffsetPixels: Int
                        ) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                pager.releasePointerCapture()
                            }
                            pager.currentItem = mode
                        }
                    })
                }

            if (savedInstanceState !== null) {
                periodicConfig = LuframRepository.PeriodicConfig(
                    savedInstanceState.getLong("periodic_cfg_interval_millis"),
                    savedInstanceState.getBoolean("periodic_cfg_randomize_order")
                )

                dynamicConfig = LuframRepository.DynamicConfig(
                    savedInstanceState.getInt("dynamic_cfg_day_range"),
                    savedInstanceState.getBoolean("dynamic_cfg_time_zone_adjust_enabled")
                )
            } else {
                val prefs = LuframRepository.luframPrefs
                mode = prefs.getInt(PREF_CONFIG_TYPE, CONFIG_PERIODIC) - CONFIG_PERIODIC

                periodicConfig = LuframRepository.PeriodicConfig(
                    prefs.getLong(PREF_CONFIG_INTERVAL_MILLIS, 3600000),
                    prefs.getBoolean(PREF_CONFIG_RANDOMIZE_ORDER, false)
                )

                dynamicConfig = LuframRepository.DynamicConfig(
                    prefs.getInt(PREF_CONFIG_DAY_RANGE, 1),
                    prefs.getBoolean(PREF_CONFIG_TIMEZONE_ADJUSTED_ENABLED, false)
                )
            }

            modePager!!.adapter = ModeAdapter(this, periodicConfig!!, dynamicConfig!!)
            modePager!!.currentItem = mode
            textMode!!.text = MODES[mode]
        }

    override fun onPause() {
        super.onPause()

        when (mode) {
            MODE_PERIODIC -> {
                LuframRepository.commitConfig(periodicConfig!!)
            }
            MODE_DYNAMIC -> {
                LuframRepository.commitConfig(dynamicConfig!!)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        modePager!!.clearOnPageChangeListeners()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.cfg_entry_mode -> {
                AlertDialog.Builder(context)
                    .setTitle("Mode")
                    .setNegativeButton("Cancel", null)
                    .setSingleChoiceItems(
                        arrayOf("Periodic", "Dynamic"),
                        mode
                    ) { dialogInterface, i ->
                        mode = i
                        modePager!!.currentItem = mode
                        textMode!!.text = MODES[mode]
                        dialogInterface.dismiss()
                    }
                    .show()
            }
        }
    }

    override fun onIntervalSelected(hr: Int, min: Int) {
        periodicConfig!!.intervalMillis = (1000 * (hr.toLong() * 3600 + min.toLong() * 60))

        view!!.findViewById<TextView?>(R.id.text_interval)?.text =
            "${hr} : ${min}"
    }

    class ModeAdapter(
        private val targetFragment: Fragment,
        private val periodicConfig: LuframRepository.PeriodicConfig,
        private val dynamicConfig: LuframRepository.DynamicConfig
    ) : PagerAdapter() {
        private var entryInterval: LinearLayout? = null
        private var entryRandomize: Switch? = null

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            when (position) {
                0 -> {
                    return targetFragment.layoutInflater.inflate(
                        R.layout.layout_config_periodic,
                        container,
                        false
                    ).also {
                        val hr = (periodicConfig.intervalMillis / 3600000).toInt()
                        val min = ((periodicConfig.intervalMillis % 3600000) / 60000).toInt()
                        entryInterval = it.findViewById<LinearLayout>(R.id.entry_interval).also {
                            it.setOnClickListener { view ->
                                targetFragment.fragmentManager!!.beginTransaction()
                                    .add(
                                        SelectIntervalDialogFragment.newInstance(
                                            hr,
                                            min
                                        ).also { frag ->
                                            frag.setTargetFragment(targetFragment, 1337)
                                        },
                                        "SelectIntervalDialogFragment"
                                    )
                                    .commit()
                            }
                        }

                        it.findViewById<TextView>(R.id.text_interval).text = "${hr} : ${min}"

                        entryRandomize = it.findViewById<Switch>(R.id.entry_randomize).also {
                            it.setOnClickListener { view ->
                                periodicConfig.randomizeOrder = entryRandomize!!.isChecked
                            }

                            it.isChecked = periodicConfig.randomizeOrder
                        }
                        container.addView(it)
                    }
                }
                1 -> {
                    return targetFragment.layoutInflater.inflate(
                        R.layout.layout_config_dynamic,
                        container,
                        false
                    ).also {

                        container.addView(it)
                    }
                }
            }

            return super.instantiateItem(container, position)
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            super.destroyItem(container, position, `object`)

            when (position) {
                0 -> {
                    entryInterval = null
                    entryRandomize = null
                }
            }
        }

        override fun isViewFromObject(view: View, `object`: Any) = view === `object`

        override fun getCount() = 2
    }

    companion object {
        val MODE_PERIODIC = 0
        val MODE_DYNAMIC = 1

        val MODES = arrayOf("Periodic", "Dynamic")
    }
}