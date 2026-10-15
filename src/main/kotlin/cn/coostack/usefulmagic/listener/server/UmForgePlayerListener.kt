package cn.coostack.usefulmagic.listener.server

import cn.coostack.usefulmagic.UsefulMagic
import cn.coostack.usefulmagic.UsefulMagic.state
import cn.coostack.usefulmagic.extend.copyManaDataFrom
import cn.coostack.usefulmagic.extend.syncManaDataToClient
import cn.coostack.usefulmagic.listener.DefendMagicListener
import net.minecraftforge.event.entity.living.LivingHurtEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

/**
 * ★★★ UsefulMagic 的**玩家事件监听** —— 对应上游 `neoforge/listener/server/PlayerListener.kt`
 * （平台侧目录，移植时整个没搬）。
 *
 * <h2>⚠️ 这个文件直接决定「蓄力能不能启动」</h2>
 *
 * 蓄力链：
 * ```
 * 按键(CooKeyBindingManager) → PacketKeyActionC2S → CooEventBus(KeyActionEvent)
 *   → UM KeyListener.onChargingKeyEvents
 *        └── if (!MagicHelper.isManaEnough(player, wand)) { 重置; return }   ← ★ 魔力不够就不蓄力
 * ```
 * 而 `isManaEnough` 读的是 `player.mana`（服务端 tracker）——
 * **只有登录/复活时同步过魔力数据，服务端才有正确的值**：
 *
 * ```kotlin
 * // 上游 PlayerListener
 * fun onLogin(e: PlayerEvent.PlayerLoggedInEvent) {
 *     state.getDataFromServer(e.entity.uuid)   // 从服务端 saved-data 取
 *     e.entity.syncManaDataToClient()          // 建立 tracker + 排一次重同步
 * }
 * ```
 *
 * 缺了这个文件 → `syncManaDataToClient()` **一次都没被调用过**（已 grep 确认，
 * 全项目只有定义没有调用点）→ 服务端 tracker 里 `CURRENT_MANA` 是默认值/缺失
 * → `isManaEnough` 判定异常 → **蓄力起不来**。
 *
 * 顺带：`onClone`（换维度/复活保数据）、`onChangedDimension`（换维度重同步）、
 * `beforeDamaged`（防御魔法免伤）也一起补上。
 */
object UmForgePlayerListener {

    /** 由 `UsefulMagicForge` 在 mod 构造期调用 */
    fun register() {
        FORGE_BUS.addListener(::onLogin)
        FORGE_BUS.addListener(::onClone)
        FORGE_BUS.addListener(::onChangedDimension)
        FORGE_BUS.addListener(::beforeDamaged)
        UsefulMagic.logger.info("UsefulMagic 玩家事件监听已注册")
    }

    /** 上游 `onLogin`：取魔力数据 + 同步给客户端 */
    private fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        runCatching {
            val player = event.entity
            state.getDataFromServer(player.uuid)
            player.syncManaDataToClient()
        }.onFailure { UsefulMagic.logger.error("玩家登录魔力同步失败", it) }
    }

    /** 上游 `onClone`：复活/换维度时保留魔力数据 */
    private fun onClone(event: PlayerEvent.Clone) {
        runCatching {
            event.entity.copyManaDataFrom(event.original)
            event.entity.syncManaDataToClient()
        }.onFailure { UsefulMagic.logger.error("玩家克隆魔力同步失败", it) }
    }

    /** 上游 `onChangedDimension` */
    private fun onChangedDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        runCatching { event.entity.syncManaDataToClient() }
            .onFailure { UsefulMagic.logger.error("玩家换维度魔力同步失败", it) }
    }

    /**
     * 上游 `beforeDamaged`（NeoForge 的 `LivingIncomingDamageEvent`）：
     * 1.20.1 的等价事件是 **[LivingHurtEvent]**（同样有 source + amount），防御魔法可免伤。
     */
    private fun beforeDamaged(event: LivingHurtEvent) {
        runCatching {
            val entity = event.entity

            val canDamage = !DefendMagicListener.call(entity, event.source, event.amount)
            event.isCanceled = canDamage
        }.onFailure { UsefulMagic.logger.error("防御魔法伤害判定失败", it) }
    }
}
