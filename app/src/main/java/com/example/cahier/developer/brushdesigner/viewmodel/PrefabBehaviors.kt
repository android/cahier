/*
 * Copyright 2026 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.cahier.developer.brushdesigner.viewmodel

import ink.proto.BrushBehavior
import kotlin.math.PI

/**
 * Pre-built behavior node lists for the Brush Designer's "Quick Add" menu.
 *
 * Each function returns a fully configured [List] of [BrushBehavior.Node] with
 * source/target ranges tuned for the specific use case. The ViewModel's generic
 * [BrushDesignerViewModel.addBehavior] method accepts these lists directly.
 *
 * Organized by input source category:
 * - **Pressure-based**: respond to stylus pressure
 * - **Tilt-based**: respond to stylus tilt angle
 * - **Speed-based**: respond to stroke velocity
 * - **Direction-based**: respond to stroke direction
 * - **Distance/Time-based**: animate over stroke progress
 * - **Jitter (Noise-based)**: add randomized variation
 */
object PrefabBehaviors {

    // ── Pressure-based ────────────────────────────────────────────

    /** Pressure → Size: tip scales 50%–150% based on stylus pressure. */
    fun pressureToSize(): List<BrushBehavior.Node> = smoothed(
        source = BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE,
        sourceStart = 0f, sourceEnd = 1f,
        target = BrushBehavior.Target.TARGET_SIZE_MULTIPLIER,
        targetStart = 0.5f, targetEnd = 1.5f,
        dampingSeconds = 0.15f
    )

    /** Pressure → Width: tip width scales 60%–140% based on stylus pressure. */
    fun pressureToWidth(): List<BrushBehavior.Node> = smoothed(
        source = BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE,
        sourceStart = 0f, sourceEnd = 1f,
        target = BrushBehavior.Target.TARGET_WIDTH_MULTIPLIER,
        targetStart = 0.6f, targetEnd = 1.4f,
        dampingSeconds = 0.1f
    )

    /** Pressure → Opacity: lighter pressure = more transparent. */
    fun pressureToOpacity(): List<BrushBehavior.Node> = smoothed(
        source = BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE,
        sourceStart = 0f, sourceEnd = 1f,
        target = BrushBehavior.Target.TARGET_OPACITY_MULTIPLIER,
        targetStart = 0.3f, targetEnd = 1.0f,
        dampingSeconds = 0.1f
    )

    // ── Tilt-based ────────────────────────────────────────────────

    /** Tilt → Width: tilting stylus widens stroke (1×–2.5×). */
    fun tiltToWidth(): List<BrushBehavior.Node> = smoothed(
        source = BrushBehavior.Source.SOURCE_TILT_IN_RADIANS,
        sourceStart = 0f, sourceEnd = (PI / 2).toFloat(),
        target = BrushBehavior.Target.TARGET_WIDTH_MULTIPLIER,
        targetStart = 1.0f, targetEnd = 2.5f,
        dampingSeconds = 0.1f
    )

    /** Tilt → Slant: tilting stylus rotates the tip shape. */
    fun tiltToSlant(): List<BrushBehavior.Node> = smoothed(
        source = BrushBehavior.Source.SOURCE_TILT_IN_RADIANS,
        sourceStart = 0f, sourceEnd = (PI / 2).toFloat(),
        target = BrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS,
        targetStart = -0.5f, targetEnd = 0.5f,
        dampingSeconds = 0.08f
    )

    /** Tilt → Opacity: flat tilt = more transparent (0.4×–1.0×). */
    fun tiltToOpacity(): List<BrushBehavior.Node> = smoothed(
        source = BrushBehavior.Source.SOURCE_TILT_IN_RADIANS,
        sourceStart = 0f, sourceEnd = (PI / 2).toFloat(),
        target = BrushBehavior.Target.TARGET_OPACITY_MULTIPLIER,
        targetStart = 0.4f, targetEnd = 1.0f,
        dampingSeconds = 0.1f
    )

    // ── Speed-based ───────────────────────────────────────────────

    /** Speed → Opacity: fast strokes become transparent (1.0×→0.2×). */
    fun speedToOpacity(): List<BrushBehavior.Node> = smoothed(
        source = BrushBehavior.Source.SOURCE_SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
        sourceStart = 0f, sourceEnd = 8f,
        target = BrushBehavior.Target.TARGET_OPACITY_MULTIPLIER,
        targetStart = 1.0f, targetEnd = 0.2f,
        dampingSeconds = 0.3f
    )

    /** Speed → Size: fast strokes shrink (1.0×→0.4×). */
    fun speedToSize(): List<BrushBehavior.Node> = smoothed(
        source = BrushBehavior.Source.SOURCE_SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
        sourceStart = 0f, sourceEnd = 8f,
        target = BrushBehavior.Target.TARGET_SIZE_MULTIPLIER,
        targetStart = 1.0f, targetEnd = 0.4f,
        dampingSeconds = 0.2f
    )

    /** Speed → Width: fast strokes narrow (1.0×→0.5×). */
    fun speedToWidth(): List<BrushBehavior.Node> = smoothed(
        source = BrushBehavior.Source.SOURCE_SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
        sourceStart = 0f, sourceEnd = 8f,
        target = BrushBehavior.Target.TARGET_WIDTH_MULTIPLIER,
        targetStart = 1.0f, targetEnd = 0.5f,
        dampingSeconds = 0.15f
    )

    // ── Direction-based ───────────────────────────────────────────

    /** Direction → Rotation: tip rotates to follow stroke direction. */
    fun directionToRotation(): List<BrushBehavior.Node> = smoothed(
        source = BrushBehavior.Source.SOURCE_DIRECTION_IN_RADIANS,
        sourceStart = 0f, sourceEnd = (2 * PI).toFloat(),
        target = BrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS,
        targetStart = (-PI).toFloat(), targetEnd = PI.toFloat(),
        dampingSeconds = 0.05f
    )

    // ── Distance / Time-based ─────────────────────────────────────

    /** Distance → Opacity fade: stroke fades out over 5× brush-size distance. */
    fun distanceToOpacityFade(): List<BrushBehavior.Node> = direct(
        source = BrushBehavior.Source.SOURCE_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
        sourceStart = 0f, sourceEnd = 5f,
        target = BrushBehavior.Target.TARGET_OPACITY_MULTIPLIER,
        targetStart = 1.0f, targetEnd = 0.1f
    )

    /** Time → Opacity fade: stroke fades out over 2 seconds. */
    fun timeToOpacityFade(): List<BrushBehavior.Node> = direct(
        source = BrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_SECONDS,
        sourceStart = 0f, sourceEnd = 2f,
        target = BrushBehavior.Target.TARGET_OPACITY_MULTIPLIER,
        targetStart = 1.0f, targetEnd = 0.2f
    )

    // ── Jitter (Noise-based) ──────────────────────────────────────

    /** Random slant jitter: pencil-like wobble (±0.15 radians). */
    fun slantJitter(): List<BrushBehavior.Node> = jitter(
        target = BrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS,
        targetStart = -0.15f, targetEnd = 0.15f,
        basePeriod = 0.3f
    )

    /** Random width jitter: organic line variation (85%–115%). */
    fun widthJitter(): List<BrushBehavior.Node> = jitter(
        target = BrushBehavior.Target.TARGET_WIDTH_MULTIPLIER,
        targetStart = 0.85f, targetEnd = 1.15f,
        basePeriod = 0.4f
    )

    /** Random opacity jitter: textured feel (70%–100%). */
    fun opacityJitter(): List<BrushBehavior.Node> = jitter(
        target = BrushBehavior.Target.TARGET_OPACITY_MULTIPLIER,
        targetStart = 0.7f, targetEnd = 1.0f,
        basePeriod = 0.5f
    )

    /** Random hue jitter: color variation (±0.15 radians). */
    fun hueJitter(): List<BrushBehavior.Node> = jitter(
        target = BrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS,
        targetStart = -0.15f, targetEnd = 0.15f,
        basePeriod = 0.8f
    )

    /** Random lateral position jitter: scatter effect (±0.1× brush size). */
    fun positionJitter(): List<BrushBehavior.Node> = jitter(
        target = BrushBehavior.Target.TARGET_POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE,
        targetStart = -0.1f, targetEnd = 0.1f,
        basePeriod = 0.2f
    )

    // ── Private helpers ───────────────────────────────────────────

    /**
     * Builds a smoothed behavior: Source → Damping → Target.
     * The damping node smooths the source signal over time.
     */
    private fun smoothed(
        source: BrushBehavior.Source,
        sourceStart: Float,
        sourceEnd: Float,
        target: BrushBehavior.Target,
        targetStart: Float,
        targetEnd: Float,
        dampingSeconds: Float
    ): List<BrushBehavior.Node> = listOf(
        sourceNode(source, sourceStart, sourceEnd),
        dampingNode(dampingSeconds),
        targetNode(target, targetStart, targetEnd)
    )

    /**
     * Builds a direct (un-damped) behavior: Source → Target.
     * The source signal maps directly to the target without smoothing.
     */
    private fun direct(
        source: BrushBehavior.Source,
        sourceStart: Float,
        sourceEnd: Float,
        target: BrushBehavior.Target,
        targetStart: Float,
        targetEnd: Float
    ): List<BrushBehavior.Node> = listOf(
        sourceNode(source, sourceStart, sourceEnd),
        targetNode(target, targetStart, targetEnd)
    )

    /**
     * Builds a noise-based jitter behavior: Noise → Target.
     * The noise source varies over distance for organic-feeling variation.
     */
    private fun jitter(
        target: BrushBehavior.Target,
        targetStart: Float,
        targetEnd: Float,
        basePeriod: Float
    ): List<BrushBehavior.Node> = listOf(
        noiseNode(basePeriod),
        targetNode(target, targetStart, targetEnd)
    )

    private fun sourceNode(
        source: BrushBehavior.Source,
        rangeStart: Float,
        rangeEnd: Float
    ): BrushBehavior.Node = BrushBehavior.Node.newBuilder().setSourceNode(
        BrushBehavior.SourceNode.newBuilder()
            .setSource(source)
            .setSourceValueRangeStart(rangeStart)
            .setSourceValueRangeEnd(rangeEnd)
            .setSourceOutOfRangeBehavior(BrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
    ).build()

    private fun dampingNode(dampingSeconds: Float): BrushBehavior.Node =
        BrushBehavior.Node.newBuilder().setDampingNode(
            BrushBehavior.DampingNode.newBuilder()
                .setDampingSource(
                    BrushBehavior.ProgressDomain.PROGRESS_DOMAIN_TIME_IN_SECONDS
                )
                .setDampingGap(dampingSeconds)
        ).build()

    private fun targetNode(
        target: BrushBehavior.Target,
        rangeStart: Float,
        rangeEnd: Float
    ): BrushBehavior.Node = BrushBehavior.Node.newBuilder().setTargetNode(
        BrushBehavior.TargetNode.newBuilder()
            .setTarget(target)
            .setTargetModifierRangeStart(rangeStart)
            .setTargetModifierRangeEnd(rangeEnd)
    ).build()

    private fun noiseNode(basePeriod: Float): BrushBehavior.Node =
        BrushBehavior.Node.newBuilder().setNoiseNode(
            BrushBehavior.NoiseNode.newBuilder()
                .setSeed(kotlin.random.Random.nextInt())
                .setVaryOver(
                    BrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_CENTIMETERS
                )
                .setBasePeriod(basePeriod)
        ).build()
}
