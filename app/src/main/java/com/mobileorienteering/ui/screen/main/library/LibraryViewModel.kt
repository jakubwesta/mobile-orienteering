package com.mobileorienteering.ui.screen.main.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.repository.AuthRepository
import com.mobileorienteering.data.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.mobileorienteering.data.model.Map as OrienteeringMap

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val maps: StateFlow<List<OrienteeringMap>> = authRepository.authModelFlow
        .flatMapLatest { auth ->
            if (auth != null) {
                mapRepository.getMapsForUserFlow(auth.userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteMap(mapId: Long) {
        viewModelScope.launch {
            mapRepository.deleteMap(mapId)
        }
    }
}