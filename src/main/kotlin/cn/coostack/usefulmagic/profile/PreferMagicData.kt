package cn.coostack.usefulmagic.profile

import cn.coostack.usefulmagic.compat.mc1201.compDbg
import cn.coostack.usefulmagic.compat.mc1201.getComponent
import cn.coostack.usefulmagic.items.UsefulMagicDataComponentTypes
import cn.coostack.usefulmagic.items.weapon.magic.MagicItem
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import kotlin.math.absoluteValue

/**
 * # 设计初衷
 * - 改为了法术分离之后 有些法杖其实没有存在的必要了（因为同级）
 * - 所以做了法术的偏好魔法， 让玩家有制作和使用这些法杖的欲望
 * # 偏好魔法
 * - 如果符合条件 那么就会有额外的加成
 * - 偏好程度 （最差 -5 最好 5） 如果偏好是负数，那就代表排斥
 * - 排斥会有Debuff
 * ## 加成列表
 * - 伤害加成 [damageFactor]
 * - 施法速度加成(需要时间减免) [usageReductionFactor]
 * - cd减免 [cdReductionFactor]
 * - 魔力减免 [manaReductionFactor]
 * 公式如下: final = (1 + level * preferFactor) * (base * wandFactor)
 */
data class PreferMagicData(
    val damageFactor: Double,
    val usageReductionFactor: Double,
    val cdReductionFactor: Double,
    val manaReductionFactor: Double,
    val preferItems: Map<Item, Int> = emptyMap()
) {
    companion object {
        /**
         * ★ 中性值（2026-09-30）：全部加成为 0、无偏好条目。
         *
         * 用于 `WAND_PREFER` 读不出来时兜底 —— 上游 1.21 里这个组件总是存在，
         * `?:` 分支永远走不到；但 1.20.1 的组件是 NBT 桩，一旦读取失败，
         * 后果是「tooltip 整段消失 / 蓄力 maxTick=-1 永不触发」这种级联灾难。
         * 兜底 + 告警，比整个功能挂掉好。
         */
        @JvmField
        val NEUTRAL = PreferMagicData(0.0, 0.0, 0.0, 0.0, emptyMap())

        @JvmStatic
        fun getPreferTranslateKey(level: Int): Component {
            return Component.translatable("item.usefulmagic.prefer.level${if (level >= 0) "$level" else "r${level.absoluteValue}"}")
        }

        /**
         * ★ 宽容解码（2026-09-30 修复）：原来只要 `prefer_items` 里有**一个** itemId
         * 在当前注册表里找不到，整个 `PreferMagicData` 就解码失败 → 法杖的
         * tooltip 全空 + 蓄力 maxTick=-1（永不释放）。
         * 现在改成：坏条目跳过并告警，其余照常。同时把失败原因打到日志。
         */
        private fun decodePreferItems(source: Map<ResourceLocation, Int>): DataResult<Map<Item, Int>> {
            val result = HashMap<Item, Int>(source.size)
            var bad = 0
            source.forEach { (itemId, level) ->
                val item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null)
                if (item == null) {
                    bad++
                    cn.coostack.usefulmagic.compat.mc1201.compDbg(
                        "prefer-bad-item",
                        "prefer_items 里的 itemId 解析不到，已跳过：$itemId (level=$level)"
                    )
                } else {
                    result[item] = level
                }
            }
            if (bad > 0) {
                cn.coostack.usefulmagic.compat.mc1201.compDbg(
                    "prefer-bad-total",
                    "本次共跳过 $bad/${source.size} 个坏条目（其余正常）"
                )
            }
            return DataResult.success(result)
        }

        private fun encodePreferItems(source: Map<Item, Int>): DataResult<Map<ResourceLocation, Int>> {
            val result = HashMap<ResourceLocation, Int>(source.size)
            source.forEach { (item, level) ->
                val itemId = BuiltInRegistries.ITEM.getKey(item)
                result[itemId] = level
            }
            return DataResult.success(result)
        }

        private val PREFER_ITEMS_CODEC: Codec<Map<Item, Int>> =
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).flatXmap(
                ::decodePreferItems,
                ::encodePreferItems
            )

        val CODEC: Codec<PreferMagicData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.DOUBLE.fieldOf("damage_factor").forGetter { it.damageFactor },
                Codec.DOUBLE.fieldOf("usage_factor").forGetter { it.usageReductionFactor },
                Codec.DOUBLE.fieldOf("cd_reduction_factor").forGetter { it.cdReductionFactor },
                Codec.DOUBLE.fieldOf("mana_reduction_factor").forGetter { it.manaReductionFactor },
                PREFER_ITEMS_CODEC.optionalFieldOf("prefer_items", emptyMap<Item, Int>())
                    .forGetter(PreferMagicData::preferItems)
            ).apply(instance, ::PreferMagicData)
        }

        /**
         * ★ 读取法杖的偏好数据；读不出来时返回 [NEUTRAL] 并告警（2026-09-30）。
         * 避免上游那种 `?: return` 把整条 tooltip / 蓄力链一起干掉。
         */
        @JvmStatic
        fun of(wand: ItemStack, site: String): PreferMagicData {
            val v = wand.getComponent(UsefulMagicDataComponentTypes.WAND_PREFER.get())
            if (v == null) {
                compDbg(
                    "prefer-null/$site",
                    "WAND_PREFER 读不到 → 用中性值兜底 (原始 NBT=${wand.tag})"
                )
                return NEUTRAL
            }
            return v
        }
    }

    fun isPreferItem(item: Item): Boolean {
        return preferItems.containsKey(item)
    }

    fun getPreferLevel(item: Item) = preferItems[item] ?: 0
    fun getPreferLevel(item: ItemStack) = preferItems[item.item] ?: 0

    fun putPrefer(item: Item, level: Int): PreferMagicData {
        return copy(preferItems = preferItems + (item to level))
    }

    fun getDamageFactor(item: ItemStack) = getDamageFactor(item.item)

    fun getUsageFactor(item: ItemStack) = getUsageFactor(item.item)

    fun getCdReductionFactor(item: ItemStack) = getCdReductionFactor(item.item)

    fun getManaReductionFactor(item: ItemStack) = getManaReductionFactor(item.item)

    fun getDamageFactor(item: Item): Double {
        val level = preferItems[item] ?: 0
        return damageFactor * level
    }

    fun getUsageFactor(item: Item): Double {
        val level = preferItems[item] ?: 0
        return usageReductionFactor * level
    }

    fun getCdReductionFactor(item: Item): Double {
        val level = preferItems[item] ?: 0
        return cdReductionFactor * level
    }

    fun getManaReductionFactor(item: Item): Double {
        val level = preferItems[item] ?: 0
        return manaReductionFactor * level
    }
}
