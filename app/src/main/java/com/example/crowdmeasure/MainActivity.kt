package com.example.crowdmeasure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.crowdmeasure.presentation.nav.AppNav
import com.example.crowdmeasure.presentation.ui.theme.CrowdMeasureTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CrowdMeasureTheme {
                AppNav()
            }
        }
    }
}