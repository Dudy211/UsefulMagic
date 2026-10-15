package cn.coostack.usefulmagic.compat.mc1201

import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData

/**
 * # 1.20.1 兼容层：SynchedEntityData.Builder
 *
 * ## 差异
 * | | 1.21.1 | 1.20.1 |
 * |---|---|---|
 * | `defineSynchedData` | `(SynchedEntityData.Builder)` | `()` 无参 |
 * | 注册数据项 | `builder.define(accessor, value)` | `entityData.define(accessor, value)` |
 *
 * ## 方案
 * 提供一个「转发 Builder」：构造时持有 `SynchedEntityData`，
 * `define()` 直接转发到它。这样 UM 的
 * ```kotlin
 * override fun defineSynchedData(builder: SynchedEntityData.Builder) {
 *     super.defineSynchedData(builder)
 *     builder.define(IS_ATTACKING, false)
 * }
 * ```
 * 只要把参数类型换成 [SynchedEntityDataBuilderCompat] 即可。
 */
class SynchedEntityDataBuilderCompat internal constructor(
    private val data: SynchedEntityData,
) {
    /** 对应 1.20.5+ 的 `SynchedEntityData.Builder.define(accessor, value)` */
    fun <T> define(accessor: EntityDataAccessor<T>, value: T) {
        data.define(accessor, value)
    }

    companion object {
        /** 把一个 SynchedEntityData 包成 builder（供 shim 内部用） */
        @JvmStatic
        fun wrap(data: SynchedEntityData): SynchedEntityDataBuilderCompat =
            SynchedEntityDataBuilderCompat(data)
    }
}

/**
 * 1.20.5+ 的 `SynchedEntityData.defineId` 在 1.20.1 已存在，签名一致：
 * `defineId(Class<out Entity>, EntityDataSerializer<T>)`
 * —— 无需额外处理。
 */
