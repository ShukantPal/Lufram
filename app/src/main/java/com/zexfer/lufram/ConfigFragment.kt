package com.zexfer.lufram

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.snackbar.Snackbar
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_DAY_RANGE
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_INTERVAL_MILLIS
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_RANDOMIZE_ORDER
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_TIMEZONE_ADJUSTED_ENABLED

class ConfigFragment : Fragment(), MaterialButtonToggleGroup.OnButtonCheckedListener {

    private var tgMode: MaterialButtonToggleGroup? = null
    private var modePager: ViewPager? = null

    private var mode: Int = MODE_PERIODIC
    private var periodicConfig: LuframRepository.PeriodicConfig? = null
    private var dynamicConfig: LuframRepository.DynamicConfig? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_config, container, false).also {
            tgMode = it.findViewById<MaterialButtonToggleGroup>(R.id.tg_mode)
                .also { tgrp -> tgrp.addOnButtonCheckedListener(this) }
            modePager = it.findViewById(R.id.mode_pager)

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

                periodicConfig = LuframRepository.PeriodicConfig(
                    prefs.getLong(PREF_CONFIG_INTERVAL_MILLIS, 3600000),
                    prefs.getBoolean(PREF_CONFIG_RANDOMIZE_ORDER, false)
                )

                dynamicConfig = LuframRepository.DynamicConfig(
                    prefs.getInt(PREF_CONFIG_DAY_RANGE, 1),
                    prefs.getBoolean(PREF_CONFIG_TIMEZONE_ADJUSTED_ENABLED, false)
                )
            }

            modePager!!.adapter = ModeAdapter(inflater, periodicConfig!!, dynamicConfig!!)
            tgMode!!.check(R.id.mbtn_mode_periodic)
        }

    override fun onButtonChecked(
        group: MaterialButtonToggleGroup?,
        checkedId: Int,
        isChecked: Boolean
    ) {
        if (isChecked) when (checkedId) {
            R.id.mbtn_mode_periodic -> {
                mode = MODE_PERIODIC
                modePager!!.currentItem = 0
            }
            R.id.mbtn_mode_dynamic -> {
                mode = MODE_DYNAMIC
                modePager!!.currentItem = 1
            }
        }
    }

    override fun onPause() {
        super.onPause()

        when (mode) {
            MODE_PERIODIC -> {
                (modePager!!.adapter as ModeAdapter).updatePeriodicConfig()
                LuframRepository.commitConfig(periodicConfig!!)
            }
            MODE_DYNAMIC -> {
                (modePager!!.adapter as ModeAdapter).updateDynamicConfig()
                LuframRepository.commitConfig(dynamicConfig!!)
            }
        }

        Snackbar.make(tgMode!!, "We've updated your configuration!", Snackbar.LENGTH_SHORT)
            .show()
    }

    class ModeAdapter(
        private val inflater: LayoutInflater,
        private val periodicConfig: LuframRepository.PeriodicConfig,
        private val dynamicConfig: LuframRepository.DynamicConfig
    ) : PagerAdapter() {
        var hourPicker: NumberPicker? = null
        var minPicker: NumberPicker? = null
        var checkRandOrder: CheckBox? = null

        var daysPicker: NumberPicker? = null
        var checkAdjustTimezoneEnabled: CheckBox? = null

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            when (position) {
                0 -> {
                    return inflater.inflate(
                        R.layout.layout_config_periodic,
                        container,
                        false
                    ).also {
                        hourPicker = it.findViewById(R.id.picker_interval_hr)
                        minPicker = it.findViewById(R.id.picker_interval_min)
                        checkRandOrder = it.findViewById(R.id.checkbox_randomize_order)

                        hourPicker!!.minValue = 0
                        hourPicker!!.maxValue = 24
                        minPicker!!.minValue = 1
                        minPicker!!.maxValue = 59

                        hourPicker!!.value = (periodicConfig.intervalMillis / 3600000).toInt()
                        minPicker!!.value =
                            (periodicConfig.intervalMillis % 3600000).toInt() / 60000
                        checkRandOrder!!.isChecked = periodicConfig.randomizeOrder

                        container.addView(it)
                    }
                }
                1 -> {
                    return inflater.inflate(
                        R.layout.layout_config_dynamic,
                        container,
                        false
                    ).also {
                        daysPicker = it.findViewById(R.id.picker_interval_days)
                        checkAdjustTimezoneEnabled =
                            it.findViewById(R.id.checkbox_adjust_timezone_enabled)

                        daysPicker!!.minValue = 1
                        daysPicker!!.maxValue = 7

                        daysPicker!!.value = dynamicConfig.dayRange
                        checkAdjustTimezoneEnabled!!.isChecked = dynamicConfig.timeZoneAdjustEnabled

                        container.addView(it)
                    }
                }
            }

            return super.instantiateItem(container, position)
        }

        fun updatePeriodicConfig() {
            if (hourPicker != null) {
                periodicConfig.apply {
                    intervalMillis = ((
                            (hourPicker!!.value * 3600) + (minPicker!!.value * 60)
                            ) * 1000).toLong()
                    randomizeOrder = checkRandOrder!!.isChecked
                }
            } else {
                Log.w("Lufram", "Could not update periodic config! Falling back!")
            }
        }

        fun updateDynamicConfig() {
            if (daysPicker != null) {
                dynamicConfig.apply {
                    dayRange = daysPicker!!.value
                    timeZoneAdjustEnabled = checkAdjustTimezoneEnabled!!.isChecked
                }
            } else {
                Log.w("Lufram", "Could not update dynamic config! Falling back!")
            }
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            super.destroyItem(container, position, `object`)

            when (position) {
                0 -> {
                    hourPicker = null
                    minPicker = null
                    checkRandOrder = null
                }
                1 -> {
                    daysPicker = null
                    checkAdjustTimezoneEnabled = null
                }
            }
        }

        override fun isViewFromObject(view: View, `object`: Any) = view === `object`

        override fun getCount() = 2
    }

    companion object {
        val MODE_PERIODIC = 3001
        val MODE_DYNAMIC = 3002
    }
}