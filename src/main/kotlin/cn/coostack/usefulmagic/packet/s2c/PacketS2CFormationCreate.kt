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
 * 阵法创建时(包括重新加载) 发包
 */
class PacketS2CFormationCreate(val pos: BlockPos) : CustomPacketPayload {
    companion object {
        val payloadID =
            CustomPacketPayload.Type<PacketS2CFormationCreate>(ResourceLocation(UsefulMagic.MOD_ID, "formation_create"))

        val CODEC = StreamCodec.of<FriendlyByteBuf, PacketS2CFormationCreate>(
            { buf, data->
                buf.writeVec3(data.pos.center)
            }, {
                PacketS2CFormationCreate(ofFloored(it.readVec3()))
            }
        )

    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?> {
        return payloadID
    }
}