package cn.coostack.usefulmagic.barrages.entity.skill

import cn.coostack.cooparticlesapi.api.controler.server.ServerControler
import cn.coostack.cooparticlesapi.barrages.BarrageHitResult
import cn.coostack.cooparticlesapi.barrages.BarrageOption
import cn.coostack.cooparticlesapi.barrages.HitBox
import cn.coostack.cooparticlesapi.extend.relativize
import cn.coostack.cooparticlesapi.network.particle.emitters.ParticleEmittersManager
import cn.coostack.cooparticlesapi.particles.impl.ControlableCloudEffect
import cn.coostack.cooparticlesapi.utils.Math3DUtil
import cn.coostack.cooparticlesapi.utils.RelativeLocation
import cn.coostack.usefulmagic.barrages.api.PlayerMagicDamagedBarrage
import cn.coostack.usefulmagic.particles.composition.skill.GiantSwordComposition
import cn.coostack.usefulmagic.particles.composition.skill.SwordLightComposition
import cn.coostack.usefulmagic.utils.FallingBlockHelper
import cn.coostack.usefulmagic.utils.FriendFilterHelper
import cn.coostack.usefulmagic.utils.ParticleOption
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.random.Random

class GiantSwordLightBarrageMagic(
    val controlStyle: SwordLightComposition?,
    val entityID: Int,
    loc: Vec3,
    world: ServerLevel,
    options: BarrageOption,
    damage: Double,
    shooter: Player
) : cn.coostack.usefulmagic.barrages.api.PlayerMagicDamagedBarrage(
    loc,
    world,
    options,
    damage,
    shooter
) {
    val countDouble: Int
        get() = ParticleOption.getParticleCounts()

    var tick = 0
    override fun tick() {
        if (options.speed > 0.8) {
            if (tick == 0) {

            }
            tick++
        }
        super.tick()

        val bindControl = bindControl.get() as GiantSwordComposition
        bindControl.rotateToPoint(RelativeLocation.of(direction))
        val entity = world.getEntity(entityID) ?: return
        direction = direction.add(loc.relativize(entity.eyePosition.add(0.0, -0.5, 0.0)).normalize()).normalize()
    }

    override fun onHitDamaged(result: BarrageHitResult) {
        val random = Random(System.currentTimeMillis())
        controlStyle?.status?.setStatus(2)
        shooter ?: return
        result.entities.forEach {
            it.invulnerableTime = 0
        }
        val blocks = FallingBlockHelper.getBoxIncludeBlockPosList(
            AABB.ofSize(loc, 8.0, 1.0, 8.0), world
        )

        FallingBlockHelper.conversionBlockToFallingBlocks(
            blocks, false, world
        ).forEach {
            val direction = loc.relativize(it.position().scale(1.0 / 9))
            it.deltaMovement = direction.add(
                random.nextDouble(-0.5, 0.5),
                random.nextDouble(0.2, 0.5),
                random.nextDouble(-0.5, 0.5)
            )
        }
        world.playSound(
            null, loc.x, loc.y, loc.z,
            SoundEvents.GENERIC_EXPLODE, shooter!!.soundSource, 10f, 2f
        )
        world.playSound(
            null, loc.x, loc.y, loc.z,
            SoundEvents.GENERIC_EXPLODE, shooter!!.soundSource, 10f, 2f
        )

    }

    override fun filterHitEntity(livingEntity: LivingEntity): Boolean {
        return livingEntity.uuid != shooter?.uuid && FriendFilterHelper.filterNotFriend(shooter!!, livingEntity)
    }

    override fun createHitBox(): HitBox {
        return HitBox.of(12.0, 12.0, 12.0)
    }

    override fun createControler(): ServerControler<*> {
        return GiantSwordComposition(loc, world)
    }
}

