package cn.coostack.usefulmagic.platform

import cn.coostack.usefulmagic.UsefulMagic
import cn.coostack.usefulmagic.platform.services.IPlatformHelper
import java.util.*
import java.util.function.Supplier

object UsefulMagicServices {



    @JvmField
    val PLATFORM: IPlatformHelper = load(IPlatformHelper::class.java)

    @JvmField
    val ENV_HELPER: EnvHelper = load(EnvHelper::class.java)





    fun <T> load(clazz: Class<T>): T {
        val loadedService = ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow { NullPointerException("Failed to load service for " + clazz.name) }
        UsefulMagic.logger.debug("Loaded {} for service {}", loadedService, clazz)
        return loadedService!!
    }
}