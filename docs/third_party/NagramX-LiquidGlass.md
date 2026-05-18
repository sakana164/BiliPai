# NagramX Liquid Glass 源码致谢

- 来源项目：[`risin42/NagramX`](https://github.com/risin42/NagramX)
- 来源文件：`TMessagesProj/src/main/res/raw/liquid_glass_shader.agsl`
- 接入参考：`TMessagesProj/src/main/java/org/telegram/ui/Components/blur3/LiquidGlassEffect.java`
- 许可证：GPL-3.0

BiliPai 的通透底栏液态玻璃**基于 NagramX 的 AGSL shader 衍生改写**：在其单矩形折射
的基础上扩展为单 Pass 双矩形（矩形 1 为底栏面板，矩形 2 为滑动选中胶囊），并在
Compose/Backdrop 管线里复用其 uniform 语义完成接入。BiliPai 保持 GPL-3.0 开源分发，
衍生改写许可证兼容，感谢 NagramX 及 Telegram Android 对 Android 端 Liquid Glass 折射
实现的开源贡献。
