package com.example.crowdmeasure.update

object UpdateNotificationPolicy {
    fun shouldNotify(availableVersionCode: Int, lastNotifiedVersionCode: Int): Boolean =
        availableVersionCode > 0 && availableVersionCode != lastNotifiedVersionCode
}
