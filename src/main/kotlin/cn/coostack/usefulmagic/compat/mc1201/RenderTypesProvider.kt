package cn.coostack.usefulmagic.compat.mc1201

import cn.coostack.cooparticlesapi.platform.services.IPlatformHelper
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation

/**
 * 1.21 的 `IPlatformHelper.getRenderTypesProvider()` 等价物。
 *
 * `entityCutoutEmissive(tex, bright)`
 *   -> 1.20.1 的 `RenderType.entityTranslucentEmissive(tex)`（自体发光、半透明）。
 */
object RenderTypesProvider {
    fun entityCutoutEmissive(texture: ResourceLocation, bright: Boolean): RenderType =
        RenderType.entityTranslucentEmissive(texture)
}

/** 让 `CooParticlesServices.PLATFORM.getRenderTypesProvider()` 这种写法可用 */
fun IPlatformHelper.getRenderTypesProvider(): RenderTypesProvider = RenderTypesProvider
