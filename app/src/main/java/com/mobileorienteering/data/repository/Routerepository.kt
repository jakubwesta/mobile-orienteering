package com.mobileorienteering.data.repository

import com.mobileorienteering.data.model.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor() {

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    val routes: Flow<List<Route>> = _routes

    fun saveRoute(route: Route) {
        _routes.value = _routes.value + route
    }

    fun deleteRoute(routeId: String) {
        _routes.value = _routes.value.filter { it.id != routeId }
    }

    fun getRoute(routeId: String): Route? {
        return _routes.value.find { it.id == routeId }
    }
}