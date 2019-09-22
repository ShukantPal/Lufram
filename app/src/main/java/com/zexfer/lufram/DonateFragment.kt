package com.zexfer.lufram


import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.zexfer.lufram.billing.IabHelper
import com.zexfer.lufram.billing.IabResult
import com.zexfer.lufram.billing.Purchase

class DonateFragment : Fragment(), View.OnClickListener,
    IabHelper.OnIabPurchaseFinishedListener,
    IabHelper.OnConsumeFinishedListener {

    private lateinit var entryDonateSmall: View
    private lateinit var entryDonate: View
    private lateinit var entryDonateMedium: View
    private lateinit var entryDonateLarge: View

    // TODO: Proper dialogs inventory consumption

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
            R.id.entry_donate_small -> donate(SKU_DONATION_SMALL)
            R.id.entry_donate -> donate(SKU_DONATION)
            R.id.entry_donate_medium -> donate(SKU_DONATION_MEDIUM)
            R.id.entry_donate_large -> donate(SKU_DONATION_LARGE)
            R.id.button_patreon_profile ->
                startActivity(
                    Intent(
                        ACTION_VIEW,
                        Uri.parse("https://www.patreon.com/user?u=21071869")
                    )
                )
        }
    }

    private fun donate(sku: String) {
        try {
            Lufram.instance.iabHelper
                .launchPurchaseFlow(activity, sku, 1001, this, "")
        } catch (e: IabHelper.IabAsyncInProgressException) {

        }
    }

    override fun onIabPurchaseFinished(result: IabResult?, info: Purchase?) {
        if (this.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {

        }

        if (result!!.isSuccess) {
            try {
                Lufram.instance.iabHelper.consumeAsync(info, this)
            } catch (e: IabHelper.IabAsyncInProgressException) {
                Log.e("Lufram", "Could not consume")
            }
        }
    }

    override fun onConsumeFinished(purchase: Purchase?, result: IabResult?) {

    }

    companion object {
        const val SKU_DONATION_SMALL = "donation_small"
        const val SKU_DONATION = "donation"
        const val SKU_DONATION_MEDIUM = "donation_medium"
        const val SKU_DONATION_LARGE = "donation_large"
    }

}
