package cn.coostack.usefulmagic.compat.mc1201

import com.mojang.serialization.Codec
import cn.coostack.usefulmagic.items.weapon.wands.MagicWand
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.IdentityHashMap

/**
 * 组件类型标记接口。
 *
 * ⚠️ 关键：`companion object` 里放 `builder()`，这样
 *    `UMDataComponentType.builder<Int>()` 才能编译通过。
 */
/**
 * UM 自有的组件句柄。
 *
 * 组件在 1.20.1 是纯 NBT-key 桩，没有真实注册表，所以不需要 CP 的
 * `CommonDeferredComponentType`（那个持有的是 CP 包里的同名类型，
 * 与本文件的 UMDataComponentType 是两个不同的类）。
 */
class UMDeferredComponent<T>(
    val id: net.minecraft.resources.ResourceLocation,
    private val supplier: java.util.function.Supplier<UMDataComponentType<T>>,
) {
    private var value: UMDataComponentType<T>? = null
    fun get(): UMDataComponentType<T> {
        var v = value
        if (v == null) {
            v = supplier.get()
            value = v
        }
        return v
    }
}

interface UMDataComponentType<T> {
    companion object {
        @JvmStatic
        fun <T> builder(): UMComponentBuilder<T> = UMComponentBuilder()
    }
}

/**
 * 魔法数据组件诊断输出。
 *
 * ★ 2026-10-05：定位完「组件 Codec 桥静默失败」之后，改为**保留能力默认关闭**。
 * 开关跟着 MagicWand.DIAG_ENABLED 走，一处控制全部 UM 探针。
 * 日志级别已降为 debug，打开也只进 debug.log。
 */
internal fun compDbg(key: String, msg: String) {
    if (!MagicWand.DIAG_ENABLED) return
    val n = COMP_DBG_COUNTS.merge(key, 1, Int::plus) ?: 1
    if (n <= COMP_DBG_LIMIT) {
        cn.coostack.usefulmagic.UsefulMagic.logger.debug("[um-comp] {}: {}", key, msg)
    }
}

private const val COMP_DBG_LIMIT = 40
private val COMP_DBG_COUNTS = java.util.concurrent.ConcurrentHashMap<String, Int>()

/** 组件键：NBT 键名 + 默认值 + 读写策略 */
class UMComponentKey<T>(
    val id: String,
    val default: T,
    val reader: (CompoundTag, String) -> T?,
    val writer: (CompoundTag, String, T) -> Unit,
) : UMDataComponentType<T> {

    fun read(stack: ItemStack): T? = stack.tag?.let { reader(it, id) } ?: default

    fun write(stack: ItemStack, value: T?) {
        if (value == null) {
            stack.tag?.remove(id)
        } else {
            writer(stack.orCreateTag, id, value)
        }
    }

    fun isPresent(stack: ItemStack): Boolean = stack.tag?.contains(id) == true
}

/** 给组件补 NBT 键 */
fun <T> UMDataComponentType<T>.withKey(key: String): UMDataComponentType<T> = when (this) {
    is UMComponentKey<T> -> UMComponentKey(key, default, reader, writer)
    else -> this
}

/**
 * 读取组件值；NBT 缺失时返回 [UMComponentKey.default]。
 *
 * ⚠️ 不要在这里对 `null` 抛异常：`default` 为 null 的组件（自定义 Codec，
 * 如 `WandMagicData`）在「物品还没被写入该组件」时**本来**就是 absent，
 * 调用点用的是 `getComponent(...) ?: return`。这里返回 null 让调用点决定。
 */
@Suppress("UNCHECKED_CAST")
internal fun <T> UMDataComponentType<T>.asComponentKey(): UMComponentKey<T>? = this as? UMComponentKey<T>

/** builder：从 Codec 描述反推 NBT 策略 */
class UMComponentBuilder<T> internal constructor() {
    private var codec: Codec<T>? = null

    fun persistent(codec: Codec<T>): UMComponentBuilder<T> = apply { this.codec = codec }

    @Suppress("UNCHECKED_CAST")
    fun build(): UMDataComponentType<T> {



































        val n = (codec?.toString() ?: "").uppercase()

        compDbg(
            "classify",
            "codec=${codec?.toString()?.take(80)} upper=${n.take(80)} branch=" +
                when (n) {
                    "INT", "BOOL", "DOUBLE", "FLOAT" -> n
                    else -> "ELSE"
                }
        )
        return when (n) {
            "INT" -> UMComponentKey(
                "", 0 as T,
                { tag, k -> tag.getInt(k) as T },
                { tag, k, v -> tag.putInt(k, v as Int) },
            )
            "BOOL" -> UMComponentKey(
                "", false as T,
                { tag, k -> tag.getBoolean(k) as T },
                { tag, k, v -> tag.putBoolean(k, v as Boolean) },
            )
            "DOUBLE" -> UMComponentKey(
                "", 0.0 as T,
                { tag, k -> tag.getDouble(k) as T },
                { tag, k, v -> tag.putDouble(k, v as Double) },
            )
            "FLOAT" -> UMComponentKey(
                "", 0f as T,
                { tag, k -> tag.getFloat(k) as T },
                { tag, k, v -> tag.putFloat(k, v as Float) },
            )
            else -> {

                val c = codec
                UMComponentKey(
                    "",
                    null as T,



                    { tag, k ->
                        val raw: net.minecraft.nbt.Tag? = tag.get(k)
                        if (raw == null) null
                        else if (c == null) null
                        else runCatching {
                            val res = c.parse(net.minecraft.nbt.NbtOps.INSTANCE, raw)
                            if (res.error().isPresent) {
                                compDbg(
                                    "decode/$k",
                                    "解码失败 key=$k raw=${raw.toString().take(220)} err=${res.error().get().message()}"
                                )
                                null
                            } else res.result().orElse(null)
                        }.getOrElse { e ->
                            compDbg("decode-ex/$k", "解码抛异常 key=$k raw=${raw.toString().take(220)} ex=${e::class.java.name}: ${e.message}")
                            null
                        }
                    },

                    { tag, k, v ->
                        if (v != null && c != null) {
                            runCatching {
                                val res = c.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, v)
                                if (res.error().isPresent) {
                                    compDbg("encode/$k", "编码失败 key=$k obj=$v err=${res.error().get().message()}")
                                } else {
                                    res.result().orElse(null)?.let { tag.put(k, it) }
                                }
                            }.getOrElse { e ->
                                compDbg("encode-ex/$k", "编码抛异常 key=$k obj=$v ex=${e::class.java.name}: ${e.message}")
                            }
                        }
                    },
                )
            }
        }
    }
}

/**
 * 1.20.5+ `stack.get(type)`。
 *
 * ⚠️ 返回**可空**：组件没被写入时返回 null，让调用点自己决定
 * （本项目所有调用点都写成 `getComponent(...) ?: return`，
 * 说明「缺失」是预期情况）。
 *
 * 千万不要在这里 `?: error("未知组件类型")` —— 那会让 `?:` 永远没机会生效：
 * 自定义 Codec 的组件（`PreferMagicData` / `WandMagicData`，默认值就是 null）
 * 只要没写过，就会**抛异常而不是返回 null**，直接崩在 tooltip / 显示逻辑里。
 */
fun <T> ItemStack.getComponent(type: UMDataComponentType<T>): T? =
    type.asComponentKey()?.read(this)

fun <T> ItemStack.componentGet(type: UMDataComponentType<T>): T? = getComponent(type)

/** 1.20.5+ `stack.getOrDefault(type, fallback)` */
fun <T> ItemStack.getOrDefaultComponent(type: UMDataComponentType<T>, fallback: T): T {
    val k = type.asComponentKey() ?: return fallback
    return if (k.isPresent(this)) (k.read(this) ?: fallback) else fallback
}

/** 1.20.5+ `stack.set(type, value)` */
fun <T> ItemStack.setComponent(type: UMDataComponentType<T>, value: T?) {
    type.asComponentKey()?.write(this, value)
}

/** 1.20.5+ `stack.remove(type)` */
fun <T> ItemStack.removeComponent(type: UMDataComponentType<T>) {
    type.asComponentKey()?.write(this, null)
}

/** 1.20.5+ `stack.has(type)` */
fun <T> ItemStack.hasComponent(type: UMDataComponentType<T>): Boolean =
    type.asComponentKey()?.isPresent(this) == true

private val pendingComponents =
    IdentityHashMap<Item.Properties, MutableList<Pair<UMDataComponentType<*>, Any?>>>()

/** 1.20.5+ 的 `Item.Properties.component(type, value)` */
fun <T> Item.Properties.component(type: UMDataComponentType<T>, value: T): Item.Properties {
    pendingComponents.getOrPut(this) { mutableListOf() }.add(type to value)
    return this
}

/** 取走某 Properties 上收集的组件（mixin 用） */

fun takePendingComponents(props: Item.Properties): List<Pair<UMDataComponentType<*>, Any?>> =
    pendingComponents.remove(props) ?: emptyList()

class UMCustomData internal constructor(internal val tag: CompoundTag) {
    fun copyTag(): CompoundTag = tag.copy()

    companion object {
        @JvmField
        val EMPTY = UMCustomData(CompoundTag())

        @JvmStatic
        fun of(tag: CompoundTag): UMCustomData = UMCustomData(tag.copy())
    }
}

object UMDataComponents {
    /** 1.21 的 UMDataComponents.CONTAINER → 1.20.1 用 NBT "Items" 列表 */
    @JvmField
    val CONTAINER: UMComponentKey<List<ItemStack>> = UMComponentKey(
        id = "Items",
        default = emptyList(),
        reader = { tag, k ->
            if (tag.contains(k, 9)) {
                val out = ArrayList<ItemStack>()
                val lt = tag.getList(k, 10)
                for (i in 0 until lt.size) {
                    val st = ItemStack.of(lt.getCompound(i))
                    if (!st.isEmpty) out.add(st)
                }
                out
            } else null
        },
        writer = { tag, k, v ->
            val lt = net.minecraft.nbt.ListTag()
            v.forEach { st ->
                if (!st.isEmpty) {
                    val t = CompoundTag()
                    st.save(t)
                    lt.add(t)
                }
            }
            tag.put(k, lt)
        },
    )

    /** 1.21 的 HIDE_TOOLTIP */
    @JvmField
    val HIDE_TOOLTIP: UMComponentKey<Boolean> = UMComponentKey(
        id = "HideTooltip", default = false,
        reader = { tag, k -> if (tag.contains(k)) tag.getBoolean(k) else null },
        writer = { tag, k, v -> tag.putBoolean(k, v) },
    )

    /** 1.21 的 HIDE_ADDITIONAL_TOOLTIP */
    @JvmField
    val HIDE_ADDITIONAL_TOOLTIP: UMComponentKey<Boolean> = UMComponentKey(
        id = "HideAdditionalTooltip", default = false,
        reader = { tag, k -> if (tag.contains(k)) tag.getBoolean(k) else null },
        writer = { tag, k, v -> tag.putBoolean(k, v) },
    )

    @JvmField
    val CUSTOM_DATA: UMComponentKey<UMCustomData> = UMComponentKey(
        id = "custom_data",
        default = UMCustomData.EMPTY,
        reader = { tag, _ -> UMCustomData(tag.copy()) },
        writer = { tag, _, value -> tag.merge(value.tag) },
    )
}

/** 1.20.5+ `Player.hasInfiniteMaterials()` → 1.20.1 `abilities.instabuild` */
fun Player.hasInfiniteMaterials(): Boolean = this.abilities.instabuild

/** 1.20.5+ `Entity.hasInfiniteMaterials()` */
fun Entity.hasInfiniteMaterials(): Boolean =
    if (this is Player) this.abilities.instabuild else false
