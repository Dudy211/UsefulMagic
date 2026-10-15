package cn.coostack.usefulmagic.entity.custom.dragon.phases

import cn.coostack.cooparticlesapi.extend.minus
import cn.coostack.cooparticlesapi.utils.GraphMathHelper
import cn.coostack.cooparticlesapi.utils.Math3DUtil
import cn.coostack.usefulmagic.entity.custom.dragon.MagicDragonAnimationState
import cn.coostack.usefulmagic.entity.custom.dragon.MagicDragonEntity
import cn.coostack.usefulmagic.entity.util.EntityPoseUtil
import cn.coostack.usefulmagic.entity.util.phases.PhaseDefinition
import cn.coostack.usefulmagic.entity.util.phases.PhaseRegistries
import cn.coostack.usefulmagic.entity.util.phases.PhaseResult
import cn.coostack.usefulmagic.entity.util.phases.PhaseRuntime
import cn.coostack.usefulmagic.extend.asDouble
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.phys.Vec3

/**
 * - 手动设置， 移动到target然后悬停 移动方式和DragonSimpleFlightPhase一致
 * - 手动结束
 * @constructor Create empty Hover flight phase
 */
class DragonHoverFlightPhase : PhaseDefinition<MagicDragonEntity> {
    companion object {
        const val ID = "hover-flight-phase"

        const val HOVER_ARRIVE_RADIUS = "hover_arrive_radius"

        const val HOVER_KEEP_SPEED = "hover_keep_speed"

        const val HOVER_CORRECTION_SPEED = "hover_correction_speed"

        const val HOVER_IDLE_DRAG = "hover_idle_drag"

        const val HOVER_DRAG_MIN = "hover_drag_min"

        const val HOVER_DRAG_SCALE = "hover_drag_scale"

        const val HOVER_UPWARD_SPEED = "hover_upward_speed"

        const val DIRECTION_LERP = "direction_lerp"

        private const val DEFAULT_HOVER_ARRIVE_RADIUS = 6.0
        private const val DEFAULT_HOVER_KEEP_SPEED = 0.08
        private const val DEFAULT_HOVER_CORRECTION_SPEED = 0.12
        private const val DEFAULT_HOVER_IDLE_DRAG = 0.6
        private const val DEFAULT_HOVER_DRAG_MIN = 0.6
        private const val DEFAULT_HOVER_DRAG_SCALE = 0.75
        private const val DEFAULT_HOVER_UPWARD_SPEED = 0.02
        private const val DEFAULT_DIRECTION_LERP = 0.15
        val HOLDER = PhaseRegistries.register(ID) { DragonHoverFlightPhase() }
    }

    var target: Vec3? = null

    override fun canBegin(
        instance: MagicDragonEntity,
        runtime: PhaseRuntime
    ): Boolean {
        return target != null || runtime.hasTarget()
    }

    override fun begin(
        instance: MagicDragonEntity,
        runtime: PhaseRuntime
    ) {
        if (!runtime.hasTarget()) {
            target?.let(runtime::addTarget)
        }
    }

    override fun step(
        instance: MagicDragonEntity,
        runtime: PhaseRuntime
    ): PhaseResult {
        val hoverTarget = runtime.peekTargetOrNull() ?: target
        if (hoverTarget == null) {
            val idleDrag = (runtime.params[HOVER_IDLE_DRAG].asDouble ?: DEFAULT_HOVER_IDLE_DRAG).coerceAtLeast(0.0)
            instance.deltaMovement = instance.deltaMovement.scale(idleDrag)
            instance.playAnimation(MagicDragonAnimationState.FLY)
            return PhaseResult.Continue
        }

        val position = instance.position()
        val currentVelocity = instance.deltaMovement
        val targetDirection = hoverTarget - position
        val arriveRadius = (runtime.params[HOVER_ARRIVE_RADIUS].asDouble ?: DEFAULT_HOVER_ARRIVE_RADIUS)
            .coerceAtLeast(1.0E-6)
        if (Math3DUtil.isPointCrossBySphere(position, currentVelocity, arriveRadius, hoverTarget)) {
            val dragMin = (runtime.params[HOVER_DRAG_MIN].asDouble ?: DEFAULT_HOVER_DRAG_MIN).coerceAtLeast(0.0)
            val dragScale = (runtime.params[HOVER_DRAG_SCALE].asDouble ?: DEFAULT_HOVER_DRAG_SCALE).coerceAtLeast(0.0)
            val correctionSpeed =
                (runtime.params[HOVER_CORRECTION_SPEED].asDouble ?: DEFAULT_HOVER_CORRECTION_SPEED)
            val hoverCorrection = if (targetDirection.lengthSqr() <= 1.0E-6) {
                Vec3.ZERO
            } else {
                targetDirection.scale(correctionSpeed / arriveRadius)
            }
            val drag = EntityPoseUtil.getVelocityAirDragRatio(currentVelocity.length()).coerceAtLeast(dragMin)
            val dampedVelocity = currentVelocity.scale(drag).scale(dragScale)
            val verticalCorrection = (targetDirection.y * correctionSpeed / arriveRadius)
                .coerceIn(-correctionSpeed, correctionSpeed)
            val rawNextY = (dampedVelocity.y * 0.35 + verticalCorrection)
                .coerceIn(-correctionSpeed, correctionSpeed)
            val nextY = when {
                targetDirection.y < -1.0E-6 -> rawNextY.coerceAtMost(0.0)
                targetDirection.y > 1.0E-6 -> rawNextY.coerceAtLeast(0.0)
                else -> rawNextY
            }
            val nextVelocity = Vec3(
                dampedVelocity.x + hoverCorrection.x,
                nextY,
                dampedVelocity.z + hoverCorrection.z
            )
            instance.deltaMovement = nextVelocity
            instance.playAnimation(MagicDragonAnimationState.FLY)
            return PhaseResult.Continue
        }

        EntityPoseUtil.rotationFix(instance.forward, targetDirection, instance)
        val flightSpeed = instance.getAttributeValue(Attributes.FLYING_SPEED)
        val normalizedVelocity = if (currentVelocity.lengthSqr() <= 1.0E-6) {
            instance.forward.normalize()
        } else {
            currentVelocity.normalize()
        }
        val desiredDirection = targetDirection.normalize()
        val targetVelocity = GraphMathHelper.lerp(
            (runtime.params[DIRECTION_LERP].asDouble ?: DEFAULT_DIRECTION_LERP).coerceIn(0.0, 1.0),
            normalizedVelocity,
            desiredDirection
        ).normalize().scale(
            flightSpeed.coerceAtLeast(runtime.params[HOVER_KEEP_SPEED].asDouble ?: DEFAULT_HOVER_KEEP_SPEED)
        )
        val correctedTargetVelocity = if (targetDirection.y < -1.0E-6 && targetVelocity.y > 0.0) {
            Vec3(targetVelocity.x, desiredDirection.y * targetVelocity.length(), targetVelocity.z)
                .normalize()
                .scale(targetVelocity.length())
        } else if (targetDirection.y > 1.0E-6 && targetVelocity.y < 0.0) {
            Vec3(targetVelocity.x, desiredDirection.y * targetVelocity.length(), targetVelocity.z)
                .normalize()
                .scale(targetVelocity.length())
        } else {
            targetVelocity
        }
        instance.deltaMovement = correctedTargetVelocity
        if (correctedTargetVelocity.y >= 0.0) {
            instance.addDeltaMovement(EntityPoseUtil.stayAwayFloor(instance.level(), position, correctedTargetVelocity))
        }
        instance.playAnimation(MagicDragonAnimationState.FLY)
        return PhaseResult.Continue
    }

    override fun end(
        instance: MagicDragonEntity,
        runtime: PhaseRuntime
    ) {
        runtime.clearTargets()
    }

    override fun id(): String {
        return ID
    }
}
