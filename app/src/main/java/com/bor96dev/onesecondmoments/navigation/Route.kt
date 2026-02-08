package com.bor96dev.onesecondmoments.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey{
    @Serializable
    data object Record : Route, NavKey
    @Serializable
    data object Montage: Route, NavKey
    @Serializable
    data object Calendar: Route, NavKey
}