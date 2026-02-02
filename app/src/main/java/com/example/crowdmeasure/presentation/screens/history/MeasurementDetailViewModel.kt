package com.example.crowdmeasure.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.domain.model.Measurement
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeasurementDetailViewModel @Inject constructor(
    private val repo: MeasurementRepository
) : ViewModel() {

    private val _item = MutableStateFlow<Measurement?>(null)
    val item: StateFlow<Measurement?> = _item

    fun load(id: String) = viewModelScope.launch {
        _item.value = repo.getMeasurementById(id)
    }
}