package cn.coostack.usefulmagic.client.tooltip

import cn.coostack.usefulmagic.items.prop.SpellBagTooltip
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.core.NonNullList
import net.minecraft.world.item.ItemStack

/**
 * 物品袋（SpellBag）的 tooltip。
 *
 * 1.20.1 没有 `GuiGraphics.blitSprite`（那是 1.20.2+ 的），
 * 所以这里复用原版的 [ClientBundleTooltip] 来画「袋子里的物品」网格 ——
 * 图标 / 背景 / 高亮都由原版负责，视觉上也和收纳袋一致。
 */
class SpellBagClientTooltip(private val tooltip: SpellBagTooltip) : ClientTooltipComponent {

    private val items: NonNullList<ItemStack> =
        if (tooltip.contents.isEmpty()) {
            NonNullList.withSize(0, ItemStack.EMPTY)
        } else {
            NonNullList.of(ItemStack.EMPTY, *tooltip.contents.toTypedArray())
        }

    private val delegate: ClientBundleTooltip = ClientBundleTooltip(
        net.minecraft.world.inventory.tooltip.BundleTooltip(items, 0)
    )

    override fun getHeight(): Int = delegate.getHeight()

    override fun getWidth(font: Font): Int = delegate.getWidth(font)

    override fun renderImage(font: Font, x: Int, y: Int, guiGraphics: GuiGraphics) =
        delegate.renderImage(font, x, y, guiGraphics)
}
