package cn.coostack.usefulmagic.platform.network

import cn.coostack.cooparticlesapi.compat.mc1201.CustomPacketPayload
import cn.coostack.cooparticlesapi.platform.network.ClientContext
import cn.coostack.cooparticlesapi.platform.network.ServerContext
import net.minecraft.client.Minecraft
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.player.Player
import net.minecraftforge.network.NetworkEvent

/**
 * 1.20.1 Forge 的 [ClientContext] 实现。
 *
 * 上游的 listener 只依赖 `player()` / `client()` 两个方法，
 * 所以这里把 Forge 的 [NetworkEvent.Context] 包成同样的接口。
 */
class ForgeClientContext(private val ctx: NetworkEvent.Context) : ClientContext {

    override fun player(): Player {
        val mc = Minecraft.getInstance()
        return mc.player ?: throw IllegalStateException("ForgeClientContext: 客户端玩家为空")
    }

    override fun client(): Minecraft = Minecraft.getInstance()
}

/**
 * 1.20.1 Forge 的 [ServerContext] 实现。
 *
 * `reply(packet)` 直接把包发回发信玩家（上游用它做请求-响应）。
 */
class ForgeServerContext(private val ctx: NetworkEvent.Context) : ServerContext {

    override fun player(): Player =
        ctx.sender ?: throw IllegalStateException("ForgeServerContext: 没有发送者")

    override fun server(): MinecraftServer =
        ctx.sender?.server ?: throw IllegalStateException("ForgeServerContext: 没有服务器实例")

    override fun reply(packet: CustomPacketPayload) {
        ctx.sender?.let { UmNetwork.sendToPlayer(packet, it) }
    }
}
