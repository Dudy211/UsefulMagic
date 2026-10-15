package cn.coostack.usefulmagic.platform

import cn.coostack.usefulmagic.platform.services.IPlatformHelper
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.loading.FMLLoader

/**
 * UsefulMagic 的 Forge 1.20.1 平台实现。
 *
 * 对应 META-INF/services/cn.coostack.usefulmagic.platform.services.IPlatformHelper
 */
class ForgePlatformHelper : IPlatformHelper {

    override fun getPlatformName(): String = "Forge"

    override fun isModLoaded(modId: String): Boolean = ModList.get().isLoaded(modId)

    override fun isDevelopmentEnvironment(): Boolean = !FMLLoader.isProduction()

    override fun getIterateTracker(): WatcherSender = ForgeWatcherSender
}
