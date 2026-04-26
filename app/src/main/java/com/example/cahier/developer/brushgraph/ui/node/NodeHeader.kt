@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.developer.brushgraph.ui.node

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.example.cahier.R
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.ui.CoatPreviewWidget
import com.example.cahier.developer.brushgraph.ui.ColorFunctionPreviewWidget
import com.example.cahier.developer.brushgraph.ui.TextureLayerPreviewWidget
import com.example.cahier.developer.brushgraph.ui.TipPreviewWidget
import com.example.cahier.developer.brushgraph.ui.asString
import com.example.cahier.developer.brushgraph.ui.titleHeight
import ink.proto.BrushCoat as ProtoBrushCoat

@Composable
fun NodeHeader(
  node: GraphNode,
  graph: BrushGraph,
  strokeRenderer: CanvasStrokeRenderer,
  modifier: Modifier = Modifier,
) {
  val data = node.data
  Column(modifier = modifier) {
    Row(
      modifier = Modifier
        .height(with(LocalDensity.current) { data.titleHeight().toDp() })
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
        Text(
          text = stringResource(data.title()),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        for (subtitle in data.subtitles()) {
          val subtitleText = subtitle.asString()
          Text(
            text = subtitleText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // Previews for Tip and Coat nodes.
      if (data is NodeData.Tip) {
        Box(modifier = Modifier.size(60.dp).padding(4.dp)) {
          TipPreviewWidget(data.tip, strokeRenderer)
        }
      } else if (data is NodeData.Coat) {
        val coat = try {
            com.example.cahier.developer.brushgraph.data.BrushFamilyConverter.createCoat(node, graph, mutableMapOf())
        } catch (e: Exception) {
            ProtoBrushCoat.getDefaultInstance()
        }

        Box(modifier = Modifier.size(60.dp).padding(4.dp)) {
          CoatPreviewWidget(coat, strokeRenderer)
        }
      }
    }

    if (data is NodeData.ColorFunction) {
      Box(modifier = Modifier.size(60.dp).padding(4.dp)) {
        ColorFunctionPreviewWidget(data.function, strokeRenderer)
      }
    }
    if (data is NodeData.TextureLayer) {
      Box(modifier = Modifier.size(60.dp).padding(4.dp)) {
        TextureLayerPreviewWidget(data.layer, strokeRenderer)
      }
    }
  }
}
