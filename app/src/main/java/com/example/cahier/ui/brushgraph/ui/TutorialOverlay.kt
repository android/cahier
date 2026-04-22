package com.example.cahier.ui.brushgraph.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.cahier.ui.brushgraph.model.TutorialStep
import com.example.cahier.ui.brushgraph.model.TutorialAction

@Composable
fun TutorialOverlay(
    step: TutorialStep,
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(400.dp)
            .wrapContentHeight()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(step.title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = stringResource(step.message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (onBack != null) {
                    Button(onClick = onBack) {
                        Text(stringResource(R.string.bg_back))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Button(onClick = onNext) {
                    Text(if (step.actionRequired == TutorialAction.CLICK_NEXT) stringResource(R.string.bg_next) else stringResource(R.string.bg_got_it))
                }
            }
        }
    }
}
