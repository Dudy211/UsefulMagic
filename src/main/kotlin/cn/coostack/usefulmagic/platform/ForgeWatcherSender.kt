package cn.coostack.usefulmagic.platform

import cn.coostack.cooparticlesapi.compat.mc1201.CustomPacketPayload
import cn.coostack.usefulmagic.platform.network.UmNetwork
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity

/**
 * 1.20.1 Forge 的 [WatcherSender] 实现。
 *
 * 上游 Fabric 版走 `PlayerLookup.tracking(entity)`；
 * Forge 用 `ChunkMap.getPlayers(chunkPos)` 取到追踪该实体的玩家。
 */
object ForgeWatcherSender : WatcherSender {

    override fun trackWith(entity: Entity, packet: CustomPacketPayload, self: Boolean) {
        UmNetwork.sendToTracking(entity, packet, self)
    }
}
