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

        /** 1.20.1 的 JSON 解析（对应 1.20.5+ 的 codec()） */
        override fun fromJson(recipeId: ResourceLocation, json: com.google.gson.JsonObject): AltarRecipeType {
            val output = ItemStack.EMPTY
            val round = RoundShapeRecipe.EMPTY
            val center = Ingredient.EMPTY
            val manaNeed = json.get("mana_need")?.asInt ?: 0
            val tick = json.get("tick")?.asInt ?: 0
            return AltarRecipeType(output, round, center, manaNeed, tick)
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
