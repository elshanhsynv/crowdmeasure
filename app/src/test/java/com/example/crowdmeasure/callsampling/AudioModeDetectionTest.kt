package com.example.crowdmeasure.callsampling

import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioModeDetectionTest {
    @Test
    fun communicationMode_isDetectedAsVoip() {
        assertTrue(AudioManager.MODE_IN_COMMUNICATION.isVoipCommunicationMode())
    }

    @Test
    fun regularAudioModes_areNotDetectedAsVoip() {
        assertFalse(AudioManager.MODE_NORMAL.isVoipCommunicationMode())
        assertFalse(AudioManager.MODE_RINGTONE.isVoipCommunicationMode())
        assertFalse(AudioManager.MODE_IN_CALL.isVoipCommunicationMode())
    }
}
