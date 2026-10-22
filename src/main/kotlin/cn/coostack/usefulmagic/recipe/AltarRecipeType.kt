package cn.coostack.usefulmagic.recipe

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.NonNullList
import net.minecraft.core.RegistryAccess
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level

/**
 * 1.20.1 移植改动：
 *  - `RecipeSerializer` 的抽象方法是 `fromJson`/`fromNetwork`/`toNetwork`
 *    （1.20.5+ 才有 `codec()`/`streamCodec()`）
 *  - 物品/配料的网络读写用 `FriendlyByteBuf.writeItem/readItem`、
 *    `Ingredient.toNetwork/fromNetwork`（1.20.1 没有 xxx_STREAM_CODEC）
 *  - `Recipe` 接口要求 `getId()`
 */
class AltarRecipeType(
    val output: ItemStack = ItemStack.EMPTY,
    val round: RoundShapeRecipe,
    val center: Ingredient,
    val manaNeed: Int,
    val tick: Int,
) : Recipe<AltarStackRecipeInput> {

    object Type : RecipeType<AltarRecipeType> {
        const val ID = "altar_recipe"
    }

    object AltarRecipeSerializer : RecipeSerializer<AltarRecipeType> {
        const val ID = "altar_recipe"

        /**
         * 1.20.1 的 JSON 解析（对应 1.20.5+ 的 codec()）。
         *
         * ★ 2026-10-22 修复:原来这里是个空壳 —— 三个字段全赋 `EMPTY`,
         * 只读了 mana_need/tick。结果 23 个配方全解析成空配方 →
         * 手册显示「配方数: 0」、祭坛也无法合成。
         */
        override fun fromJson(recipeId: ResourceLocation, json: com.google.gson.JsonObject): AltarRecipeType {
            val output = parseItemStack(json.getAsJsonObject("output"))
            val round = parseRoundShape(json.getAsJsonObject("data"))
            val center = parseIngredient(json.get("center"))
                ?: net.minecraft.world.item.crafting.Ingredient.EMPTY
            val manaNeed = json.get("mana_need")?.asInt ?: 0
            val tick = json.get("tick")?.asInt ?: 0
            return AltarRecipeType(output, round, center, manaNeed, tick)
        }

        /**
         * 解析 ItemStack。
         *
         * ⚠️ 数据文件用的是 1.20.5+ 格式 `{"id": "...", "count": N}`,
         * 而 1.20.1 的 `ItemStack.CODEC` 要的是 `{"item": "..."}`。
         * 这里两种都兼容。
         */
        private fun parseItemStack(obj: com.google.gson.JsonObject?): ItemStack {
            if (obj == null) return ItemStack.EMPTY
            val idStr = when {
                obj.has("item") -> obj.get("item").asString
                obj.has("id") -> obj.get("id").asString
                else -> return ItemStack.EMPTY
            }
            val id = ResourceLocation.tryParse(idStr) ?: return ItemStack.EMPTY
            val item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id)
            if (item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY
            val count = obj.get("count")?.asInt ?: 1
            return ItemStack(item, count)
        }

        /** 解析环形基座（8 个槽位，key 为 "0".."7"）。 */
        private fun parseRoundShape(obj: com.google.gson.JsonObject?): RoundShapeRecipe {
            if (obj == null) return RoundShapeRecipe.EMPTY
            val list = net.minecraft.core.NonNullList.withSize(
                8, net.minecraft.world.item.crafting.Ingredient.EMPTY
            )
            obj.entrySet().forEach { (key, value) ->
                val index = key.toIntOrNull() ?: return@forEach
                if (index !in 0..7) return@forEach
                list[index] = parseIngredient(value) ?: net.minecraft.world.item.crafting.Ingredient.EMPTY
            }
            return RoundShapeRecipe(list)
        }

        /** 解析单个 Ingredient（支持 `{"item":...}` / `{"tag":...}` / 数组）。 */
        private fun parseIngredient(
            element: com.google.gson.JsonElement?
        ): net.minecraft.world.item.crafting.Ingredient? {
            if (element == null || element.isJsonNull) return null
            return try {
                net.minecraft.world.item.crafting.Ingredient.fromJson(element)
            } catch (_: Exception) {
                null
            }
        }

        /** 1.20.1 的网络读取 */
        override fun fromNetwork(recipeId: ResourceLocation, buffer: FriendlyByteBuf): AltarRecipeType {
            val manaNeed = buffer.readInt()
            val tick = buffer.readInt()
            val round = RoundShapeRecipe.fromNetwork(buffer)
            val center = Ingredient.fromNetwork(buffer)
            val output = buffer.readItem()
            return AltarRecipeType(output, round, center, manaNeed, tick)
        }

        /** 1.20.1 的网络写出 */
        override fun toNetwork(buffer: FriendlyByteBuf, recipe: AltarRecipeType) {
            buffer.writeInt(recipe.manaNeed)
            buffer.writeInt(recipe.tick)
            RoundShapeRecipe.toNetwork(buffer, recipe.round)
            recipe.center.toNetwork(buffer)
            buffer.writeItem(recipe.output)
        }
    }

    override fun matches(input: AltarStackRecipeInput, level: Level): Boolean {
        if (level.isClientSide) return false
        if (input.mana < manaNeed) return false
        if (!center.test(input.centerItem)) return false
        return round.matchers(input)
    }

    override fun assemble(input: AltarStackRecipeInput, registries: RegistryAccess): ItemStack {
        return output.copy()
    }

    override fun getIngredients(): NonNullList<Ingredient> {
        val list = NonNullList.create<Ingredient>()
        list.add(center)
        list.addAll(round.ingredients)
        return list
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = true

    override fun getResultItem(registries: RegistryAccess): ItemStack = output

    override fun getSerializer(): RecipeSerializer<*> = AltarRecipeSerializer

    override fun getType(): RecipeType<*> = Type

    /** 1.20.1 的 Recipe 接口要求实现 */
    override fun getId(): ResourceLocation =
        ResourceLocation("usefulmagic", AltarRecipeSerializer.ID)
}
