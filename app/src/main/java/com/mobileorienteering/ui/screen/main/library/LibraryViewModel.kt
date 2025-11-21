package com.mobileorienteering.ui.screen.main.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.Route
import com.mobileorienteering.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val routeRepository: RouteRepository
) : ViewModel() {

    val routes: StateFlow<List<Route>> = routeRepository.routes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteRoute(routeId: String) {
        routeRepository.deleteRoute(routeId)
    }
}