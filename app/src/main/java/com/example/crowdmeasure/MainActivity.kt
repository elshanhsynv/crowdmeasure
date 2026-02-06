package com.example.crowdmeasure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.presentation.nav.AppNav
import com.example.crowdmeasure.presentation.screens.consent.ConsentGateScreen
import com.example.crowdmeasure.presentation.screens.consent.ConsentGateViewModel
import com.example.crowdmeasure.presentation.ui.theme.CrowdMeasureTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            CrowdMeasureTheme {
                MainContent()
            }
        }
    }
}

@Composable
private fun MainContent() {
    val consentViewModel: ConsentGateViewModel = hiltViewModel<ConsentGateViewModel>()
    val settings by consentViewModel.settings.collectAsStateWithLifecycle()

    var userDismissedConsent by rememberSaveable { mutableStateOf(false) }

    val shouldShowConsent = when {
        userDismissedConsent -> false
        settings == null -> false
        settings?.consentGateDismissed == true -> false
        settings?.consentAccepted != true || !settings!!.collectionEnabled -> true
        else -> false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppNav()
        ConsentGateScreen(
            visible = shouldShowConsent,
            onComplete = {
                consentViewModel.markConsentGateCompleted()
            },
            onDismiss = {
                userDismissedConsent = true
            },
            viewModel = consentViewModel
        )
    }
}