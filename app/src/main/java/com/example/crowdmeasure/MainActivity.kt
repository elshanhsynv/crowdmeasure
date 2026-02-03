package com.example.crowdmeasure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.crowdmeasure.presentation.nav.AppNav
import com.example.crowdmeasure.presentation.screens.consent.ConsentGateDialog
import com.example.crowdmeasure.presentation.screens.consent.ConsentGateViewModel
import com.example.crowdmeasure.presentation.ui.theme.CrowdMeasureTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CrowdMeasureTheme {
                val vm: ConsentGateViewModel = hiltViewModel()
                val settings = vm.settings.collectAsState().value

                // Show gate if user hasn't opted in or collection disabled
                var gateDismissed by remember { mutableStateOf(false) }
                val shouldShowGate =
                    settings?.consentGateDismissed != true && (settings?.consentAccepted != true || !settings.collectionEnabled)

                AppNav()

                ConsentGateDialog(
                    visible = shouldShowGate,
                    onDismiss = { gateDismissed = true },
                    vm = vm
                )
            }
        }
    }
}