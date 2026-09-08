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
    private val requireNotRoaming: Boolean,
) {
    private val requiredMnoId = requiredMnoId?.trim()

    fun isRestricted(): Boolean = requiredMnoId != null || requireNotRoaming

    fun evaluate(): DefaultDataMnoEligibility {
        val required = requiredMnoId
        if (!isRestricted()) return DefaultDataMnoEligibility()
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

        val telephony = context.getSystemService(TelephonyManager::class.java)
            ?: return unavailable(required)
        val defaultDataTelephony = runCatching {
            telephony.createForSubscriptionId(defaultSubscriptionId)
        }.getOrElse { return unavailable(required) }
        val homeMnoId = if (required == null) {
            null
        } else {
            listOfNotNull(subscription.mccString, subscription.mncString)
                .takeIf { it.size == 2 }
                ?.joinToString(separator = "")
                ?: runCatching { defaultDataTelephony.simOperator }.getOrNull()
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                ?: return unavailable(required)
        }
        val roaming = if (requireNotRoaming) {
            runCatching { defaultDataTelephony.isNetworkRoaming }.getOrElse {
                return unavailable(required)
            }
        } else {
            null
        }

        return classifyDefaultDataMnoEligibility(
            requiredMnoId = required,
            defaultDataMnoId = homeMnoId,
            requireNotRoaming = requireNotRoaming,
            defaultDataRoaming = roaming,
        )
    }

    private fun hasPhoneStatePermission() =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED

    private fun unavailable(required: String?) = DefaultDataMnoEligibility(
        state = DefaultDataMnoEligibilityState.UNAVAILABLE,
        requiredMnoId = required,
        requireNotRoaming = requireNotRoaming,
    )
}

internal fun classifyDefaultDataMnoEligibility(
    requiredMnoId: String?,
    defaultDataMnoId: String?,
    requireNotRoaming: Boolean = false,
    defaultDataRoaming: Boolean? = null,
): DefaultDataMnoEligibility {
    val required = requiredMnoId?.trim()?.takeIf { it.isNotEmpty() }
    if (required == null && !requireNotRoaming) return DefaultDataMnoEligibility()
    if (requireNotRoaming && defaultDataRoaming == null) {
        return DefaultDataMnoEligibility(
            state = DefaultDataMnoEligibilityState.UNAVAILABLE,
            requiredMnoId = required,
            requireNotRoaming = true,
        )
    }
    if (requireNotRoaming && defaultDataRoaming == true) {
        return DefaultDataMnoEligibility(
            state = DefaultDataMnoEligibilityState.ROAMING,
            requiredMnoId = required,
            requireNotRoaming = true,
            defaultDataRoaming = true,
        )
    }
    val resolved = if (required == null) null else defaultDataMnoId?.trim()?.takeIf { it.isNotEmpty() }
    if (required != null && resolved == null) {
        return DefaultDataMnoEligibility(
            state = DefaultDataMnoEligibilityState.UNAVAILABLE,
            requiredMnoId = required,
            requireNotRoaming = requireNotRoaming,
            defaultDataRoaming = defaultDataRoaming,
        )
    }
    return DefaultDataMnoEligibility(
        state = if (required == null) {
            DefaultDataMnoEligibilityState.NOT_ROAMING
        } else if (resolved == required) {
            DefaultDataMnoEligibilityState.MATCHED
        } else {
            DefaultDataMnoEligibilityState.MISMATCHED
        },
        requiredMnoId = required,
        defaultDataMnoId = resolved,
        requireNotRoaming = requireNotRoaming,
        defaultDataRoaming = defaultDataRoaming,
    )
}
