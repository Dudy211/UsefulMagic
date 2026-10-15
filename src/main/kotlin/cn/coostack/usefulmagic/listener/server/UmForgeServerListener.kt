package cn.coostack.usefulmagic.listener.server

import cn.coostack.usefulmagic.UsefulMagic
import cn.coostack.usefulmagic.extend.isOf
import cn.coostack.usefulmagic.platform.FuelHelper
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

/**
 * UsefulMagic 的**燃料事件监听** —— 对应上游 `neoforge/listener/server/ServerListener.kt`
 * 里的 `onFuel`（平台侧目录，移植时没搬）。
 *
 * 上游还负责 tick/启动/命令注册，但那三项我们在 `UsefulMagicForge` 里已经挂了
 * （`onServerTick` / `onServerStarted` / `onRegisterCommands`），所以这里只补燃料。
 */
object UmForgeServerListener {

    /** 由 `UsefulMagicForge` 在 mod 构造期调用 */
    fun register() {
        FORGE_BUS.addListener(::onFuel)
    }

    /** 上游 `onFuel`：让本模组的物品作为熔炉燃料 */
    private fun onFuel(event: FurnaceFuelBurnTimeEvent) {
        runCatching {
            val stack = event.itemStack
            val burn = FuelHelper.fuels
                .filter { stack.isOf(it.key.getItem()) }
                .entries.firstOrNull()?.value ?: 0
            if (burn != 0) {
                event.burnTime = burn
            }
        }.onFailure { UsefulMagic.logger.warn("燃料判定失败: {}", it.message) }
    }
}
