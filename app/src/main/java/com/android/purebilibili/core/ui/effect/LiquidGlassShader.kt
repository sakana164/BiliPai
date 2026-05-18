package com.android.purebilibili.core.ui.effect

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import org.intellij.lang.annotations.Language

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object LiquidGlassShader {

    /**
     * 基于 NagramX 的 Liquid Glass AGSL shader 衍生改写。
     *
     * 原始来源：NagramX
     * https://github.com/risin42/NagramX/blob/dev/TMessagesProj/src/main/res/raw/liquid_glass_shader.agsl
     * 接入参考：
     * https://github.com/risin42/NagramX/blob/dev/TMessagesProj/src/main/java/org/telegram/ui/Components/blur3/LiquidGlassEffect.java
     *
     * 许可证：GPL-3.0。BiliPai 同为 GPL-3.0 项目，衍生改写许可证兼容。
     * 致谢：NagramX / Telegram Android 对 Android 端 Liquid Glass 折射实现的开源贡献。
     *
     * 在 NagramX 原版单矩形折射的基础上扩展为单 Pass 双矩形：矩形 1 是底栏面板，
     * 矩形 2 是在标签间滑动的选中胶囊。两个矩形的折射偏移在同一 Pass 内累加，最后
     * 只对背景采样一次，避免链式 RenderEffect 的二次重采样发虚。
     */
    @Language("AGSL")
    const val SHADER = """
        uniform shader img;

        uniform float2 resolution;
        uniform float2 center;
        uniform float2 size;
        uniform float4 radius;
        uniform float thickness;
        uniform float refract_index;
        uniform float refract_intensity;
        uniform float4 foreground_color_premultiplied;

        // 矩形 2：滑动选中胶囊（单 Pass 双矩形扩展）。
        uniform float enable2;
        uniform float2 center2;
        uniform float2 size2;
        uniform float4 radius2;
        uniform float thickness2;
        uniform float refract_intensity2;
        uniform float4 foreground2_color_premultiplied;

        half sdfRect(half2 p, half4 r, half2 halfSize) {
          r.xy = (p.x > 0.0) ? r.xy : r.zw;
          r.x  = (p.y > 0.0) ? r.x  : r.y;
          half2 q = abs(p) - halfSize + r.x;
          return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r.x;
        }

        half4 srcOver(half4 src, half4 dst) {
            half3 outRGB = (src.rgb + dst.rgb * (1.0 - src.a));
            float outA = src.a + (1.0 - src.a) * dst.a;
            return half4(outRGB, outA);
        }

        // 圆角矩形透镜的折射 UV 偏移；矩形外返回零偏移。
        half2 refractOffset(
            half2 fragCoord, half2 c, half2 halfSize, half4 r,
            half thick, half idx, half intensity
        ) {
          half2 p = fragCoord - c;
          half sd = sdfRect(p, r, halfSize);
          if (sd >= 0.0) {
            return half2(0.0, 0.0);
          }
          half sdX = sdfRect(p + half2(1.0, 0.0), r, halfSize);
          half sdY = sdfRect(p + half2(0.0, 1.0), r, halfSize);

          half n_cos = max(thick + sd, 0.0) / thick;
          half n_cos2 = n_cos * n_cos;
          half n_sin = sqrt(1.0 - n_cos2);
          half3 normal = normalize(half3((sdX - sd) * n_cos, (sdY - sd) * n_cos, n_sin));

          half3 refract_vec = refract(half3(0.0, 0.0, -1.0), normal, 1.0 / idx);
          half h = sd < -thick ? thick : sqrt(sd * (-2.0 * thick - sd));
          half refract_length = (h + 8.0 * thick) / -refract_vec.z;

          return refract_vec.xy * refract_length * intensity;
        }

        half4 main(in float2 fragCoord) {
          half2 fc = half2(fragCoord);
          half2 uv = fc;

          // 矩形 1：底栏面板。
          uv += refractOffset(
              fc, half2(center), half2(size), half4(radius),
              thickness, refract_index, refract_intensity
          );

          // 矩形 2：滑动胶囊，折射偏移叠加在面板之上。
          if (enable2 > 0.5) {
            uv += refractOffset(
                fc, half2(center2), half2(size2), half4(radius2),
                thickness2, refract_index, refract_intensity2
            );
          }

          half4 outColor = srcOver(half4(foreground_color_premultiplied), img.eval(uv));

          // 胶囊 tint 叠在面板 tint 之上，仅限胶囊矩形内部。
          if (enable2 > 0.5) {
            half sd2 = sdfRect(fc - half2(center2), half4(radius2), half2(size2));
            if (sd2 < 0.0) {
              outColor = srcOver(half4(foreground2_color_premultiplied), outColor);
            }
          }
          return outColor;
        }
    """
}
