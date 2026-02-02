package com.example.crowdmeasure.presentation.nav

object Destinations {
    const val Onboarding = "onboarding"

    // Root graph that hosts bottom tabs
    const val Main = "main"

    // Tabs
    const val Home = "home"
    const val History = "history"
    const val Settings = "settings"

    // Non-tab destinations
    const val Detail = "detail/{id}"
    fun detail(id: String) = "detail/$id"
}