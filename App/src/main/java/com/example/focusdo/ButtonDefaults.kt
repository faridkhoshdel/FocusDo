package com.example.focusdo

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults as Material3ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ButtonDefaults {
    @Composable
    fun buttonColors(containerColor: Color): ButtonColors =
        Material3ButtonDefaults.buttonColors(
            containerColor = containerColor
        )
}
