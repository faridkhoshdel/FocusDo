package com.example.focusdo

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults as Material3ButtonDefaults
import androidx.compose.ui.graphics.Color

/** Compatibility facade for the themed button call used by MainScreen. */
object ButtonDefaults {
    fun buttonColors(containerColor: Color): ButtonColors =
        Material3ButtonDefaults.buttonColors(containerColor = containerColor)
}
