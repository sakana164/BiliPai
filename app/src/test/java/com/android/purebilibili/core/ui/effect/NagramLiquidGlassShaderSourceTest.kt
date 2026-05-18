package com.android.purebilibili.core.ui.effect

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NagramLiquidGlassShaderSourceTest {

    @Test
    fun `shader keeps nagramx license notice and adds dual-rect uniforms`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/effect/LiquidGlassShader.kt"
        )
        val thirdPartyNotice = loadSource(
            "docs/third_party/NagramX-LiquidGlass.md"
        )

        // GPL-3.0 / NagramX source notice must stay (license requirement, not optional).
        assertTrue(source.contains("risin42/NagramX"))
        assertTrue(source.contains("TMessagesProj/src/main/res/raw/liquid_glass_shader.agsl"))
        assertTrue(source.contains("GPL-3.0"))
        assertTrue(thirdPartyNotice.contains("risin42/NagramX"))
        assertTrue(thirdPartyNotice.contains("GPL-3.0"))
        assertTrue(thirdPartyNotice.contains("liquid_glass_shader.agsl"))

        // Rect 1 (panel) uniforms from NagramX.
        assertTrue(source.contains("uniform float2 center;"))
        assertTrue(source.contains("uniform float2 size;"))
        assertTrue(source.contains("uniform float4 radius;"))

        // Rect 2 (sliding capsule) uniforms — single-pass dual-rect extension.
        assertTrue(source.contains("uniform float enable2;"))
        assertTrue(source.contains("uniform float2 center2;"))
        assertTrue(source.contains("uniform float2 size2;"))
        assertTrue(source.contains("uniform float4 radius2;"))
        assertTrue(source.contains("uniform float thickness2;"))
        assertTrue(source.contains("uniform float refract_intensity2;"))

        // No BiliPai-only uniforms that NagramX never had.
        assertFalse(source.contains("aberration_strength"))
        assertFalse(source.contains("specular_alpha"))
        assertFalse(source.contains("light_dir"))
        assertFalse(source.contains("background_color"))
    }

    @Test
    fun `transparent bottom bar branch applies dual-rect nagramx shader modifier`() {
        val bottomBarSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt"
        )
        val transparentPresetSource = bottomBarSource
            .substringAfter("BottomBarLiquidGlassPreset.BACKDROP_NATIVE -> drawBackdrop(")
            .substringBefore("BottomBarLiquidGlassPreset.BILIPAI_TUNED -> drawBackdrop(")
        val modifierSource = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/effect/LiquidGlassModifier.kt"
        )

        assertTrue(transparentPresetSource.contains(".nagramLiquidGlass("))
        assertTrue(modifierSource.contains("fun Modifier.nagramLiquidGlass("))
        // 通透玻璃面板必须渲染成玻璃材质：轻度模糊 + vibrancy。
        assertTrue(transparentPresetSource.contains("vibrancy()"))
        assertTrue(transparentPresetSource.contains("blur("))
        // 折射全部交给双矩形着色器，面板不再叠加 AndroidLiquidGlass lens（避免双重折射）。
        assertFalse(transparentPresetSource.contains("lens("))
        // Modifier feeds the sliding-capsule rect into the shader.
        assertTrue(modifierSource.contains("setFloatUniform(\"enable2\""))
        assertTrue(modifierSource.contains("setFloatUniform(\"center2\""))
        assertFalse(modifierSource.contains("setFloatUniform(\"aberration_strength\""))
        assertFalse(modifierSource.contains("setFloatUniform(\"specular_alpha\""))
        assertFalse(modifierSource.contains("setFloatUniform(\"light_dir\""))
        assertFalse(modifierSource.contains("setFloatUniform(\"background_color\""))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath),
            File("../$path")
        ).firstOrNull { it.exists() } ?: error("Source file not found: $path")
        return sourceFile.readText()
    }
}
