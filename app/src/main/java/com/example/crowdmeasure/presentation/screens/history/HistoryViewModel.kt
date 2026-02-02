package com.example.crowdmeasure.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistory: GetHistoryUseCase
) : ViewModel() {

    private val _tag = MutableStateFlow<String?>(null)
    val tag: StateFlow<String?> = _tag.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val items = _tag.flatMapLatest { tag ->
        getHistory(limit = 50, feedbackTag = tag)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTag(tag: String?) {
        _tag.value = tag?.takeIf { it.isNotBlank() }
    }
}