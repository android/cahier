@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.ui

import androidx.compose.runtime.produceState
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor
import com.example.cahier.ui.brushgraph.model.toBrushFamily
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.graphics.Matrix
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.InProgressStroke
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.BrushPaint as ProtoBrushPaint

@Composable
fun SineWavePreview(
  brush: Brush,
  strokeRenderer: CanvasStrokeRenderer,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier = modifier) {
    val canvasWidth = with(LocalDensity.current) { maxWidth.toPx() }
    val canvasHeight = with(LocalDensity.current) { maxHeight.toPx() }

    val stroke =
      remember(brush, canvasWidth, canvasHeight) {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@remember null
        val inputs = MutableStrokeInputBatch()
        val numPoints = 100
        val horizontalBuffer = 40f
        val effectiveWidth = canvasWidth - 2 * horizontalBuffer
        if (effectiveWidth <= 0f) return@remember null

        val midY = canvasHeight / 2f
        val amplitude = canvasHeight * 0.2f
        val period = 1.5f
        val frequency = 2f * Math.PI.toFloat() * period / effectiveWidth

        for (i in 0 until numPoints) {
          val xOffset = i.toFloat() * effectiveWidth / (numPoints - 1)
          val x = horizontalBuffer + xOffset
          val y = midY + amplitude * kotlin.math.sin(frequency * xOffset)
          inputs.add(type = InputToolType.STYLUS, x = x, y = y, elapsedTimeMillis = i.toLong() * 10)
        }
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(brush)
        inProgressStroke.enqueueInputs(inputs, MutableStrokeInputBatch())
        inProgressStroke.updateShape(numPoints.toLong() * 10)
        inProgressStroke.toImmutable()
      }

    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = Modifier.fillMaxSize()) {
      // Checkerboard background
      val tileSize = 7.dp.toPx()
      val numTilesX = (size.width / tileSize).toInt() + 2
      val numTilesY = (size.height / tileSize).toInt() + 2
      val startX = (size.width - numTilesX * tileSize) / 2f
      val startY = (size.height - numTilesY * tileSize) / 2f
      for (ix in 0 until numTilesX) {
        for (iy in 0 until numTilesY) {
          val color = if ((ix + iy) % 2 == 0) surface else surfaceVariant
          drawRect(
            color = color,
            topLeft = Offset(startX + ix * tileSize, startY + iy * tileSize),
            size = Size(tileSize, tileSize),
          )
        }
      }

      if (stroke != null) {
        drawIntoCanvas { canvas ->
          strokeRenderer.draw(canvas.nativeCanvas, stroke, Matrix())
        }
      }
    }
  }
}

@Composable
fun CoatPreviewWidget(brushCoat: ProtoBrushCoat, renderer: CanvasStrokeRenderer) {
  val family by produceState<BrushFamily>(initialValue = StockBrushes.marker(), key1 = brushCoat) {
    value = withContext(Dispatchers.IO) {
      val familyProto = ProtoBrushFamily.newBuilder().addCoats(brushCoat).build()
      runCatching { familyProto.toBrushFamily() }.getOrDefault(StockBrushes.marker())
    }
  }
  StrokePreviewWidget(
    family,
    renderer = renderer,
    brushSize = 10f,
    showSingleInput = false,
  )
}

@Composable
fun TipPreviewWidget(brushTip: ProtoBrushTip, renderer: CanvasStrokeRenderer) {
  val family by produceState<BrushFamily>(initialValue = StockBrushes.marker(), key1 = brushTip) {
    value = withContext(Dispatchers.IO) {
      val familyProto = ProtoBrushFamily.newBuilder()
        .addCoats(ProtoBrushCoat.newBuilder().setTip(brushTip).build())
        .build()
      runCatching { familyProto.toBrushFamily() }.getOrDefault(StockBrushes.marker())
    }
  }
  StrokePreviewWidget(family, renderer = renderer)
}

@Composable
fun ColorFunctionPreviewWidget(
  colorFunction: ink.proto.ColorFunction,
  renderer: CanvasStrokeRenderer,
) {
  val family by produceState<BrushFamily>(initialValue = StockBrushes.marker(), key1 = colorFunction) {
    value = withContext(Dispatchers.IO) {
      runCatching {
        ProtoBrushFamily.newBuilder()
          .addCoats(
            ProtoBrushCoat.newBuilder()
              .setTip(ProtoBrushTip.newBuilder().setCornerRounding(0f).build())
              .addPaintPreferences(ProtoBrushPaint.newBuilder().addColorFunctions(colorFunction).build())
              .build()
          )
          .build()
          .toBrushFamily()
      }.getOrDefault(StockBrushes.marker())
    }
  }
  StrokePreviewWidget(family, renderer = renderer, brushSize = 30f, zoom = 3f)
}

@Composable
fun TextureLayerPreviewWidget(
  textureLayer: ProtoBrushPaint.TextureLayer,
  renderer: CanvasStrokeRenderer,
) {
  val family by produceState<BrushFamily>(initialValue = StockBrushes.marker(), key1 = textureLayer) {
    value = withContext(Dispatchers.IO) {
      runCatching {
        ProtoBrushFamily.newBuilder()
          .addCoats(
            ProtoBrushCoat.newBuilder()
              .setTip(ProtoBrushTip.newBuilder().setCornerRounding(0f).build())
              .addPaintPreferences(ProtoBrushPaint.newBuilder().addTextureLayers(textureLayer).build())
              .build()
          )
          .build()
          .toBrushFamily()
      }.getOrDefault(StockBrushes.marker())
    }
  }
  StrokePreviewWidget(family, renderer = renderer, brushSize = 30f, zoom = 3f)
}

@Composable
fun TextureWrapPreviewWidget(
  wrapX: ProtoBrushPaint.TextureLayer.Wrap,
  wrapY: ProtoBrushPaint.TextureLayer.Wrap,
  renderer: CanvasStrokeRenderer,
  clientTextureId: String = "",
) {
  val family by produceState<BrushFamily>(initialValue = StockBrushes.marker(), key1 = Triple(wrapX, wrapY, clientTextureId)) {
    value = withContext(Dispatchers.IO) {
      val textureLayer =
        ProtoBrushPaint.TextureLayer.newBuilder()
          .setClientTextureId(clientTextureId)
          .setSizeX(1f / 3f)
          .setSizeY(1f / 3f)
          .setWrapX(wrapX)
          .setWrapY(wrapY)
          .setMapping(ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING)
          .setSizeUnit(ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_BRUSH_SIZE)
          .build()

      runCatching {
        ProtoBrushFamily.newBuilder()
          .addCoats(
            ProtoBrushCoat.newBuilder()
              .setTip(ProtoBrushTip.newBuilder().setCornerRounding(0f).build())
              .addPaintPreferences(ProtoBrushPaint.newBuilder().addTextureLayers(textureLayer).build())
              .build()
          )
          .build()
          .toBrushFamily()
      }.getOrDefault(StockBrushes.marker())
    }
  }
  StrokePreviewWidget(family, renderer = renderer, brushSize = 30f, zoom = 3f)
}

@Composable
fun BlendModePreviewWidget(
  blendMode: ProtoBrushPaint.TextureLayer.BlendMode,
  renderer: CanvasStrokeRenderer,
  clientTextureId: String = "",
) {
  val family by produceState<BrushFamily>(initialValue = StockBrushes.marker(), key1 = Pair(blendMode, clientTextureId)) {
    value = withContext(Dispatchers.IO) {
      val topLayer =
        ProtoBrushPaint.TextureLayer.newBuilder()
          .setClientTextureId(clientTextureId)
          .setBlendMode(blendMode)
          .setSizeX(1f)
          .setSizeY(1f)
          .build()
          
      val bottomLayer = 
        ProtoBrushPaint.TextureLayer.newBuilder()
          .setClientTextureId(clientTextureId)
          .setOffsetX(0.2f)
          .setOffsetY(0.2f)
          .setSizeX(1f)
          .setSizeY(1f)
          .build()

      runCatching {
        ProtoBrushFamily.newBuilder()
          .addCoats(
            ProtoBrushCoat.newBuilder()
              .setTip(ProtoBrushTip.newBuilder().setCornerRounding(0f).build())
              .addPaintPreferences(
                ProtoBrushPaint.newBuilder()
                  .addTextureLayers(bottomLayer)
                  .addTextureLayers(topLayer)
                  .build()
              )
              .build()
          )
          .build()
          .toBrushFamily()
      }.getOrDefault(StockBrushes.marker())
    }
  }
  StrokePreviewWidget(family, renderer = renderer, brushSize = 30f, zoom = 3f)
}

@Composable
fun StrokePreviewWidget(
  family: BrushFamily,
  renderer: CanvasStrokeRenderer,
  brushSize: Float = 30f,
  showSingleInput: Boolean = true,
  zoom: Float = 1f,
) {
  val canvasBackground = MaterialTheme.colorScheme.surfaceContainer
  val brush =
    Brush.createWithComposeColor(
      family,
      MaterialTheme.colorScheme.primary,
      size = brushSize,
      epsilon = 0.1f
    )

  // The two [StrokeInputBatch]s for the preview widget.
  val zigzagStroke = remember(brush) {
    val mutable = MutableStrokeInputBatch()
      .add(type = InputToolType.STYLUS, x = -30f, y = -24f, elapsedTimeMillis = 0L)
      .add(type = InputToolType.STYLUS, x = 15f, y = -30f, elapsedTimeMillis = 100L)
      .add(type = InputToolType.STYLUS, x = -24f, y = 24f, elapsedTimeMillis = 200L)
      .add(type = InputToolType.STYLUS, x = 30f, y = 6f, elapsedTimeMillis = 300L)
    val inProgress = InProgressStroke()
    inProgress.start(brush)
    inProgress.enqueueInputs(mutable, MutableStrokeInputBatch())
    inProgress.updateShape(300L)
    inProgress.toImmutable()
  }

  val dotStroke = remember(brush) {
    val mutable = MutableStrokeInputBatch()
      .add(type = InputToolType.STYLUS, x = 0f, y = 0f, elapsedTimeMillis = 0L)
    val inProgress = InProgressStroke()
    inProgress.start(brush)
    inProgress.enqueueInputs(mutable, MutableStrokeInputBatch())
    inProgress.updateShape(0L)
    inProgress.toImmutable()
  }

  var zigzag by remember(brush) { mutableStateOf(zigzagStroke) }
  var singleDot by remember(brush) { mutableStateOf(dotStroke) }

  // maxTipWidth is 4x the Brush.size. BrushTip.scale and BrushTip.slant each can double the width.
  val maxTipWidth = with(LocalDensity.current) { (4f * brush.size).toDp() }
  val maxStrokeWidth = with(LocalDensity.current) { (3f * 4f * brush.size).toDp() }
  val canvasSize = if (showSingleInput) maxTipWidth else maxStrokeWidth
  Canvas(
    modifier =
      Modifier.height(canvasSize)
        .width(canvasSize)
        .clip(RectangleShape)
        .background(canvasBackground),
    onDraw = {
      drawIntoCanvas { canvas ->
        // Translate stroke to center of the canvas.
        canvas.scale(zoom, zoom)
        canvas.translate(size.width / (2f * zoom), size.height / (2f * zoom))
        renderer.draw(canvas.nativeCanvas, if (showSingleInput) singleDot else zigzag, Matrix())
      }
    },
  )
}
