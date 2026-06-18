package com.example.crowdmeasure.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateNotificationPolicyTest {
    @Test
    fun notifiesOnlyWhenVersionWasNotAlreadyNotified() {
        assertTrue(UpdateNotificationPolicy.shouldNotify(104, 103))
        assertTrue(UpdateNotificationPolicy.shouldNotify(104, 0))
        assertFalse(UpdateNotificationPolicy.shouldNotify(104, 104))
        assertFalse(UpdateNotificationPolicy.shouldNotify(0, 104))
    }
}
