package com.bor96dev.edit.presentation

fun formatLocationText(city: String?, country: String?): String? {
    val cleanCity = city?.trim().orEmpty()
    val cleanCountry = country?.trim().orEmpty()
    return when {
        cleanCity.isNotEmpty() && cleanCountry.isNotEmpty() -> "$cleanCity / $cleanCountry"
        else -> null
    }
}