package cn.coostack.usefulmagic.platform

import cn.coostack.cooparticlesapi.platform.registry.CommonDeferredItem

/**
 * 熔炉燃料表 —— 对应上游 `neoforge/platform/FuelHelper.kt`（平台侧文件，移植时没搬）。
 *
 * 上游在 `UsefulMagicNeo` 里填了一条：`WOODEN_WAND = 200`（木法杖当燃料烧 200 tick）。
 * 由 `UsefulMagicForge` 的 [fillFuels] 填充，`UmForgeServerListener` 消费。
 */
object FuelHelper {
    val fuels: MutableMap<CommonDeferredItem, Int> = HashMap()
}
