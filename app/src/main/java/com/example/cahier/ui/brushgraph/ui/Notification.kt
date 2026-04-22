package com.example.cahier.ui.brushgraph.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.cahier.ui.brushgraph.BrushGraphViewModel
import com.example.cahier.ui.brushgraph.model.GraphValidationException
import com.example.cahier.ui.brushgraph.model.ValidationSeverity
import com.example.cahier.ui.brushgraph.model.INSPECTOR_WIDTH_LANDSCAPE
import com.example.cahier.ui.brushgraph.model.INSPECTOR_HEIGHT_PORTRAIT
import com.example.cahier.ui.theme.extendedColorScheme
import android.util.Log

@Composable
fun NotificationPane(
  isLandscape: Boolean,
  viewModel: BrushGraphViewModel,
  modifier: Modifier = Modifier,
) {
  val issues = viewModel.graphIssues.collectAsState().value
  val hasErrors = issues.any { it.severity == ValidationSeverity.ERROR }
  val hasWarnings = issues.any { it.severity == ValidationSeverity.WARNING }

  AnimatedVisibility(
    visible = viewModel.isErrorPaneOpen,
    enter =
      if (isLandscape) {
        slideInHorizontally(initialOffsetX = { it })
      } else {
        slideInVertically(initialOffsetY = { it })
      },
    exit =
      if (isLandscape) {
        slideOutHorizontally(targetOffsetX = { it })
      } else {
        slideOutVertically(targetOffsetY = { it })
      },
    modifier = modifier.zIndex(10f),
  ) {
    Surface(
      modifier =
        if (isLandscape) {
          Modifier.fillMaxHeight().width(INSPECTOR_WIDTH_LANDSCAPE.dp)
        } else {
          Modifier.fillMaxWidth().height(INSPECTOR_HEIGHT_PORTRAIT.dp)
        },
      tonalElevation = 8.dp,
      shadowElevation = 8.dp,
      color = MaterialTheme.colorScheme.surface,
    ) {
      Column {
        // Title bar with close button
        val headerColor =
          when {
            hasErrors -> MaterialTheme.colorScheme.error
            hasWarnings -> MaterialTheme.extendedColorScheme.warning
            else -> MaterialTheme.colorScheme.primary
          }
        val iconColor =
          when {
            hasErrors -> MaterialTheme.colorScheme.onError
            hasWarnings -> MaterialTheme.extendedColorScheme.onWarning
            else -> MaterialTheme.colorScheme.onPrimary
          }
        val headerIcon =
          when {
            hasErrors -> Icons.Default.Error
            hasWarnings -> Icons.Default.Warning
            else -> Icons.Default.Info
          }

        Surface(color = headerColor, tonalElevation = 2.dp) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
          ) {
            Icon(headerIcon, contentDescription = null, tint = iconColor)
            Spacer(Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.bg_notifications_count, issues.size),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
              color = iconColor,
            )
            IconButton(onClick = { viewModel.toggleErrorPane() }) {
              Icon(Icons.Default.Close, contentDescription = stringResource(R.string.bg_cd_close_pane), tint = iconColor)
            }
          }
        }
        LazyColumn(modifier = Modifier.padding(16.dp)) {
          val errors = issues.filter { it.severity == ValidationSeverity.ERROR }
          val warnings = issues.filter { it.severity == ValidationSeverity.WARNING }
          val debugs = issues.filter { it.severity == ValidationSeverity.DEBUG }

          if (errors.isNotEmpty()) {
            item {
              NotificationGroup(
                title = stringResource(R.string.bg_errors),
                issues = errors,
                icon = Icons.Default.Error,
                color = MaterialTheme.colorScheme.error,
                viewModel = viewModel,
                isLandscape = isLandscape,
              )
            }
          }
          if (warnings.isNotEmpty()) {
            item {
              NotificationGroup(
                title = stringResource(R.string.bg_warnings),
                issues = warnings,
                icon = Icons.Default.Warning,
                color = MaterialTheme.extendedColorScheme.warning,
                viewModel = viewModel,
                isLandscape = isLandscape,
              )
            }
          }
          if (debugs.isNotEmpty()) {
            item {
              NotificationGroup(
                title = stringResource(R.string.bg_debug),
                issues = debugs,
                icon = Icons.Default.Info,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                viewModel = viewModel,
                isLandscape = isLandscape,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun NotificationGroup(
  title: String,
  issues: List<GraphValidationException>,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  viewModel: BrushGraphViewModel,
  isLandscape: Boolean,
) {
  var expanded by remember { mutableStateOf(true) }
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
    Surface(
      modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      shape = RoundedCornerShape(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      ) {
        Icon(
          if (expanded) {
            Icons.Default.KeyboardArrowDown
          } else {
            Icons.Default.ChevronRight
          },
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
          text = "$title (${issues.size})",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = color,
        )
      }
    }
    if (expanded) {
      Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
        for (issue in issues) {
          val density = androidx.compose.ui.platform.LocalDensity.current.density
          val message = issue.displayMessage.asString()
          LaunchedEffect(issue) {
            Log.d("NotificationPane", message)
          }
          Surface(
            modifier =
              Modifier.fillMaxWidth().padding(vertical = 4.dp).let {
                if (issue.nodeId != null) {
                  it.clickable { viewModel.onIssueClick(issue, isLandscape, density) }
                } else {
                  it
                }
              },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(4.dp),
          ) {
            Text(
              text = message,
              modifier = Modifier.padding(8.dp),
              style = MaterialTheme.typography.bodySmall,
              color = color,
            )
          }
        }
      }
    }
  }
}
