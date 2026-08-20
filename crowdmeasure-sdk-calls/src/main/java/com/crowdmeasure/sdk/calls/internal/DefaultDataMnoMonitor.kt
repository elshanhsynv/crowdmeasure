@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.crowdmeasure.sdk.calls.internal

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

/** Observes a default/active data subscription change; sampling still rechecks before every write. */
@Suppress("DEPRECATION")
internal class DefaultDataMnoMonitor(
    private val context: Context,
    private val onChanged: () -> Unit,
) {
    private val telephony = context.getSystemService(TelephonyManager::class.java)
    private var legacyListener: PhoneStateListener? = null
    private var callback: TelephonyCallback? = null

    @Suppress("DEPRECATION")
    fun start() {
        val manager = telephony ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val registeredCallback = object : TelephonyCallback(),
                    TelephonyCallback.ActiveDataSubscriptionIdListener {
                    override fun onActiveDataSubscriptionIdChanged(subId: Int) = onChanged()
                }
                manager.registerTelephonyCallback(context.mainExecutor, registeredCallback)
                callback = registeredCallback
            } else {
                val registeredListener = object : PhoneStateListener(context.mainExecutor) {
                    override fun onActiveDataSubscriptionIdChanged(subId: Int) = onChanged()
                }
                manager.listen(
                    registeredListener,
                    PhoneStateListener.LISTEN_ACTIVE_DATA_SUBSCRIPTION_ID_CHANGE,
                )
                legacyListener = registeredListener
            }
        }
    }

    @Suppress("DEPRECATION")
    fun stop() {
        val manager = telephony ?: return
        callback?.let { registered -> runCatching { manager.unregisterTelephonyCallback(registered) } }
        callback = null
        legacyListener?.let { registered -> runCatching { manager.listen(registered, PhoneStateListener.LISTEN_NONE) } }
        legacyListener = null
    }
}
