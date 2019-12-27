package com.zexfer.lufram.gui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_DAY_RANGE
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_INTERVAL_MILLIS
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_RANDOMIZE_ORDER
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_TIMEZONE_ADJUSTED_ENABLED
import com.zexfer.lufram.Lufram.Companion.PREF_CONFIG_TYPE
import com.zexfer.lufram.LuframRepository
import com.zexfer.lufram.LuframRepository.CONFIG_PERIODIC
import com.zexfer.lufram.LuframRepository.DynamicConfig
import com.zexfer.lufram.LuframRepository.PeriodicConfig
import com.zexfer.lufram.R
import com.zexfer.lufram.databinding.FragmentConfigEditorBinding
import com.zexfer.lufram.gui.dialogs.SelectIntervalDialog

/**
 * Fragment that allows editing of the periodic/dynamic configuration
 * settings.
 */
class ConfigEditorFragment : Fragment(), View.OnClickListener,
    SelectIntervalDialog.OnIntervalSelectedListener {

    /** Manages the current mode-page */
    private var modeFrameBridge: ConfigAdapterBridge? = null

    /** View-binding for the layout. */
    private lateinit var viewBinding: FragmentConfigEditorBinding

    /** Currently visible/active mode. */
    private var mode: Int = MODE_PERIODIC
    /** Cache for the periodic-config settings */
    private var periodicConfig: PeriodicConfig? = null
    /** Cache for the dynamic-config settings. */
    private var dynamicConfig: DynamicConfig? = null

    @SuppressLint("NewApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_config_editor,
            container,
            false
        )

        viewBinding.cfgEntryMode.setOnClickListener(this)

        buildConfigCache()
        modeFrameBridge = ConfigAdapterBridge(
            ConfigAdapter(this, periodicConfig!!, dynamicConfig!!),
            viewBinding.modeFrame
        )
        modeTo(mode)

        return viewBinding.root
    }

    override fun onResume() {
        super.onResume()
        buildConfigCache()
        modeFrameBridge!!.configAdapter.bindConfig(periodicConfig!!, dynamicConfig!!)
    }

    override fun onPause() {
        super.onPause()
        commitConfigCache()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding.cfgEntryMode.setOnClickListener(null)
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
                        modeTo(i)
                        dialogInterface.dismiss()
                    }
                    .show()
            }
        }
    }

    override fun onIntervalSelected(hr: Int, min: Int) {
        periodicConfig!!.intervalMillis = (1000 * (hr.toLong() * 3600 + min.toLong() * 60))

        view!!.findViewById<TextView?>(R.id.text_interval)?.text =
            PeriodicConfig.formattedIntervalString(
                hr,
                min
            )
    }

    /**
     * Internally set the current mode to.
     *
     * @param value - MODE_PERIODIC or MODE_DYNAMIC
     */
    private fun modeTo(value: Int) {
        mode = value
        modeFrameBridge!!.setCurrentItem(mode)
        viewBinding.textMode.text = MODES[mode]

        commitConfigCache()
    }

    /**
     * Builds a cache of the periodic & dynamic configuration settings
     * in-memory. If the cache is already allocated, it isn't allocated again;
     * rather data is copied into it.
     */
    private fun buildConfigCache() {
        val prefs = LuframRepository.luframPrefs
        mode = prefs.getInt(PREF_CONFIG_TYPE, CONFIG_PERIODIC) - CONFIG_PERIODIC

        val intervalMillis = prefs.getLong(PREF_CONFIG_INTERVAL_MILLIS, 3600_000)
        val randomizeOrder = prefs.getBoolean(PREF_CONFIG_RANDOMIZE_ORDER, false)
        if (periodicConfig == null) {
            periodicConfig = PeriodicConfig(intervalMillis, randomizeOrder)
        } else {
            periodicConfig!!.copy(intervalMillis, randomizeOrder)
        }

        val dayRange = prefs.getInt(PREF_CONFIG_DAY_RANGE, 1)
        val tzAdjusted = prefs.getBoolean(PREF_CONFIG_TIMEZONE_ADJUSTED_ENABLED, false)
        if (dynamicConfig == null) {
            dynamicConfig = DynamicConfig(dayRange, tzAdjusted)
        } else {
            dynamicConfig!!.copy(dayRange, tzAdjusted)
        }
    }

    /**
     * Commits the configuration settings (stores them in shared-prefs).
     *
     * @return whether the committed configuration was different than the
     *  one before.
     */
    private fun commitConfigCache(): Boolean {
        val configDiff: Boolean

        if (mode == MODE_PERIODIC) {
            configDiff = LuframRepository.commitConfig(periodicConfig!!, true)
            LuframRepository.commitConfig(dynamicConfig!!, false)
        } else {
            LuframRepository.commitConfig(periodicConfig!!, false)
            configDiff = LuframRepository.commitConfig(dynamicConfig!!, true)
        }

        return configDiff
    }

    /**
     * Manages switching the view in modeFrame, like what a ViewPager2
     * would've done! Clunky bloat, yet mighty fix!
     */
    class ConfigAdapterBridge(
        val configAdapter: ConfigAdapter,
        private val frame: FrameLayout
    ) {
        private val view: MutableList<View?> = mutableListOf(null, null)// 2 modes, clunky fix

        fun setCurrentItem(i: Int) {
            if (view[i] === null) {
                view[i] = configAdapter.onCreateViewHolder(frame, configAdapter.getItemViewType(i))
                    .also { configAdapter.onBindViewHolder(it, i) }
                    .itemView
            }

            frame.removeAllViews()
            frame.addView(view[i])
        }
    }

    /*
     * ConfigAdapter was supposed to be used with a ViewPager2. However, the
     * ViewPager2 will capture swipes and not let the main fragment's ViewPager
     * get the swipe (even after disabling isUserInputEnabled). Due to this,
     * we are using a FrameLayout and setting the view manually.
     *
     * @see ConfigEditorFragment.ConfigAdapterBridge
     */
    class ConfigAdapter(
        private val targetFragment: Fragment,
        private var periodicConfig: PeriodicConfig,
        private var dynamicConfig: DynamicConfig
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (position == POSITION_PERIODIC) {
                (holder as PeriodicConfigViewHolder).bindTo(periodicConfig)
            } else {
                (holder as DynamicConfigViewHolder).bindTo(dynamicConfig)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layoutRes: Int = if (viewType == POSITION_PERIODIC) {
                R.layout.layout_config_periodic
            } else {
                R.layout.layout_config_dynamic
            }

            val rootView = targetFragment
                .layoutInflater
                .inflate(layoutRes, parent, false)
            return if (viewType == POSITION_PERIODIC) {
                PeriodicConfigViewHolder(targetFragment, rootView)
            } else {
                DynamicConfigViewHolder(rootView)
            }
        }

        override fun getItemCount(): Int = CONFIG_COUNT

        override fun getItemViewType(position: Int): Int = position

        fun bindConfig(periodic: PeriodicConfig, dynamic: DynamicConfig) {
            this.periodicConfig = periodic
            this.dynamicConfig = dynamic
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
        private var configSource: PeriodicConfig? = null

        fun bindTo(config: PeriodicConfig) {
            this.configSource = config
            textInterval.text = config.formattedIntervalString()
            switchRandomize.isChecked = config.randomizeOrder
        }

        override fun onClick(view: View?) {
            when (view?.id) {
                R.id.entry_interval -> {
                    val hr = configSource!!.hr
                    val min = configSource!!.min

                    targetFragment.fragmentManager!!.beginTransaction()
                        .add(
                            SelectIntervalDialog.newInstance(
                                hr,
                                min
                            ).also { frag ->
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

            LuframRepository.commitConfig(configSource!!)
        }
    }

    class DynamicConfigViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        fun bindTo(config: DynamicConfig) {
            // Do nothing! DynamicConfig has no implemented fields!
        }
    }

    companion object {
        const val MODE_PERIODIC = 0
        const val MODE_DYNAMIC = 1

        val MODES = arrayOf("Periodic", "Dynamic")
    }
}