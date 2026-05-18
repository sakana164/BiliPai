package com.android.purebilibili.core.ui.effect

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 在标签间滑动的选中胶囊玻璃矩形（双矩形着色器的矩形 2）。
 *
 * 全部为像素值，坐标相对于应用了 [nagramLiquidGlass] 的元素。
 *
 * @param center 胶囊中心。
 * @param size 胶囊整体宽高（非半尺寸）。
 * @param cornerRadius 胶囊圆角半径（像素）。胶囊形传 height / 2。
 * @param thickness 折射边缘厚度（像素）。
 * @param refractIntensity 胶囊折射强度。
 * @param foregroundColor 胶囊 tint，叠在面板之上、仅胶囊矩形内部生效。
 */
data class CapsuleGlassRect(
    val center: Offset,
    val size: Size,
    val cornerRadius: Float,
    val thickness: Float,
    val refractIntensity: Float,
    val foregroundColor: Color = Color.Transparent
)

@Stable
internal data class NagramLiquidGlassUniformState(
    val width: Float,
    val height: Float,
    val centerX: Float,
    val centerY: Float,
    val sizeX: Float,
    val sizeY: Float,
    val radius: Float,
    val thickness: Float,
    val refractIndex: Float,
    val refractIntensity: Float,
    val foregroundColorArgb: Int,
    val capsuleEnabled: Boolean,
    val capsuleCenterX: Float,
    val capsuleCenterY: Float,
    val capsuleSizeX: Float,
    val capsuleSizeY: Float,
    val capsuleRadius: Float,
    val capsuleThickness: Float,
    val capsuleRefractIntensity: Float,
    val capsuleForegroundColorArgb: Int
)

internal fun shouldUpdateNagramLiquidGlassUniforms(
    previous: NagramLiquidGlassUniformState?,
    next: NagramLiquidGlassUniformState,
    threshold: Float = 0.1f
): Boolean {
    if (previous == null) return true
    if (previous.foregroundColorArgb != next.foregroundColorArgb) return true
    if (previous.capsuleEnabled != next.capsuleEnabled) return true
    if (previous.capsuleForegroundColorArgb != next.capsuleForegroundColorArgb) return true
    fun changed(oldValue: Float, newValue: Float): Boolean =
        kotlin.math.abs(oldValue - newValue) > threshold

    return changed(previous.width, next.width) ||
        changed(previous.height, next.height) ||
        changed(previous.centerX, next.centerX) ||
        changed(previous.centerY, next.centerY) ||
        changed(previous.sizeX, next.sizeX) ||
        changed(previous.sizeY, next.sizeY) ||
        changed(previous.radius, next.radius) ||
        changed(previous.thickness, next.thickness) ||
        changed(previous.refractIndex, next.refractIndex) ||
        changed(previous.refractIntensity, next.refractIntensity) ||
        changed(previous.capsuleCenterX, next.capsuleCenterX) ||
        changed(previous.capsuleCenterY, next.capsuleCenterY) ||
        changed(previous.capsuleSizeX, next.capsuleSizeX) ||
        changed(previous.capsuleSizeY, next.capsuleSizeY) ||
        changed(previous.capsuleRadius, next.capsuleRadius) ||
        changed(previous.capsuleThickness, next.capsuleThickness) ||
        changed(previous.capsuleRefractIntensity, next.capsuleRefractIntensity)
}

private class NagramLiquidGlassRenderCache {
    var uniformState: NagramLiquidGlassUniformState? = null
    var renderEffect: androidx.compose.ui.graphics.RenderEffect? = null
}

/**
 * NagramX 衍生 Liquid Glass shader 的 Compose 接入层（单 Pass 双矩形）。
 *
 * 矩形 1 是整个元素（底栏面板）；矩形 2 是可选的滑动选中胶囊，通过
 * [capsuleProvider] 在 graphicsLayer 绘制块内读取——胶囊滑动只触发重绘、不触发
 * 重组，参照 [liquidGlassBackground] 的 scrollOffsetProvider 写法。
 *
 * RuntimeShader 用 remember 持有，不再每帧重新编译 AGSL。
 *
 * 着色器源码见 [LiquidGlassShader]（基于 NagramX 衍生，GPL-3.0）。
 *
 * @param radius 面板圆角（Dp）。
 * @param refractIndex 折射率，面板与胶囊共用。
 * @param refractIntensity 面板折射强度。
 * @param thickness 面板折射边缘厚度。
 * @param foregroundColor 面板 tint。
 * @param capsuleProvider 给定底栏图层尺寸（像素），返回当前帧的滑动胶囊矩形；
 *   返回 null 表示本帧不绘制胶囊。
 */
fun Modifier.nagramLiquidGlass(
    radius: Dp,
    refractIndex: Float = 1.5f,
    refractIntensity: Float = 0.75f,
    thickness: Dp = 11.dp,
    foregroundColor: Color = Color.Transparent,
    capsuleProvider: (Size) -> CapsuleGlassRect? = { null }
): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        this
    } else {
        val density = LocalDensity.current
        val shader = remember { RuntimeShader(LiquidGlassShader.SHADER) }
        val renderCache = remember { NagramLiquidGlassRenderCache() }
        this.graphicsLayer {
            val radiusPx = with(density) { radius.toPx() }
            val thicknessPx = with(density) { thickness.toPx() }
            val width = size.width.coerceAtLeast(1f)
            val height = size.height.coerceAtLeast(1f)
            val resolvedRadius = radiusPx.coerceAtMost(minOf(width, height) / 2f)

            // 矩形 2：滑动选中胶囊。
            val capsule = capsuleProvider(size)
            val capsuleHalfW = (capsule?.size?.width?.div(2f) ?: 0f).coerceAtLeast(0f)
            val capsuleHalfH = (capsule?.size?.height?.div(2f) ?: 0f).coerceAtLeast(0f)
            val capsuleRadius = capsule?.cornerRadius
                ?.coerceIn(0f, minOf(capsuleHalfW, capsuleHalfH))
                ?: 0f
            val capsuleThickness = capsule?.thickness?.coerceAtLeast(0.01f) ?: 1f
            val uniformState = NagramLiquidGlassUniformState(
                width = width,
                height = height,
                centerX = width / 2f,
                centerY = height / 2f,
                sizeX = width / 2f,
                sizeY = height / 2f,
                radius = resolvedRadius,
                thickness = thicknessPx,
                refractIndex = refractIndex,
                refractIntensity = refractIntensity,
                foregroundColorArgb = foregroundColor.toArgb(),
                capsuleEnabled = capsule != null,
                capsuleCenterX = capsule?.center?.x ?: 0f,
                capsuleCenterY = capsule?.center?.y ?: 0f,
                capsuleSizeX = capsuleHalfW,
                capsuleSizeY = capsuleHalfH,
                capsuleRadius = capsuleRadius,
                capsuleThickness = capsuleThickness,
                capsuleRefractIntensity = capsule?.refractIntensity ?: 0f,
                capsuleForegroundColorArgb = capsule?.foregroundColor?.toArgb() ?: Color.Transparent.toArgb()
            )

            if (shouldUpdateNagramLiquidGlassUniforms(renderCache.uniformState, uniformState)) {
                // NagramX 同级脏检查：参数变化足够大时才写 uniform 并重建 RenderEffect。
                renderCache.uniformState = uniformState
                shader.setFloatUniform("resolution", width, height)
                shader.setFloatUniform("center", width / 2f, height / 2f)
                shader.setFloatUniform("size", width / 2f, height / 2f)
                shader.setFloatUniform(
                    "radius",
                    resolvedRadius,
                    resolvedRadius,
                    resolvedRadius,
                    resolvedRadius
                )
                shader.setFloatUniform("thickness", thicknessPx)
                shader.setFloatUniform("refract_index", refractIndex)
                shader.setFloatUniform("refract_intensity", refractIntensity)
                setPremultipliedColorUniform(shader, "foreground_color_premultiplied", foregroundColor)
                shader.setFloatUniform("enable2", if (capsule != null) 1f else 0f)
                shader.setFloatUniform("center2", uniformState.capsuleCenterX, uniformState.capsuleCenterY)
                shader.setFloatUniform("size2", capsuleHalfW, capsuleHalfH)
                shader.setFloatUniform(
                    "radius2",
                    capsuleRadius,
                    capsuleRadius,
                    capsuleRadius,
                    capsuleRadius
                )
                shader.setFloatUniform("thickness2", capsuleThickness)
                shader.setFloatUniform("refract_intensity2", uniformState.capsuleRefractIntensity)
                setPremultipliedColorUniform(
                    shader,
                    "foreground2_color_premultiplied",
                    capsule?.foregroundColor ?: Color.Transparent
                )
                renderCache.renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "img")
                    .asComposeRenderEffect()
            }

            renderEffect = renderCache.renderEffect
        }
    }
}

private fun setPremultipliedColorUniform(
    shader: RuntimeShader,
    name: String,
    color: Color
) {
    val argb = color.toArgb()
    val a = android.graphics.Color.alpha(argb) / 255f
    val r = android.graphics.Color.red(argb) / 255f * a
    val g = android.graphics.Color.green(argb) / 255f * a
    val b = android.graphics.Color.blue(argb) / 255f * a
    shader.setFloatUniform(name, r, g, b, a)
}
