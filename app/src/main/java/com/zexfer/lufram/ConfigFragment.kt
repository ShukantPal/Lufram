package com.zexfer.lufram

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_DAY_RANGE
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_INTERVAL_MILLIS
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_RANDOMIZE_ORDER
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_TIMEZONE_ADJUSTED_ENABLED

class ConfigFragment : Fragment(), MaterialButtonToggleGroup.OnButtonCheckedListener {

    private var tgMode: MaterialButtonToggleGroup? = null
    private var modePager: ViewPager? = null

    private var mode: Int = MODE_PERIODIC
    private var periodicConfig: PeriodicConfig? = null
    private var dynamicConfig: DynamicConfig? = null

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
                periodicConfig = PeriodicConfig(
                    savedInstanceState.getLong("periodic_cfg_interval_millis"),
                    savedInstanceState.getBoolean("periodic_cfg_randomize_order")
                )

                dynamicConfig = DynamicConfig(
                    savedInstanceState.getInt("dynamic_cfg_day_range"),
                    savedInstanceState.getBoolean("dynamic_cfg_time_zone_adjust_enabled")
                )
            } else {
                val prefs = LuframRepository.luframPrefs

                periodicConfig = PeriodicConfig(
                    prefs.getLong(PREF_CONFIG_INTERVAL_MILLIS, 3600000),
                    prefs.getBoolean(PREF_CONFIG_RANDOMIZE_ORDER, false)
                )

                dynamicConfig = DynamicConfig(
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
        Log.d("Lufram", "chcedkcalled ${checkedId} and ${R.id.mbtn_mode_periodic} but ${isChecked}")
        if (isChecked) when (checkedId) {
            R.id.mbtn_mode_periodic -> {
                modePager!!.currentItem = 0
            }
            R.id.mbtn_mode_dynamic -> {
                modePager!!.currentItem = 1
            }
        }
    }


    data class PeriodicConfig(
        var intervalMillis: Long,
        var randomizeOrder: Boolean
    )

    data class DynamicConfig(
        var dayRange: Int,
        var timeZoneAdjustEnabled: Boolean
    )

    class ModeAdapter(
        private val inflater: LayoutInflater,
        private val periodicConfig: PeriodicConfig,
        private val dynamicConfig: DynamicConfig
    ) : PagerAdapter() {
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            when (position) {
                0 -> {
                    return inflater.inflate(
                        R.layout.layout_config_periodic,
                        container,
                        false
                    ).also {
                        container.addView(it)
                    }
                }
                1 -> {
                    return inflater.inflate(
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

        override fun isViewFromObject(view: View, `object`: Any) = view === `object`

        override fun getCount() = 2
    }

    companion object {
        val MODE_PERIODIC = 3001
        val MODE_DYNAMIC = 3002
    }
}