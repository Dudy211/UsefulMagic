package cn.coostack.usefulmagic.gui.guildbook.widget.button

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.sounds.SoundManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents

/**
 * 1.20.1 移植改动：
 * 上游基于 1.20.5+ 的 `ImageButton`（用 `WidgetSprites` 装「普通+悬停」两张纹理）。
 * 1.20.1 的 `ImageButton` 只支持**一张**纹理 + `yDiffTex`（悬停时纹理向下偏移）。
 *
 * 这里改成**自定义 Widget**，直接持有两张纹理，悬停时切换。
 *
 * 上游调用形式：
 * ```
 * TextureButton(x, y, w, h, WidgetSprites(NORMAL, HOVER)) { ... }
 * ```
 * 移植后改为传两个 ResourceLocation。
 */
class TextureButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val normalTexture: ResourceLocation,
    private val hoveredTexture: ResourceLocation,
    pressAction: Button.OnPress,
) : Button(x, y, width, height, net.minecraft.network.chat.Component.empty(), pressAction, Button.DEFAULT_NARRATION) {

    var u = 0f
    var v = 0f
    var clickSound: SoundEvent = SoundEvents.UI_BUTTON_CLICK.value()
    var pitch = 1f
    var textureWidth = width
    var textureHeight = height

    /** 兼容上游的 WidgetSprites 调用（用 Pair 装两张纹理） */
    constructor(
        x: Int, y: Int, width: Int, height: Int,
        sprites: Pair<ResourceLocation, ResourceLocation>,
        pressAction: Button.OnPress,
    ) : this(x, y, width, height, sprites.first, sprites.second, pressAction)

    /** 兼容上游的 WidgetSprites 调用 */
    constructor(
        x: Int, y: Int, width: Int, height: Int,
        sprites: cn.coostack.usefulmagic.compat.mc1201.WidgetSprites,
        pressAction: Button.OnPress,
    ) : this(x, y, width, height, sprites.normal, sprites.hovered, pressAction)

    override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val tex = if (this.isHoveredOrFocused) hoveredTexture else normalTexture
        context.blit(tex, this.x, this.y, u, v, width, height, textureWidth, textureHeight)
    }

    override fun playDownSound(soundManager: SoundManager) {
        soundManager.play(SimpleSoundInstance.forUI(clickSound, pitch))
    }
}
