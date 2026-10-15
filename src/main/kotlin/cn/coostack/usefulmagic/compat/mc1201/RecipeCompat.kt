package cn.coostack.usefulmagic.compat.mc1201

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer

/**
 * # 1.20.1 兼容层：配方体系
 *
 * ## 差异
 * | | 1.21.1 | 1.20.1 |
 * |---|---|---|
 * | 配方容器类型 | `RecipeInput` | `Container` |
 * | 包装类 | `RecipeHolder<T>`（带 id） | **没有**（配方自带 getId()） |
 * | `assemble(input, registries)` | `HolderLookup.Provider` | `RegistryAccess` |
 * | `getResultItem(registries)` | `HolderLookup.Provider` | `RegistryAccess` |
 * | `RecipeSerializer` | 有 `codec()`/`streamCodec()` | 只有 `fromJson`/`toNetwork` |
 *
 * ## 方案
 * 1. `RecipeHolder` 用 shim 类型替代（只包一个 Recipe，id 从 recipe.getId() 取）
 * 2. `RecipeSerializer.codec()` 提供默认实现（返回 null / 抛异常，UM 不真正依赖它）
 * 3. `streamCodec()` 同理
 */

/**
 * 1.20.5+ 的 `RecipeHolder<T>`。
 *
 * 1.20.1 没有（配方自己带 getId()），这里做一个等价包装。
 */
class RecipeHolder<T : Recipe<*>>(val value: T) {
    /** 1.20.5+ 的 `holder.id()` */
    fun id(): ResourceLocation = value.id

    override fun equals(other: Any?): Boolean =
        other is RecipeHolder<*> && other.value.id == value.id

    override fun hashCode(): Int = value.id.hashCode()

    override fun toString(): String = "RecipeHolder(${value.id})"
}

/**
 * 1.20.1 兼容：`RecipeSerializer` 的 `codec()` / `streamCodec()`。
 *
 * 1.20.1 的 RecipeSerializer 只有 `fromJson` / `toNetwork` / `fromNetwork`，
 * 所以这两个方法在 shim 里给出默认实现（返回 null，UM 不真正用）。
 */
fun RecipeSerializer<*>.codecOrNull(): Any? = null

/**
 * 1.20.1 兼容：Container 的最小实现（供 Recipe<C> 的 C 用）。
 *
 * 1.20.1 的 `Recipe<C extends Container>`，UM 的 input 类型要继承它。
 */
interface UMRecipeInput : net.minecraft.world.Container
