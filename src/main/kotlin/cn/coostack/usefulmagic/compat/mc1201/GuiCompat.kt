package cn.coostack.usefulmagic.compat.mc1201

import net.minecraft.resources.ResourceLocation

/**
 * # 1.20.1 兼容层：WidgetSprites
 *
 * 1.20.5+ 的 `WidgetSprites`：一个「普通态 + 悬停态」纹理对，
 * 配 `ImageButton` / `SpriteIconButton` 用。
 * 1.20.1 没有这套（`ImageButton` 只吃单张纹理 + yDiffTex）。
 *
 * 这里提供同形 API，是 `Pair<ResourceLocation, ResourceLocation>` 的包装。
 */
class WidgetSprites(
    val normal: ResourceLocation,
    val hovered: ResourceLocation,
    val disabled: ResourceLocation? = normal,
    val disabledHovered: ResourceLocation? = normal,
) {
    /** 1.20.5+ 的 `get(active, hovered)` */
    fun get(active: Boolean, hovered: Boolean): ResourceLocation = when {
        !active -> disabled ?: normal
        hovered -> this.hovered
        else -> normal
    }

    /** 拆成 Pair 供 1.20.1 的 Button 用 */
    fun asPair(): Pair<ResourceLocation, ResourceLocation> = normal to hovered
}
