package cn.coostack.usefulmagic.packet.c2s

import cn.coostack.cooparticlesapi.compat.mc1201.readVec3
import cn.coostack.cooparticlesapi.compat.mc1201.writeVec3

import cn.coostack.cooparticlesapi.extend.ofFloored
import cn.coostack.usefulmagic.UsefulMagic
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import cn.coostack.cooparticlesapi.compat.mc1201.StreamCodec
import cn.coostack.cooparticlesapi.compat.mc1201.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

class PacketC2SFormationSettingRequest(val clickPos: BlockPos) : CustomPacketPayload {
    companion object {
        val CODEC: StreamCodec<FriendlyByteBuf, PacketC2SFormationSettingRequest> =
            CustomPacketPayload.codec<FriendlyByteBuf, PacketC2SFormationSettingRequest>(
                { p, b ->
                    b.writeVec3(p.clickPos.center)
                }, {
                    return@codec PacketC2SFormationSettingRequest(ofFloored(it.readVec3()))
                }
            )

        val payloadID =
            CustomPacketPayload.Type<PacketC2SFormationSettingRequest>(
                ResourceLocation(UsefulMagic.MOD_ID, "formation_settings_request")
            )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?> {
        return payloadID
    }
}