package com.zexfer.lufram.gui

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.zexfer.lufram.R

/**
 * Fragment that displays information about this app & its author.
 */
class AboutFragment : Fragment(), View.OnClickListener {

    // TODO: Use view-binding, maybe?

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false).also {
            it.findViewById<View>(R.id.entry_app_introduction)
                .setOnClickListener(this)
            it.findViewById<View>(R.id.entry_about_libraries)
                .setOnClickListener(this)
            it.findViewById<View>(R.id.entry_github_repo)
                .setOnClickListener(this)
            it.findViewById<View>(R.id.entry_rate)
                .setOnClickListener(this)
            it.findViewById<View>(R.id.entry_donate)
                .setOnClickListener(this)
            it.findViewById<View>(R.id.entry_report_bug)
                .setOnClickListener(this)
            it.findViewById<View>(R.id.entry_author_twitter_profile)
                .setOnClickListener(this)
        }
    }

    override fun onClick(view: View?) {
        val uri: Uri?

        when (view?.id) {
            R.id.entry_github_repo ->
                uri = Uri.parse("https://github.com/SukantPal/Lufram")
            R.id.entry_about_libraries -> {
                LibsBuilder()
                    .withActivityStyle(Libs.ActivityStyle.LIGHT)
                    .start(context!!)
                    .also { return }
            }
            R.id.entry_donate -> {
                findNavController().navigate(R.id.action_aboutFragment_to_donateFragment)
                    .also { return }
            }
            R.id.entry_report_bug ->
                uri = Uri.parse("https://github.com/SukantPal/Lufram/issues")
            R.id.entry_author_twitter_profile ->
                uri = Uri.parse("https://twitter.com/ShukantP")
            else ->
                return
        }

        startActivity(Intent(ACTION_VIEW, uri))
    }
}
