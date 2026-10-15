package cn.coostack.usefulmagic.items

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * 法杖里存的「法球」数据。
 *
 * ## 1.20.1 移植改动
 * 1. 上游用 1.20.5+ 的 `DataComponentPatch` → 改用 `CompoundTag`（NBT）
 * 2. `ItemStack.ITEM_NON_AIR_CODEC`（1.21）→ 1.20.1 用
 *    `BuiltInRegistries.ITEM.holderByNameCodec()`
 */
data class WandMagicData(
    val item: Holder<Item>,
    val count: Int,
    /** 1.20.1: 物品的 NBT（替代上游的 DataComponentPatch） */
    val tag: CompoundTag? = null,
) {
    companion object {
        val CODEC: Codec<WandMagicData> = RecordCodecBuilder.create { instance ->
            instance.group(

                BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id")
                    .forGetter(WandMagicData::item),
                ExtraCodecs.intRange(1, 99).fieldOf("count").orElse(1)
                    .forGetter(WandMagicData::count),
                CompoundTag.CODEC.optionalFieldOf("tag")
                    .forGetter { java.util.Optional.ofNullable(it.tag) },
            ).apply(instance) { item, count, tag ->
                WandMagicData(item, count, tag.orElse(null))
            }
        }

        fun fromStack(stack: ItemStack): WandMagicData? {
            if (stack.isEmpty) return null
            return WandMagicData(
                stack.itemHolder,
                stack.count,
                stack.tag?.copy(),
            )
        }
    }

    fun toStack(): ItemStack {
        val stack = ItemStack(item, count)
        if (tag != null) {
            stack.tag = tag.copy()
        }
        return stack
    }

    fun `is`(target: Item): Boolean = item.value() === target
}
