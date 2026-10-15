package cn.coostack.usefulmagic.compat.mc1201

import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * # 1.20.1 兼容层：RecipeInput
 *
 * ## 差异
 * | | 1.21.1 | 1.20.1 |
 * |---|---|---|
 * | 配方输入 | `interface RecipeInput { getItem(i); size() }` | `Container`（方法更多） |
 *
 * ## 方案
 * `RecipeInput` 继承 `Container`，并把 `Container` 的抽象方法给出默认实现。
 * 这样 UM 的输入类只需实现 `getItem`/`size()` 两个方法即可。
 */
interface RecipeInput : Container {

    fun getItemAt(slot: Int): ItemStack
    fun size(): Int



    override fun getContainerSize(): Int = size()

    override fun getItem(slot: Int): ItemStack = getItemAt(slot)

    override fun isEmpty(): Boolean {
        for (i in 0 until size()) {
            if (!getItemAt(i).isEmpty) return false
        }
        return true
    }

    override fun removeItem(slot: Int, amount: Int): ItemStack = ItemStack.EMPTY

    override fun removeItemNoUpdate(slot: Int): ItemStack = ItemStack.EMPTY

    override fun setItem(slot: Int, stack: ItemStack) {}

    override fun setChanged() {}

    override fun stillValid(player: Player): Boolean = true

    override fun clearContent() {}
}
