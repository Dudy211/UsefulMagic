package cn.coostack.usefulmagic.recipe

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.minecraft.core.NonNullList
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.crafting.Ingredient
import java.util.LinkedHashMap

/**
 * 配方环
 * 5 6 7
 * 3   4
 * 0 1 2
 */
class RoundShapeRecipe(
    val ingredients: NonNullList<Ingredient> = NonNullList.withSize(
        8,
        Ingredient.EMPTY
    )
) {
    companion object {
        /** 空配方（1.20.1 新增，供 AltarRecipeType.fromJson 兜底） */
        @JvmField
        val EMPTY = RoundShapeRecipe()

        @JvmStatic
        private fun fromData(data: IndexData): DataResult<RoundShapeRecipe> {
            val ingredients = NonNullList.withSize(8, Ingredient.EMPTY)
            data.map.forEach {
                val key = it.key.toIntOrNull() ?: return DataResult.error { "key type is not int" }
                if (key !in 0..7) return DataResult.error { "index must in 0 - 7" }
                ingredients[key] = it.value
            }
            return DataResult.success(RoundShapeRecipe(ingredients))
        }

        @JvmStatic
        val CODEC: Codec<RoundShapeRecipe> = IndexData.CODEC.flatXmap<RoundShapeRecipe>(
            ::fromData
        ) { it.toData() }
        /** 1.20.1 的网络写出（对应 1.20.5+ 的 PACKET_CODEC.encode） */
        @JvmStatic
        fun toNetwork(buf: FriendlyByteBuf, recipe: RoundShapeRecipe) {
            recipe.ingredients.forEach {
                it.toNetwork(buf)
            }
        }

        /** 1.20.1 的网络读取（对应 1.20.5+ 的 PACKET_CODEC.decode） */
        @JvmStatic
        fun fromNetwork(buf: FriendlyByteBuf): RoundShapeRecipe {
            val ingredients = NonNullList.withSize(8, Ingredient.EMPTY)
            for (i in 0..7) {
                ingredients[i] = Ingredient.fromNetwork(buf)
            }
            return RoundShapeRecipe(ingredients)
        }
    }

    val data: IndexData
        get() {
            val map = LinkedHashMap<String, Ingredient>(ingredients.size)
            ingredients.forEachIndexed { index, ingredient ->
                if (!ingredient.isEmpty) {
                    map[index.toString()] = ingredient
                }
            }
            return IndexData(map)
        }

    private fun toData(): DataResult<IndexData> {
        return if (data.map.isNotEmpty()) {
            DataResult.success(data)
        } else {
            DataResult.error { "recipe ingredients must contain at least one non-empty slot" }
        }
    }

    fun matchers(input: AltarStackRecipeInput): Boolean {
        val rotates = getRotates()
        rotates.forEach {
            it.forEachIndexed { inputIndex, i ->


                val stack = input.getItem(inputIndex)
                if (!ingredients[i].test(stack)) {
                    return@forEach
                }
            }
            return true
        }
        return false
    }

    private fun getRotates(): Set<List<Int>> {
        return setOf(


            listOf(0, 1, 2, 3, 4, 5, 6, 7),
            listOf(2, 3, 4, 5, 6, 7, 0, 1),
            listOf(4, 5, 6, 7, 0, 1, 2, 3),
            listOf(6, 7, 0, 1, 2, 3, 4, 5),
            listOf(0, 7, 6, 5, 4, 3, 2, 1),
            listOf(6, 5, 4, 3, 2, 1, 0, 7),
            listOf(4, 3, 2, 1, 0, 7, 6, 5),
            listOf(2, 1, 0, 7, 6, 5, 4, 3),
        )
    }

    /**
     * 由int判断ingredients索引
     */
    class IndexData(val map: LinkedHashMap<String, Ingredient>) {
        companion object {
            val CODEC: Codec<IndexData> = Codec.unboundedMap(Codec.STRING, IngredientCodec).xmap(
                { map -> IndexData(LinkedHashMap(map)) },
                { indexData -> indexData.map }
            )
        }
    }
}

/**
 * 1.20.1 兼容：Ingredient 的占位 Codec。
 *
 * 1.20.5+ 有 `Ingredient.CODEC_NONEMPTY`；1.20.1 没有（配方走 Gson JSON）。
 * 这里只满足编译，实际配方解析由 `RecipeSerializer.fromJson` 负责。
 */
object IngredientCodec : com.mojang.serialization.Codec<Ingredient> {
    override fun <T : Any> encode(
        input: Ingredient,
        ops: com.mojang.serialization.DynamicOps<T>,
        prefix: T,
    ): com.mojang.serialization.DataResult<T> =
        com.mojang.serialization.DataResult.error { "1.20.1 不支持 Ingredient 编码" }

    override fun <T : Any> decode(
        ops: com.mojang.serialization.DynamicOps<T>,
        input: T,
    ): com.mojang.serialization.DataResult<com.mojang.datafixers.util.Pair<Ingredient, T>> =
        com.mojang.serialization.DataResult.error { "1.20.1 不支持 Ingredient 解码" }
}
