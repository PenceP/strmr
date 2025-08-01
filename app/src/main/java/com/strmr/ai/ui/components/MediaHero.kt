package com.strmr.ai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MediaHero(
    mediaDetails: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        // Media details
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(0.dp),
        ) {
            mediaDetails()
        }
    }
}
