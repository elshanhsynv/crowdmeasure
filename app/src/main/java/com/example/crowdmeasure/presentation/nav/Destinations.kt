object Destinations {
    const val Home = "home"
    const val History = "history"
    const val Settings = "settings"
    const val Detail = "detail/{id}"
    fun detail(id: String) = "detail/$id"
}