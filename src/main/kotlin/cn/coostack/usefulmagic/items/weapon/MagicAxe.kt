package cn.coostack.usefulmagic.items.weapon

import cn.coostack.cooparticlesapi.CooParticlesAPI
import cn.coostack.cooparticlesapi.barrages.HitBox
import cn.coostack.cooparticlesapi.network.particle.emitters.ParticleEmittersManager
import cn.coostack.cooparticlesapi.network.particle.emitters.PhysicConstant
import cn.coostack.cooparticlesapi.network.particle.emitters.type.EmittersShootTypes
import cn.coostack.cooparticlesapi.particles.impl.ControlableCloudEffect
import cn.coostack.usefulmagic.items.UsefulMagicToolMaterials
import cn.coostack.usefulmagic.particles.emitters.DirectionShootEmitters
import cn.coostack.usefulmagic.particles.emitters.LineEmitters
import cn.coostack.usefulmagic.skill.api.EntityRandomSkillManager
import cn.coostack.usefulmagic.skill.api.EntitySkillManager
import cn.coostack.usefulmagic.skill.player.ComboCondition
import cn.coostack.usefulmagic.skill.player.HeavyHitSkill
import cn.coostack.usefulmagic.skill.player.PlayerSwordLightSkill
import cn.coostack.usefulmagic.skill.player.PlayerSwordSlashSkill
import cn.coostack.usefulmagic.utils.ComboUtil
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.LevelEvent
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import cn.coostack.cooparticlesapi.extend.*
import java.util.*
import java.util.function.Predicate
import kotlin.math.pow

/**
 * 特性介绍
 *
 * 消耗自身魔力值 恢复耐久度
 * 右键使用技能(不对着方块)
 *
 * 连击会加强技能
 *
 * 设定技能
 * 按照连击次数
 * 技能释放优先级是 高连击次数 -> 低连击次数
 * 拥有最低连击次数可以获得一些粒子效果
 */
class MagicAxe(settings: Properties) : AxeItem(UsefulMagicToolMaterials.MAGIC, 7.0f, -3.0f, settings) {
    companion object {
        val playerSkills = HashMap<UUID, EntitySkillManager>()
        fun getSkillManager(player: ServerPlayer): EntitySkillManager {
            return playerSkills.getOrPut(player.uuid) {
                EntityRandomSkillManager(player).apply {

                    addSkill(HeavyHitSkill(7f))
                    addSkill(PlayerSwordSlashSkill(4f))
                    addSkill(PlayerSwordLightSkill())
                }
            }.also {


                it.owner = player
            }
        }

        fun postPlayerAxeSkillTick() {
            playerSkills.forEach {
                it.value.tick()
            }
        }
    }

    val random = Random(System.currentTimeMillis())

    /**
     * 造成了伤害（连击/粒子效果）
     *
     * <h2>⚠️ 1.20.1 移植要点（2026-09-26 修）</h2>
     *
     * 上游重写的是 **`postHurtEnemy`（1.21 新增，`void`，伤害**已造成后**调用）**。
     * 1.20.1 **没有** `postHurtEnemy`，只有 `Item.hurtEnemy(stack, target, attacker): boolean`。
     *
     * 1.20.1 的调用链（javap 实测）：
     *
     * <pre>
     * Player.attack
     *   ├─ offset 420/729:  Entity.hurt(source, amount)      // 伤害已造成
     *   └─ offset 1022:     ItemStack.hurtEnemy(target, attacker)
     *                          └─ Item.hurtEnemy(stack, target, attacker): boolean
     *                                └─ DiggerItem: ItemStack.hurtAndBreak(2, …)  // 扣耐久
     *                                └─ 返回 true → 记一次「物品使用」统计
     * </pre>
     *
     * ⇒ **`ItemStack.hurtEnemy` 才是 1.20.1 里 `postHurtEnemy` 的等价物**（同为伤害后调用）；
     * 返回值语义是「**是否计入物品使用**」（含**是否消耗耐久**），**不是**「是否造成伤害」。
     *
     * 所以：
     * * 必须在所有代码路径上调用 `super.hurtEnemy(...)`（否则**不掉耐久、不计统计**）
     * * 最终必须 `return super.hurtEnemy(...)`（不要 `return false` 撒谎）
     *
     * 之前移植时把 `postHurtEnemy` 的「void 无返回」机械翻译成 `return false`，
     * 导致 **`state.count < 3` 时连 `super` 都没调** → 斧头**永不掉耐久**。
     */
    override fun hurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        if (attacker !is ServerPlayer) {
            return super.hurtEnemy(stack, target, attacker)
        }
        val state = ComboUtil.getComboState(attacker.uuid)
        state.increase()
        if (state.count > 6 && random.nextDouble() > 0.8) {
            val direction = attacker.forward
            CooParticlesAPI.scheduler.runTaskTimerMaxTick(2, 6) {
                val spawnPos = attacker.eyePosition
                    .add(
                        random.nextDouble(-3.0, 3.0),
                        random.nextDouble(-0.5, 2.0),
                        random.nextDouble(-3.0, 3.0),
                    )
            }
        }
        if (state.count < 3) {

            return super.hurtEnemy(stack, target, attacker)
        }
        val targetPos = target.eyePosition.add(0.0, -0.5, 0.0)
        val spawnPos = targetPos.add(
            random.nextDouble(-5.0, 5.0),
            random.nextDouble(-5.0, 5.0),
            random.nextDouble(-5.0, 5.0),
        )
        val dir = spawnPos.relativize(targetPos)

        CooParticlesAPI.scheduler.runTask(10) {
            val emitter = LineEmitters(spawnPos, attacker.level()).apply {
                templateData.apply {
                    effect = ControlableCloudEffect(uuid)
                    maxAge = 15
                    size = 0.1f
                }
                maxTick = 1
                this.endPos = dir.scale(2.0)
                this.count = (dir.length() * 3).toInt()
            }
            val world = attacker.level()
            world.playSound(
                null, attacker.x, attacker.y, attacker.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 10f, 2f
            )
            ParticleEmittersManager.spawnEmitters(emitter)
            world.getEntitiesOfClass(LivingEntity::class.java, AABB.ofSize(targetPos, 4.0, 4.0, 4.0)) {
                it.uuid != attacker.uuid
            }.forEach {
                val source = it.damageSources().playerAttack(attacker)
                it.invulnerableTime = 0
                it.hurt(source, 5f)
            }
        }
        maceHit(attacker, target)
        return super.hurtEnemy(stack, target, attacker)
    }

    /**
     * 攻击
     * 返回值为是否能造成伤害
     */


    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack?> {
        if (world.isClientSide || user !is ServerPlayer) {
            return super.use(world, user, hand)
        }
        val stack = user.getItemInHand(hand)
        val skillManager = getSkillManager(user)
        if (skillManager.hasActiveSkill()) {
            return super.use(world, user, hand)
        }
        val choice = skillManager.getSkills {
            it is ComboCondition && it.canTrigger(user)
        }.maxByOrNull { (it.value as ComboCondition).triggerComboMin }?.value ?: return super.use(world, user, hand)
        skillManager.setActiveSkill(choice)
        user.startUsingItem(hand)
        return InteractionResultHolder.consume(stack)
    }

    override fun releaseUsing(stack: ItemStack, world: Level, user: LivingEntity, remainingUseTicks: Int) {
        super.releaseUsing(stack, world, user, remainingUseTicks)
        if (world.isClientSide) return
        if (user !is ServerPlayer) return
        val skillManager = getSkillManager(user)
        if (skillManager.hasActiveSkill()) {
            skillManager.resetActiveSkill(false)
        }
    }

    override fun finishUsingItem(stack: ItemStack, world: Level, user: LivingEntity): ItemStack {
        if (world.isClientSide) return super.finishUsingItem(stack, world, user)
        if (user !is ServerPlayer) return super.finishUsingItem(stack, world, user)
        val skillManager = getSkillManager(user)
        if (skillManager.hasActiveSkill()) {
            skillManager.resetActiveSkill(false)
        }
        return super.finishUsingItem(stack, world, user)
    }

    override fun inventoryTick(stack: ItemStack, world: Level, entity: Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (!selected) {
            return
        }

        if (entity !is ServerPlayer) {
            return
        }

        val state = ComboUtil.getComboState(entity.uuid)
        if (state.count < 3) {
            return
        }

        val emitter = DirectionShootEmitters(entity.position(), world)
            .apply {
                maxTick = 1
                shootDirection = Vec3(0.0, 1.0, 0.0)
                randomX = 0.2
                randomZ = 0.2
                randomY = 0.1
                speedDrag = 0.98
                count = 3
                shootType = EmittersShootTypes.box(
                    HitBox.of(1.0, 0.5, 1.0)
                )
                gravity = PhysicConstant.EARTH_GRAVITY
                templateData.apply {
                    this.speed = 0.3
                    this.maxAge = 20
                    this.effect = ControlableCloudEffect(uuid)
                }
            }
        ParticleEmittersManager.spawnEmitters(emitter)
    }

    override fun getUseDuration(stack: ItemStack): Int {


        return 72000
    }

    private fun maceHit(attacker: LivingEntity, target: Entity) {

        val world = attacker.level()
        world.playSound(
            null, attacker.x, attacker.y, attacker.z,
            SoundEvents.GENERIC_EXPLODE, attacker.soundSource, 1.0f, 1.0f,
        )
        knockbackNearbyEntities(world, attacker as? Player ?: return, target)
    }

    private fun knockbackNearbyEntities(world: Level, player: Player, attacked: Entity) {
        world.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, attacked.blockPosition(), 750)
        world.getEntitiesOfClass(
            LivingEntity::class.java,
            attacked.boundingBox.inflate(3.5),
            getKnockbackPredicate(player, attacked)
        ).forEach { entity ->
            if (entity == null) return@forEach

            val vec = entity.position().subtract(attacked.position())
            val d = getKnockback(player, entity, vec)
            val pushVec = vec.normalize().scale(d)

            if (d > 0.0) {
                entity.push(pushVec.x, 0.7, pushVec.z)
                if (entity is ServerPlayer) {
                    entity.connection.send(ClientboundSetEntityMotionPacket(entity))
                }
            }
        }
    }

    private fun getKnockbackPredicate(player: Player, attacked: Entity): Predicate<LivingEntity?> {
        return Predicate { entity: LivingEntity? ->
            val bl = !entity!!.isSpectator
            val bl2 = entity !== player && entity !== attacked
            val bl3 = !player.isAlliedTo(entity)
            val bl4 = !(entity is TamableAnimal && entity.isTame && player.uuid == entity.ownerUUID)
            val bl5 = !(entity is ArmorStand && entity.isMarker)
            val bl6 = attacked.distanceToSqr(entity) <= 3.5.pow(2.0)
            bl && bl2 && bl3 && bl4 && bl5 && bl6
        }
    }

    fun getKnockback(player: Player, attacked: LivingEntity, distance: Vec3): Double {
        return ((3.5 - distance.length())
                * 0.7f
                * (if (player.fallDistance > 5.0f) 2 else 1)
                * (1.0 - attacked.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)))
    }
}
