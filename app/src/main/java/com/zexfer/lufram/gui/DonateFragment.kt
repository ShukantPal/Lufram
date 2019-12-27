package com.zexfer.lufram.gui

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.zexfer.lufram.R

class DonateFragment : Fragment(), View.OnClickListener {

    private lateinit var entryDonateSmall: View
    private lateinit var entryDonate: View
    private lateinit var entryDonateMedium: View
    private lateinit var entryDonateLarge: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_donate, container, false).also {
            entryDonateSmall = it.findViewById<View>(R.id.entry_donate_small)
                .also { it.setOnClickListener(this) }
            entryDonate = it.findViewById<View>(R.id.entry_donate)
                .also { it.setOnClickListener(this) }
            entryDonateMedium = it.findViewById<View>(R.id.entry_donate_medium)
                .also { it.setOnClickListener(this) }
            entryDonateLarge = it.findViewById<View>(R.id.entry_donate_large)
                .also { it.setOnClickListener(this) }
            it.findViewById<View>(R.id.button_patreon_profile)
                .setOnClickListener(this)
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.entry_donate_small,
            R.id.entry_donate,
            R.id.entry_donate_medium,
            R.id.entry_donate_large,
            R.id.button_patreon_profile ->
                startActivity(
                    Intent(
                        ACTION_VIEW,
                        Uri.parse("https://www.patreon.com/user?u=21071869")
                    )
                )
        }
    }
}
