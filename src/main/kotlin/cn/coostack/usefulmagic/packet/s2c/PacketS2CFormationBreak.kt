package cn.coostack.usefulmagic.packet.s2c

import cn.coostack.cooparticlesapi.compat.mc1201.readVec3
import cn.coostack.cooparticlesapi.compat.mc1201.writeVec3

import cn.coostack.cooparticlesapi.extend.ofFloored
import cn.coostack.usefulmagic.UsefulMagic
import net.minecraft.network.FriendlyByteBuf
import cn.coostack.cooparticlesapi.compat.mc1201.StreamCodec
import cn.coostack.cooparticlesapi.compat.mc1201.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos

/**
 * 阵法被破坏时发送 (告知客户端这里爆炸了)
 */
class PacketS2CFormationBreak(val formationPos: BlockPos, val damage: Float) : CustomPacketPayload {
    companion object {

        val payloadID = CustomPacketPayload.Type<PacketS2CFormationBreak>(
            ResourceLocation.fromNamespaceAndPath(
                UsefulMagic.MOD_ID,
                "formation_break"
            )
        )

        val CODEC = StreamCodec.of<FriendlyByteBuf, PacketS2CFormationBreak>(
            { buf, data ->
                buf.writeVec3(data.formationPos.center)
                buf.writeFloat(data.damage)
            }, { buf ->
                PacketS2CFormationBreak(ofFloored(buf.readVec3()), buf.readFloat())
            }
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return payloadID
    }
}