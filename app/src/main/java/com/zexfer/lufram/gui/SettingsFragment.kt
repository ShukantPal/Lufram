package com.zexfer.lufram.gui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.zexfer.lufram.R

/**
 * Fragment that allows the user to edit preferences.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_settings, rootKey)
    }
}
