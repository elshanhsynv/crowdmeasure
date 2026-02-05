package com.example.crowdmeasure.data.prefs

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.dataStore by preferencesDataStore(name = "crowdmeasure_prefs")
