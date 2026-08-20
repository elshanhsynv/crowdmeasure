package com.crowdmeasure.sdk.internal

import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.crowdmeasure.sdk.DefaultDataMnoEligibility
import com.crowdmeasure.sdk.DefaultDataMnoEligibilityState

internal class DefaultDataMnoEligibilityEvaluator(
    private val context: Context,
    requiredMnoId: String?,
) {
    private val requiredMnoId = requiredMnoId?.trim()

    fun isRestricted(): Boolean = requiredMnoId != null

    fun evaluate(): DefaultDataMnoEligibility {
        val required = requiredMnoId ?: return DefaultDataMnoEligibility()
        if (!hasPhoneStatePermission()) return unavailable(required)

        val subscriptions = context.getSystemService(SubscriptionManager::class.java)
            ?: return unavailable(required)
        val defaultSubscriptionId = runCatching {
            SubscriptionManager.getDefaultDataSubscriptionId()
        }.getOrNull()?.takeIf(SubscriptionManager::isValidSubscriptionId)
            ?: return unavailable(required)
        val subscription = runCatching {
            subscriptions.activeSubscriptionInfoList.orEmpty()
                .firstOrNull { it.subscriptionId == defaultSubscriptionId }
        }.getOrNull() ?: return unavailable(required)

        val homeMnoId = listOfNotNull(subscription.mccString, subscription.mncString)
            .takeIf { it.size == 2 }
            ?.joinToString(separator = "")
            ?: context.getSystemService(TelephonyManager::class.java)
                ?.let { manager -> runCatching { manager.createForSubscriptionId(defaultSubscriptionId).simOperator }.getOrNull() }
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            ?: return unavailable(required)

        return classifyDefaultDataMnoEligibility(required, homeMnoId)
    }

    private fun hasPhoneStatePermission() =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED

    private fun unavailable(required: String) = DefaultDataMnoEligibility(
        state = DefaultDataMnoEligibilityState.UNAVAILABLE,
        requiredMnoId = required,
    )
}

internal fun classifyDefaultDataMnoEligibility(
    requiredMnoId: String?,
    defaultDataMnoId: String?,
): DefaultDataMnoEligibility {
    val required = requiredMnoId?.trim()?.takeIf { it.isNotEmpty() }
        ?: return DefaultDataMnoEligibility()
    val resolved = defaultDataMnoId?.trim()?.takeIf { it.isNotEmpty() }
        ?: return DefaultDataMnoEligibility(
            state = DefaultDataMnoEligibilityState.UNAVAILABLE,
            requiredMnoId = required,
        )
    return DefaultDataMnoEligibility(
        state = if (resolved == required) {
            DefaultDataMnoEligibilityState.MATCHED
        } else {
            DefaultDataMnoEligibilityState.MISMATCHED
        },
        requiredMnoId = required,
        defaultDataMnoId = resolved,
    )
}
