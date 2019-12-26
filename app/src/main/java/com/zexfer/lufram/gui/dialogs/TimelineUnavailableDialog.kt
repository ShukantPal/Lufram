package com.zexfer.lufram.gui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

/**
 * Fragment that displays why a timeline is unavailable. Use the static
 * factory method {@code TimelineUnavailableDialog.newInstance} to construct
 * this fragment.
 */
class TimelineUnavailableDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(context)
            .setTitle("Timeline Unavailable")
            .setMessage(arguments!!.getString(ARG_REASON))
            .setPositiveButton("Okay") { dialog, _ -> dialog.dismiss() }
            .create()

    companion object {
        const val ARG_REASON = "reason"

        /**
         * Create a {@code TimelineUnavailableDialog} using this method.
         *
         * @param reason - the reason why a timeline is unavailable
         */
        fun newInstance(reason: String): TimelineUnavailableDialog =
            TimelineUnavailableDialog().apply {
                arguments = Bundle().apply { putString(ARG_REASON, reason) }
            }
    }
}