package com.mobileorienteering.data.model.app

import com.mobileorienteering.data.model.domain.OrientationType
import com.mobileorienteering.data.model.domain.RaceStyle
import com.mobileorienteering.data.model.domain.TimerStart

data class RunSettings(
    val orderedControlPoints: Boolean = true,
    val timerStart: TimerStart = TimerStart.RACE_START,
    val raceStyle: RaceStyle = RaceStyle.STANDARD,
    val orientationType: OrientationType = OrientationType.FOOT,
    val detectionRadius: Float = 15f
)
