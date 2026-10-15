package cn.coostack.usefulmagic.particles.particle

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import cn.coostack.cooparticlesapi.platform.registry.CommonDeferredRegistry
import cn.coostack.usefulmagic.UsefulMagic
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.FriendlyByteBuf
import cn.coostack.cooparticlesapi.compat.mc1201.RegistryFriendlyByteBuf
import cn.coostack.cooparticlesapi.compat.mc1201.StreamCodec
import net.minecraft.resources.ResourceLocation

object UsefulMagicParticleTypes {
    val particleTypes = mutableListOf<CommonDeferredRegistry<ParticleType<*>>>()

    @JvmField
    val WAVE_PARTICLE = register(
        "wave_particle",
        false,
        { WaveParticleEffect.codec },
        { WaveParticleEffect.packetCode }
    )

    private fun <T : ParticleOptions> register(
        id: String,
        alwaysShow: Boolean,
        codecGetter: (type: ParticleType<T>) -> MapCodec<T>,
        packetCodec: (type: ParticleType<T>) -> StreamCodec<FriendlyByteBuf, T>
    ): CommonDeferredRegistry<ParticleType<T>> {
        val registry = CommonDeferredRegistry(
            BuiltInRegistries.PARTICLE_TYPE,
            ResourceLocation(UsefulMagic.MOD_ID, id)
        ) {
            object : ParticleType<T>(
                alwaysShow,
                object : ParticleOptions.Deserializer<T> {
                    override fun fromCommand(type: ParticleType<T>, reader: com.mojang.brigadier.StringReader): T =
                        throw UnsupportedOperationException("1.20.1 不支持命令解析粒子")

                    override fun fromNetwork(type: ParticleType<T>, buf: FriendlyByteBuf): T =
                        packetCodec(type).decode(buf)
                }
            ) {
                override fun codec(): Codec<T> = codecGetter(this).codec()
            }
        }
        particleTypes.add(registry)
        @Suppress("UNCHECKED_CAST")
        return registry as CommonDeferredRegistry<ParticleType<T>>
    }

    fun init() {}
}
