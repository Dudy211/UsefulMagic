package cn.coostack.usefulmagic.entity.custom.dragon.phases

import cn.coostack.cooparticlesapi.utils.GraphMathHelper
import cn.coostack.cooparticlesapi.utils.Math3DUtil
import cn.coostack.usefulmagic.entity.custom.dragon.MagicDragonEntity
import cn.coostack.usefulmagic.entity.util.EntityPoseUtil
import cn.coostack.usefulmagic.entity.util.phases.*
import cn.coostack.usefulmagic.extend.asDouble
import cn.coostack.usefulmagic.extend.asInt
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import cn.coostack.cooparticlesapi.extend.*
import kotlin.random.Random

/**
 * 加速冲刺穿过目标 一定时间后回到DragonSimpleFlightPhase
 *
 */
class DragonCrossFlightPhase : PhaseDefinition<MagicDragonEntity>, PhaseCollisionEntity<MagicDragonEntity> {
    companion object {
        const val ID = "cross-flight-phase"

        const val IMPACT_DAMAGE = "impact_damage"

        const val ARRIVE_RADIUS = "arrive_radius"

        const val ARRIVE_COUNTDOWN_MIN = "arrive_countdown_min"

        const val ARRIVE_COUNTDOWN_MAX = "arrive_countdown_max"

        const val MISSING_TARGET_RESET_TICKS = "missing_target_reset_ticks"

        const val DIRECTION_LERP = "direction_lerp"

        private const val DEFAULT_IMPACT_DAMAGE = 15f
        private const val DEFAULT_ARRIVE_RADIUS = 12.0
        private const val DEFAULT_ARRIVE_COUNTDOWN_MIN = 30
        private const val DEFAULT_ARRIVE_COUNTDOWN_MAX = 80
        private const val DEFAULT_MISSING_TARGET_RESET_TICKS = 10
        private const val DEFAULT_DIRECTION_LERP = 0.6
        val HOLDER = PhaseRegistries.register(ID) { DragonCrossFlightPhase() }
    }

    var impactDamage = DEFAULT_IMPACT_DAMAGE
    private var countdown = 0
    private var arrive = false
    override fun canBegin(
        instance: MagicDragonEntity,
        runtime: PhaseRuntime
    ): Boolean {
        return true
    }

    override fun begin(
        instance: MagicDragonEntity,
        runtime: PhaseRuntime
    ) {
    }

    override fun step(
        instance: MagicDragonEntity,
        runtime: PhaseRuntime
    ): PhaseResult {
        if (arrive && countdown-- <= 0) {
            return PhaseResult.Reset
        }
        val target = runtime.peekTargetOrNull()
        if (target == null && !arrive) {

            val missingTargetResetTicks =
                (runtime.params[MISSING_TARGET_RESET_TICKS].asInt ?: DEFAULT_MISSING_TARGET_RESET_TICKS)
                    .coerceAtLeast(0)
            if (countdown-- <= -missingTargetResetTicks) {
                return PhaseResult.Reset
            }
            return PhaseResult.Continue
        }

        val position = instance.position()
        val currentVelocity = instance.deltaMovement
        val targetDirection = if (target != null) {
            target - position
        } else {
            instance.forward
        }
        val level = instance.level()

        if (target != null) {
            val arriveRadius = (runtime.params[ARRIVE_RADIUS].asDouble ?: DEFAULT_ARRIVE_RADIUS).coerceAtLeast(0.0)
            if (Math3DUtil.isPointCrossBySphere(position, currentVelocity, arriveRadius, target)) {

                arrive = true
                val minCountdown =
                    (runtime.params[ARRIVE_COUNTDOWN_MIN].asInt ?: DEFAULT_ARRIVE_COUNTDOWN_MIN).coerceAtLeast(0)
                val maxCountdown =
                    (runtime.params[ARRIVE_COUNTDOWN_MAX].asInt ?: DEFAULT_ARRIVE_COUNTDOWN_MAX)
                        .coerceAtLeast(minCountdown + 1)
                countdown = Random.nextInt(minCountdown, maxCountdown)
                runtime.popTarget()
            }
        }

        val stayAway = EntityPoseUtil.stayAwayFloor(level, position, currentVelocity)
        instance.addDeltaMovement(stayAway)


        EntityPoseUtil.rotationFix(instance.forward, targetDirection, instance)

        val flightSpeed = instance.getAttributeValue(Attributes.FLYING_SPEED)

        val finalVelocity = GraphMathHelper.lerp(
            (runtime.params[DIRECTION_LERP].asDouble ?: DEFAULT_DIRECTION_LERP).coerceIn(0.0, 1.0),
            instance.deltaMovement.normalize(),
            targetDirection.normalize()
        ).normalize() * flightSpeed

        instance.deltaMovement =
            finalVelocity * if (arrive) EntityPoseUtil.getVelocityAirDragRatio(flightSpeed) else 1.0
        return PhaseResult.Continue
    }

    override fun end(
        instance: MagicDragonEntity,
        runtime: PhaseRuntime
    ) {
    }

    override fun id(): String {
        return ID
    }

    override fun collisionEntity(
        targets: Set<LivingEntity>,
        instance: MagicDragonEntity,
        runtime: PhaseRuntime
    ) {
        val damage = runtime.params[IMPACT_DAMAGE].asDouble?.toFloat() ?: impactDamage

        targets.forEach {
            it.addDeltaMovement(instance.deltaMovement.add(0.0, 1.0, 0.0))
            val source = it.damageSources().mobAttack(instance)

            it.hurt(source, damage)

        }
    }
}
