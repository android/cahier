package com.example.cahier.developer.brushgraph.ui.fields

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.R

@Composable
fun CoatNodeFields(
  modifier: Modifier = Modifier
) {
  Text(
    text = stringResource(R.string.bg_coat_node_description),
    style = MaterialTheme.typography.bodySmall,
    modifier = modifier
  )
}
