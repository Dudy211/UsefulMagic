package cn.coostack.usefulmagic

import cn.coostack.usefulmagic.compat.mc1201.getComponent
import cn.coostack.usefulmagic.items.UsefulMagicDataComponentTypes
import cn.coostack.usefulmagic.items.WandMagicData
import net.minecraft.world.item.ItemStack

/**
 * 给 Java（mixin）侧用的桥。
 *
 * Kotlin 顶层扩展函数在 Java 里要写 `DataComponentsKt.getComponent(...)`，
 * 而 `depend/` 里的 CooParticlesAPI jar 是旧版（没有 getComponent），
 * 所以在 UM 自己的 Kotlin 里包一层，Java 直接调本类。
 */
object JavaCompat {

    /** 取法杖上装的法球数据（1.21 的 stack.get(WAND_MAGIC)） */
    @JvmStatic
    fun wandMagic(stack: ItemStack): WandMagicData? =
        stack.getComponent(UsefulMagicDataComponentTypes.WAND_MAGIC.get())
}
