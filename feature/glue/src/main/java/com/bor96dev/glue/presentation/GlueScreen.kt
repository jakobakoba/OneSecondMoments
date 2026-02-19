package com.bor96dev.glue.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bor96dev.glue.presentation.event.GlueEvent
import com.bor96dev.glue.presentation.state.GlueState

@Composable
fun GlueScreen(
    state: GlueState,
    onEvent: (GlueEvent)  -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){

        Text("GlueScreen")


    }
}