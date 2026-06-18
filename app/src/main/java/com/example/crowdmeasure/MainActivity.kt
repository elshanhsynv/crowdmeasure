package com.example.crowdmeasure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.presentation.nav.AppNav
import com.example.crowdmeasure.presentation.screens.consent.ConsentGateScreen
import com.example.crowdmeasure.presentation.screens.consent.ConsentGateViewModel
import com.example.crowdmeasure.presentation.ui.theme.CrowdMeasureTheme
import com.example.crowdmeasure.update.UpdateDialog
import com.example.crowdmeasure.update.UpdateViewModel
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
    val updateViewModel: UpdateViewModel = hiltViewModel<UpdateViewModel>()
    val settings by consentViewModel.settings.collectAsStateWithLifecycle()
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()

    val shouldShowConsent =
        settings != null && !settings!!.consentGateDismissed

    LaunchedEffect(Unit) {
        updateViewModel.checkOnStartup()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppNav()
        ConsentGateScreen(
            visible = shouldShowConsent,
            onComplete = {
                consentViewModel.markConsentGateCompleted()
            },
            onDismiss = {},
            viewModel = consentViewModel
        )
        UpdateDialog(
            state = updateState,
            onInstall = updateViewModel::installUpdate,
            onDismiss = updateViewModel::dismissOptionalUpdate,
            onRetryCheck = updateViewModel::retryCheck
        )
    }
}
