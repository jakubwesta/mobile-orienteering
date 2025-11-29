package com.mobileorienteering.ui.screen.main.library

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.mobileorienteering.data.model.Map

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val mapRepository: MapRepository
) : ViewModel() {

    val maps: StateFlow<List<Map>> = mapRepository.getAllMapsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)
    var successMessage = mutableStateOf<String?>(null)

    var searchQuery = mutableStateOf("")
        private set

    var sortOrder = mutableStateOf(SortOrder.DATE_DESC)
        private set

    private val _filteredMaps = MutableStateFlow<List<Map>>(emptyList())
    val filteredMaps: StateFlow<List<Map>> = _filteredMaps.asStateFlow()

    init {
        viewModelScope.launch {
            maps.collect { allMaps ->
                updateFilteredMaps(allMaps)
            }
        }
    }

    fun getMap(mapId: Long): Flow<Map?> {
        return mapRepository.getMapByIdFlow(mapId)
    }

    fun deleteMap(mapId: Long) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            try {
                val result = mapRepository.deleteMap(mapId)

                result.onSuccess {
                    successMessage.value = "Map deleted"
                }.onFailure { e ->
                    error.value = e.message ?: "Failed to delete map"
                }
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        updateFilteredMaps(maps.value)
    }

    fun setSortOrder(order: SortOrder) {
        sortOrder.value = order
        updateFilteredMaps(maps.value)
    }

    fun clearError() {
        error.value = null
    }

    fun clearSuccessMessage() {
        successMessage.value = null
    }

    private fun updateFilteredMaps(allMaps: List<Map>) {
        var filtered = allMaps

        val query = searchQuery.value
        if (query.isNotBlank()) {
            filtered = filtered.filter { map ->
                map.name.contains(query, ignoreCase = true)
            }
        }

        filtered = when (sortOrder.value) {
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.createdAt }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.createdAt }
            SortOrder.TITLE_ASC -> filtered.sortedBy { it.name }
        }

        _filteredMaps.value = filtered
    }

    fun getAllMaps(): StateFlow<List<Map>> {
        return mapRepository.getAllMapsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
}

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    TITLE_ASC
}
