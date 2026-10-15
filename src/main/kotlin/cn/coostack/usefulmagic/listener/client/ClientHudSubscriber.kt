package cn.coostack.usefulmagic.listener.client

import cn.coostack.usefulmagic.gui.magicexchange.MagicExchangeOverlay
import cn.coostack.usefulmagic.gui.mana.ManaBarCallback
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

/**
 * HUD 渲染订阅（仅客户端）。
 *
 * <h2>⚠️⚠️ 为什么**不用** `@Mod.EventBusSubscriber`（2026-09-27 定稿）</h2>
 *
 * 这是个反复踩的坑，记录清楚：
 *
 * **① KFF 的 `AutoKotlinEventBusSubscriber` 只支持 Kotlin `object`。**
 *
 * 反编译字节码，它对每个 `@Mod.EventBusSubscriber` 类走两条路：
 *
 * ```
 * KClass.getObjectInstance()            // ← object 才能取到单例
 *   成功  → "Auto-subscribing kotlin object" → register(实例)
 *   抛异常且 message 含 "file facades" → "Auto-subscribing kotlin file" → register(Class)
 *   其它异常                            → 直接 athrow（什么都注册不了）
 * ```
 *
 * **② 但 KFF 注册到的是【实例】，而 Forge 的 `EventBus.registerObject` 会跳过 `static` 方法**
 * （`Modifier.isStatic` → `goto` 跳过，见 `EventBus` 字节码）。
 *
 * ⇒ 所以「`object` + `@JvmStatic`」（v18 及以前）和「普通 `class`」（v19 试过）
 * **两种写法都注册不上**：
 * * `object` + `@JvmStatic`：KFF 能取到实例，但方法编译成 `static` → 被 Forge 跳过
 * * 普通 `class`：`getObjectInstance()` 抛异常（非 "file facades"）→ KFF 直接 `athrow`
 *
 * 实测日志（v19/v20）：
 * ```
 * Attempting to inject @EventBusSubscriber kotlin objects ... for usefulmagic
 * （之后一条 "Auto-subscribing" 都没有）
 * ```
 *
 * **③ 定稿做法：显式 `FORGE_BUS.addListener`。**
 *
 * 这与本工程其他地方（`UsefulMagicForge.init{}` 里的 `FORGE_BUS.addListener(::onServerTick)`
 * 等）**完全一致** —— 那些是**实测可用**的。KFF 的 `FORGE_BUS` 就是个普通
 * `IEventBus`，`addListener(::方法引用)` 不依赖任何 KFF 注解魔法。
 *
 * <p>由 [cn.coostack.usefulmagic.UsefulMagicForgeClient.setupClient] 在 **mod 构造期**调用。
 */
object UsefulMagicHud {

    /**
     * 显式挂到 `FORGE_BUS`（游戏总线）。
     *
     * ⚠️ 必须在 **mod 构造期**挂（`Minecraft` 构造函数内那批事件的时序问题见
     * `CRASH_FIX_REGISTER_EARLY.md`）；不过 `RenderGuiEvent.Post` 是**每帧**派发的
     * 运行期事件，晚一点挂也不会永久错过 —— 但为一致性仍放构造期。
     */
    fun register() {
        FORGE_BUS.addListener(::hudCallback)
    }

    private fun hudCallback(event: RenderGuiEvent.Post) {
        val partialTick = event.partialTick
        ManaBarCallback.onHudRender(event.guiGraphics, partialTick)
        MagicExchangeOverlay.onHudRender(event.guiGraphics, partialTick)
    }
}
