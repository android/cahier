package com.example.cahier.developer.brushgraph.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

internal const val SPLINE_HIT_SEGMENTS = 50

internal fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
  val l2 = (b - a).getDistanceSquared()
  if (l2 == 0f) return (p - a).getDistance()
  var t = ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2
  t = max(0f, min(1f, t))
  return (p - (a + (b - a) * t)).getDistance()
}

internal fun createSplinePath(start: Offset, end: Offset): Path {
  val horizontalOffset = maxOf(50f, abs(end.x - start.x) / 2f).coerceAtMost(200f)
  return Path().apply {
    moveTo(start.x, start.y)
    cubicTo(start.x + horizontalOffset, start.y, end.x - horizontalOffset, end.y, end.x, end.y)
  }
}

internal fun distanceToSpline(p: Offset, start: Offset, end: Offset): Float {
  val horizontalOffset = maxOf(50f, abs(end.x - start.x) / 2f).coerceAtMost(200f)
  val cp1 = Offset(start.x + horizontalOffset, start.y)
  val cp2 = Offset(end.x - horizontalOffset, end.y)

  var minDistance = Float.MAX_VALUE
  var prevPoint = start
  for (i in 1..SPLINE_HIT_SEGMENTS) {
    val t = i.toFloat() / SPLINE_HIT_SEGMENTS
    val currentPoint = cubicBezier(t, start, cp1, cp2, end)
    minDistance = min(minDistance, distanceToSegment(p, prevPoint, currentPoint))
    prevPoint = currentPoint
  }
  return minDistance
}

internal fun cubicBezier(t: Float, p0: Offset, p1: Offset, p2: Offset, p3: Offset): Offset {
  val u = 1 - t
  val tt = t * t
  val uu = u * u
  val uuu = uu * u
  val ttt = tt * t

  val x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x
  val y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y
  return Offset(x, y)
}
