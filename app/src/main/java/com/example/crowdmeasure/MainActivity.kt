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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.presentation.nav.AppNav
import com.example.crowdmeasure.presentation.screens.consent.ConsentGateScreen
import com.example.crowdmeasure.presentation.screens.consent.ConsentGateViewModel
import com.example.crowdmeasure.presentation.ui.theme.CrowdMeasureTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main (and only) activity for CrowdMeasure.
 *
 * Architecture:
 * - Single-activity + Jetpack Compose Navigation
 * - Edge-to-edge layout with proper insets handling
 * - Consent gate shown on first launch or when consent is needed
 * - Main app navigation shown after consent is granted
 *
 * Flow:
 * 1. Check if consent is needed (via ViewModel)
 * 2. If needed: show ConsentGateScreen (full screen overlay)
 * 3. If not needed: show AppNav (main app)
 * 4. User can dismiss consent screen (we track dismissal to avoid re-showing)
 *
 * Performance:
 * - collectAsStateWithLifecycle for automatic lifecycle handling
 * - rememberSaveable for dismissal state (survives config changes)
 * - Minimal recomposition (stable state, no lambda recreation)
 */
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
    val consentViewModel: ConsentGateViewModel = hiltViewModel()
    val settings by consentViewModel.settings.collectAsStateWithLifecycle()

    // Track whether user explicitly dismissed the consent screen
    // (survives configuration changes via rememberSaveable)
    var userDismissedConsent by rememberSaveable { mutableStateOf(false) }

    // Determine if consent screen should be shown
    val shouldShowConsent = when {
        // User explicitly dismissed - don't show again this session
        userDismissedConsent -> false

        // Settings not loaded yet - wait
        settings == null -> false

        // User previously completed consent flow - don't show
        settings?.consentGateDismissed == true -> false

        // Missing consent or collection not enabled - show consent screen
        settings?.consentAccepted != true || !settings!!.collectionEnabled -> true

        // All good - don't show
        else -> false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main app (always rendered, but may be covered by consent screen)
        AppNav()

        // Consent screen (overlays main app when needed)
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