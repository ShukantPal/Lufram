package com.zexfer.lufram

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment

/**
 * A simple [Fragment] subclass.
 */
class SelectIntervalDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (targetFragment == null) {
            throw IllegalStateException("Target fragment cannot be null")
        }

        return Dialog(context!!).also { dialog ->
            dialog.setContentView(R.layout.fragment_select_interval_dialog)

            val hrPicker = dialog.findViewById<NumberPicker>(R.id.picker_hr).apply {
                minValue = 0
                maxValue = 24
                value = arguments!!.getInt(ARG_HR, 0)
            }

            val minPicker = dialog.findViewById<NumberPicker>(R.id.picker_min).apply {
                minValue = 1
                maxValue = 59
                value = arguments!!.getInt(ARG_MIN, 30)
            }

            dialog.findViewById<Button>(R.id.btn_done).setOnClickListener {
                (this.targetFragment as OnIntervalSelectedListener)
                    .onIntervalSelected(hrPicker.value, minPicker.value)
                dismiss()
            }
        }
    }

    interface OnIntervalSelectedListener {
        fun onIntervalSelected(hr: Int, min: Int)
    }

    companion object {
        val ARG_HR = "interval_hr"
        val ARG_MIN = "interval_min"

        fun newInstance(hr: Int, min: Int) =
            SelectIntervalDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_HR, hr)
                    putInt(ARG_MIN, min)
                }
            }
    }
}
