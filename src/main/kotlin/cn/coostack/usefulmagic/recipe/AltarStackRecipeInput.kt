package cn.coostack.usefulmagic.recipe

import net.minecraft.world.item.ItemStack
import cn.coostack.usefulmagic.compat.mc1201.RecipeInput
class AltarStackRecipeInput(
    val roundItems: List<ItemStack>,
    val centerItem: ItemStack,
    var mana: Int = 0
) : RecipeInput {

    override fun getItemAt(slot: Int): ItemStack {
        return if (slot in roundItems.indices) {
            roundItems[slot]
        } else {
            ItemStack.EMPTY
        }
    }

    override fun size(): Int {
        return 8
    }
}
