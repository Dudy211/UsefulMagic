package cn.coostack.usefulmagic.compat.mc1201

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.item.ItemStack

/**
 * # 1.20.1 兼容层：ItemContainerContents
 *
 * 1.20.5+ 用 `ItemContainerContents`（一个数据组件）来存「物品袋」里的内容。
 * 1.20.1 没有这套，只能自己存 NBT：`ListTag<CompoundTag>`。
 *
 * 这里提供同形的 API，让 UM 的代码能用同样写法：
 * ```
 * ItemContainerContents.fromItems(list)         // 打包
 * contents.nonEmptyStream()                     // 读取（流式）
 * ```
 */
class ItemContainerContentsCompat internal constructor(
    private val items: List<ItemStack>,
) {
    /** 1.20.5+ 的 `nonEmptyStream()` */
    fun nonEmptyStream(): java.util.stream.Stream<ItemStack> =
        items.stream().filter { !it.isEmpty }

    /** 转回列表 */
    fun items(): List<ItemStack> = items

    /** 序列化到 NBT（存 ItemStack 列表） */
    fun toTag(): ListTag {
        val list = ListTag()
        items.forEach { stack ->
            if (!stack.isEmpty) {
                val t = CompoundTag()
                stack.save(t)
                list.add(t)
            }
        }
        return list
    }

    companion object {
        @JvmField
        val EMPTY = ItemContainerContentsCompat(emptyList())

        /** 1.20.5+ 的 `ItemContainerContents.fromItems(list)` */
        @JvmStatic
        fun fromItems(list: List<ItemStack>): ItemContainerContentsCompat =
            ItemContainerContentsCompat(list)

        /** 从 NBT 还原 */
        @JvmStatic
        fun fromTag(tag: ListTag?): ItemContainerContentsCompat {
            if (tag == null) return EMPTY
            val out = ArrayList<ItemStack>(tag.size)
            for (i in 0 until tag.size) {
                val t = tag.getCompound(i)
                ItemStack.of(t).let { if (!it.isEmpty) out.add(it) }
            }
            return ItemContainerContentsCompat(out)
        }
    }
}

/**
 * 让 `ItemContainerContents.fromItems(...)` 的写法可用。
 *
 * ⚠️ 我把它命名为和上游相同的 `ItemContainerContents`（通过 typealias），
 * 这样调用点零改动。
 */
typealias ItemContainerContents = ItemContainerContentsCompat
