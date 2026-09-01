package com.abtsplazita.posplazita.scale

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockScaleManager : ScaleManager {
    private val _currentWeight = MutableStateFlow(0.0)
    override val currentWeight: StateFlow<Double> = _currentWeight.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override fun connect() {
        _isConnected.value = true
    }

    override fun disconnect() {
        _isConnected.value = false
    }

    fun setWeight(weight: Double) {
        _currentWeight.value = weight
    }
}
