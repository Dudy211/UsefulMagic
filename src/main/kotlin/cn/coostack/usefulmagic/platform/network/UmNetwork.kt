package cn.coostack.usefulmagic.platform.network

import cn.coostack.cooparticlesapi.compat.mc1201.CustomPacketPayload
import cn.coostack.cooparticlesapi.compat.mc1201.StreamCodec
import cn.coostack.usefulmagic.UsefulMagic
import cn.coostack.usefulmagic.packet.c2s.*
import cn.coostack.usefulmagic.packet.listener.client.*
import cn.coostack.usefulmagic.packet.listener.server.*
import cn.coostack.usefulmagic.packet.s2c.*
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.simple.SimpleChannel

/**
 * UsefulMagic 的 Forge 1.20.1 网络层。
 *
 * 上游用 1.20.5+ 的 `CustomPacketPayload` + `PayloadTypeRegistry`；
 * 1.20.1 只有 Forge 的 SimpleChannel，所以这里给每个业务包开一个通道条目，
 * 编解码复用包自带的 `CODEC`（[StreamCodec] shim）。
 *
 * 收包后转发到上游的 listener（`packet.listener.*`），保持与 Fabric/Neo 相同的接入点。
 */
object UmNetwork {

    private const val PROTOCOL_VERSION = "1"

    @JvmStatic
    val CHANNEL: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ResourceLocation(UsefulMagic.MOD_ID, "main"),
        { PROTOCOL_VERSION },
        { it == PROTOCOL_VERSION },
        { it == PROTOCOL_VERSION },
    )

    private var registered = false

    /** 用一个包自带的 CODEC 注册通道条目 */
    private fun <T : CustomPacketPayload> reg(
        cls: Class<T>,
        id: Int,
        dir: NetworkDirection,
        codec: StreamCodec<FriendlyByteBuf, T>,
        handle: (T, NetworkEvent.Context) -> Unit,
    ) {
        CHANNEL.messageBuilder(cls, id, dir)
            .encoder { packet, buf -> codec.encode(buf, packet) }
            .decoder { buf -> codec.decode(buf) }
            .consumerMainThread { packet, ctx ->
                val c = ctx.get()
                handle(packet, c)
                c.setPacketHandled(true)
            }
            .add()
    }

    @JvmStatic
    fun register() {
        if (registered) return
        registered = true


        reg(PacketS2CFriendListResponse::class.java, 0, NetworkDirection.PLAY_TO_CLIENT,
            PacketS2CFriendListResponse.CODEC) { p, c ->
            FriendResponsePacketListener.receive(p, ForgeClientContext(c)) }

        reg(PacketS2CFriendChangeResponse::class.java, 1, NetworkDirection.PLAY_TO_CLIENT,
            PacketS2CFriendChangeResponse.CODEC) { p, c ->
            FriendChangeResponsePacketListener.receive(p, ForgeClientContext(c)) }

        reg(PacketS2CFormationSettingsResponse::class.java, 2, NetworkDirection.PLAY_TO_CLIENT,
            PacketS2CFormationSettingsResponse.CODEC) { p, c ->
            FormationSettingsPacketResponseListener.receive(p, ForgeClientContext(c)) }

        reg(PacketS2CEnergyCrystalChange::class.java, 3, NetworkDirection.PLAY_TO_CLIENT,
            PacketS2CEnergyCrystalChange.CODEC) { p, c ->
            FormationPacketListener.handleEnergyChange(p, ForgeClientContext(c)) }

        reg(PacketS2CFormationCreate::class.java, 4, NetworkDirection.PLAY_TO_CLIENT,
            PacketS2CFormationCreate.CODEC) { p, c ->
            FormationPacketListener.handleCreate(p, ForgeClientContext(c)) }

        reg(PacketS2CFormationBreak::class.java, 5, NetworkDirection.PLAY_TO_CLIENT,
            PacketS2CFormationBreak.CODEC) { p, c ->
            FormationPacketListener.handleBreak(p, ForgeClientContext(c)) }

        reg(PacketS2CTrackerToggle::class.java, 6, NetworkDirection.PLAY_TO_CLIENT,
            PacketS2CTrackerToggle.CODEC) { p, c ->
            TrackerToggleListener.receive(p, ForgeClientContext(c)) }


        reg(PacketC2SFriendListRequest::class.java, 7, NetworkDirection.PLAY_TO_SERVER,
            PacketC2SFriendListRequest.CODEC) { p, c ->
            FriendListRequestHandler.receive(p, ForgeServerContext(c)) }

        reg(PacketC2SFriendAddRequest::class.java, 8, NetworkDirection.PLAY_TO_SERVER,
            PacketC2SFriendAddRequest.CODEC) { p, c ->
            FriendAddListRequestHandler.receive(p, ForgeServerContext(c)) }

        reg(PacketC2SFriendRemoveRequest::class.java, 9, NetworkDirection.PLAY_TO_SERVER,
            PacketC2SFriendRemoveRequest.CODEC) { p, c ->
            FriendRemoveListRequestHandler.receive(p, ForgeServerContext(c)) }

        reg(PacketC2SFriendSettingsChangeRequest::class.java, 10, NetworkDirection.PLAY_TO_SERVER,
            PacketC2SFriendSettingsChangeRequest.CODEC) { p, c ->
            FriendSettingsChangeRequestHandler.receive(p, ForgeServerContext(c)) }

        reg(PacketC2SFormationSettingRequest::class.java, 11, NetworkDirection.PLAY_TO_SERVER,
            PacketC2SFormationSettingRequest.CODEC) { p, c ->
            FormationSettingRequestPacketListener.receive(p, ForgeServerContext(c)) }

        reg(PacketC2SFormationSettingChangeRequest::class.java, 12, NetworkDirection.PLAY_TO_SERVER,
            PacketC2SFormationSettingChangeRequest.CODEC) { p, c ->
            FormationSettingChangePacketListener.receive(p, ForgeServerContext(c)) }

        reg(PacketC2SWandMagicExchangeRequest::class.java, 13, NetworkDirection.PLAY_TO_SERVER,
            PacketC2SWandMagicExchangeRequest.CODEC) { p, c ->
            WandMagicExchangeRequestHandler.receive(p, ForgeServerContext(c)) }

        UsefulMagic.logger.info("UsefulMagic 网络通道注册完成（14 个包）")
    }



    @JvmStatic
    fun sendToPlayer(packet: CustomPacketPayload, to: ServerPlayer) {
        CHANNEL.send(PacketDistributor.PLAYER.with { to }, packet)
    }

    @JvmStatic
    fun sendToAll(packet: CustomPacketPayload) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet)
    }

    @JvmStatic
    fun sendToTracking(entity: net.minecraft.world.entity.Entity, packet: CustomPacketPayload, includeSelf: Boolean) {
        val level = entity.level() as? net.minecraft.server.level.ServerLevel ?: return
        val players = level.chunkSource.chunkMap.getPlayers(entity.chunkPosition(), false)
        players.forEach { sp ->
            if (includeSelf || sp !== entity) sendToPlayer(packet, sp)
        }
    }

    @JvmStatic
    fun sendToServer(packet: CustomPacketPayload) {
        CHANNEL.sendToServer(packet)
    }
}
