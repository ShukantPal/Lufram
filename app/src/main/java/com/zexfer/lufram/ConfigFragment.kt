package com.zexfer.lufram

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
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

    private var modePager: ViewPager2? = null

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
            modePager = it.findViewById<ViewPager2>(R.id.mode_pager)
                .apply { isUserInputEnabled = false }

            updateConfigCache()
            modePager!!.adapter = ConfigAdapter(this, periodicConfig!!, dynamicConfig!!)
            modePager!!.currentItem = mode
            textMode!!.text = MODES[mode]
        }

    override fun onResume() {
        super.onResume()
        updateConfigCache()
        (modePager!!.adapter as ConfigAdapter).updateConfigs(periodicConfig!!, dynamicConfig!!)
    }

    override fun onPause() {
        super.onPause()

        val configDiff: Boolean

        when (mode) {
            MODE_PERIODIC -> {
                configDiff = LuframRepository.commitConfig(periodicConfig!!)
            }
            MODE_DYNAMIC -> {
                configDiff = LuframRepository.commitConfig(dynamicConfig!!)
            }
            else ->
                return
        }

        if (configDiff) {
            Snackbar.make(modePager!!, "We've updated your configuration", Snackbar.LENGTH_SHORT)
                .show()
        }
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

    private fun updateConfigCache() {
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

    class ConfigAdapter(
        private val targetFragment: Fragment,
        private var periodicConfig: LuframRepository.PeriodicConfig,
        private var dynamicConfig: LuframRepository.DynamicConfig
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (position) {
                POSITION_PERIODIC -> {
                    (holder as PeriodicConfigViewHolder).bindTo(periodicConfig)
                }
                POSITION_DYNAMIC -> {
                    (holder as DynamicConfigViewHolder).bindTo(dynamicConfig)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layoutRes: Int = when (viewType) {
                POSITION_PERIODIC -> R.layout.layout_config_periodic
                POSITION_DYNAMIC -> R.layout.layout_config_dynamic
                else -> throw IllegalStateException("ConfigAdapter cannot get invalid position!")
            }

            val rootView = targetFragment
                .layoutInflater
                .inflate(layoutRes, parent, false)

            return when (viewType) {
                POSITION_PERIODIC -> PeriodicConfigViewHolder(targetFragment, rootView)
                else -> DynamicConfigViewHolder(rootView) // must be exhaustive
            }
        }

        override fun getItemCount(): Int = CONFIG_COUNT

        override fun getItemViewType(position: Int): Int = position

        fun updateConfigs(
            periodicConfig: LuframRepository.PeriodicConfig,
            dynamicConfig: LuframRepository.DynamicConfig
        ) {
            this.periodicConfig = periodicConfig
            this.dynamicConfig = dynamicConfig
        }

        companion object {
            const val POSITION_PERIODIC = 0
            const val POSITION_DYNAMIC = 1

            const val CONFIG_COUNT = 2
        }
    }

    // targetFragment must implement SelectIntervalDialogFragment.OnIntervalSelectedListener
    // and should sync intervals with view R.id.text_interval in onIntervalSelected()
    class PeriodicConfigViewHolder(private val targetFragment: Fragment, rootView: View) :
        RecyclerView.ViewHolder(rootView), View.OnClickListener {

        init {
            rootView.findViewById<View>(R.id.entry_interval).setOnClickListener(this)
            rootView.findViewById<View>(R.id.entry_randomize).setOnClickListener(this)
        }

        private val textInterval: TextView = rootView.findViewById(R.id.text_interval)
        private val switchRandomize: Switch = rootView.findViewById(R.id.switch_randomize)
        private var configSource: LuframRepository.PeriodicConfig? = null

        fun bindTo(config: LuframRepository.PeriodicConfig) {
            this.configSource = config
            textInterval.text = "${config.hr} : ${config.min}"
            switchRandomize.isChecked = config.randomizeOrder
        }

        override fun onClick(view: View?) {
            when (view?.id) {
                R.id.entry_interval -> {
                    val hr = configSource!!.hr
                    val min = configSource!!.min

                    targetFragment.fragmentManager!!.beginTransaction()
                        .add(
                            SelectIntervalDialogFragment.newInstance(hr, min).also { frag ->
                                frag.setTargetFragment(targetFragment, 1337)
                            },
                            "SelectIntervalDialogFragment"
                        )
                        .commit()
                }
                R.id.entry_randomize -> {
                    switchRandomize.isChecked = !switchRandomize.isChecked
                    configSource!!.randomizeOrder = switchRandomize.isChecked
                }
            }
        }
    }

    class DynamicConfigViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        fun bindTo(config: LuframRepository.DynamicConfig) {
            // Do nothing! DynamicConfig has no implemented fields!
        }
    }

    companion object {
        const val MODE_PERIODIC = 0
        const val MODE_DYNAMIC = 1

        val MODES = arrayOf("Periodic", "Dynamic")
    }
}