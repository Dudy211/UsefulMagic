package cn.coostack.usefulmagic.compat.mc1201

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.IdentityHashMap

/**
 * # 数据组件桥接
 *
 * ```
 * Item.Properties().component(type, value)   → 记进 pendingComponents
 *        ↓  Item.<init>(Properties)   [ItemPropertiesMixin 触发]
 *   绑定到该 Item（itemComponents）
 *        ↓  new ItemStack(item)       [ItemStackMixin 触发]
 *   写进 ItemStack 的 NBT
 * ```
 */
object DataComponentBridge {

    /** Item → 已绑定的组件值 */
    @JvmStatic
    val itemComponents: IdentityHashMap<Item, List<Pair<UMDataComponentType<*>, Any?>>> =
        IdentityHashMap()

    /**
     * Item 构造完成时调用（mixin）。
     *
     * 直接接收 `Properties` 参数 —— 不用猜，可靠。
     */
    @JvmStatic
    fun bindToItem(item: Item, props: Item.Properties) {
        val list = takePendingComponents(props)
        if (list.isEmpty()) return
        itemComponents[item] = list
    }

    /**
     * ItemStack 创建时调用（mixin）。
     *
     * ⚠️ **只在 NBT 里没有该键时才写**，避免覆盖玩家改过的值（如升级后的魔力等级）。
     */
    @JvmStatic
    fun applyToStack(stack: ItemStack) {
        if (stack.isEmpty) return
        val list = itemComponents[stack.item] ?: return
        if (list.isEmpty()) return

        list.forEach { (type, value) ->
            if (value == null) return@forEach
            val key = type as? UMComponentKey<*> ?: return@forEach

            if (stack.tag?.contains(key.id) == true) return@forEach

            @Suppress("UNCHECKED_CAST")
            (key as UMComponentKey<Any?>).write(stack, value)
        }
    }

    /** 调试：某 Item 绑定了几项组件 */
    @JvmStatic
    fun componentCount(item: Item): Int = itemComponents[item]?.size ?: 0

    /** 清空（测试用） */
    @JvmStatic
    fun clear() {
        itemComponents.clear()
    }
}
