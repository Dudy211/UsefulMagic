package cn.coostack.usefulmagic.listener.server

import cn.coostack.usefulmagic.UsefulMagic
import cn.coostack.usefulmagic.items.UsefulMagicItems
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/**
 * 熔炉燃料（木法杖可当燃料，200 tick）。
 *
 * <h2>⚠️⚠️ 必须用 `class`，不能用 `object`（2026-09-25 修）</h2>
 *
 * 同 [cn.coostack.usefulmagic.listener.client.UsefulMagicHud]：Forge 的
 * `EventBus.registerObject(Object)` 会**跳过所有 static 方法**，而 KFF 传给它的
 * 是**实例**。Kotlin `object` + `@JvmStatic` ⇒ 方法变成 `static` ⇒ **被跳过**。
 *
 * （功能上与 [UmForgeServerListener.onFuel] 重复，但那一个走
 *  `FORGE_BUS.addListener` 是生效的 —— 这里统一成 class，避免留一个
 *  「看起来注册了其实没注册」的隐患。）
 */
@Mod.EventBusSubscriber(
    modid = UsefulMagic.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
class FuelListener {

    @SubscribeEvent
    fun onFuel(event: FurnaceFuelBurnTimeEvent) {
        if (event.itemStack.`is`(UsefulMagicItems.WOODEN_WAND.getItem())) {
            event.burnTime = 200
        }
    }
}
