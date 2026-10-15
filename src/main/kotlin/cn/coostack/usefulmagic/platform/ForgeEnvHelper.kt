package cn.coostack.usefulmagic.platform

import net.minecraftforge.fml.loading.FMLEnvironment

/**
 * UsefulMagic 的 Forge 1.20.1 环境判定。
 *
 * 对应 META-INF/services/cn.coostack.usefulmagic.platform.EnvHelper
 */
class ForgeEnvHelper : EnvHelper {
    override fun isServerSide(): Boolean = FMLEnvironment.dist.isDedicatedServer
}
