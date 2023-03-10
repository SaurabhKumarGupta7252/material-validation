package com.android.app.OTPAutoRead

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status


class SMSBroadcastReceiver : BroadcastReceiver() {

    var smsReceiverListener: SMSBroadcastReceiverListener? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
            val extras = intent.extras
            val smsRetrieverStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as Status
            when (smsRetrieverStatus.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    val intent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                    smsReceiverListener?.onSuccess(intent)
                }
                CommonStatusCodes.TIMEOUT -> {
                    smsReceiverListener?.onFailure()
                }
            }
        }
    }

    interface SMSBroadcastReceiverListener {
        fun onSuccess(intent: Intent?)
        fun onFailure()
    }
}