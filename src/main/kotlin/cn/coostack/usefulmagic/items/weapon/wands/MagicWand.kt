package cn.coostack.usefulmagic.items.weapon.wands

import cn.coostack.usefulmagic.compat.mc1201.hasComponent
import cn.coostack.usefulmagic.compat.mc1201.removeComponent
import cn.coostack.usefulmagic.compat.mc1201.getComponent
import cn.coostack.usefulmagic.compat.mc1201.setComponent
import cn.coostack.usefulmagic.compat.mc1201.hasInfiniteMaterials
import cn.coostack.usefulmagic.profile.PreferMagicData
import cn.coostack.usefulmagic.extend.chargedItem
import cn.coostack.usefulmagic.extend.charging
import cn.coostack.usefulmagic.extend.chargingTick
import cn.coostack.usefulmagic.extend.mana
import cn.coostack.usefulmagic.extend.resetChargeState
import cn.coostack.usefulmagic.effects.UsefulMagicEffects
import cn.coostack.usefulmagic.items.UsefulMagicDataComponentTypes
import cn.coostack.usefulmagic.items.WandMagicData
import cn.coostack.usefulmagic.items.weapon.magic.MagicItem
import cn.coostack.usefulmagic.utils.MagicHelper
import cn.coostack.usefulmagic.compat.mc1201.UMDataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.SlotAccess
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ClickAction
import net.minecraft.world.inventory.Slot
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import net.minecraft.sounds.SoundEvents
import java.util.Optional
import kotlin.math.roundToInt

/**
 * # 法杖基类
 * ## 基本参数如下
 * - 法杖等级： 决定能否容纳某些法球
 * - 魔力效率： 决定法球使用的魔力消耗
 * - 速率加成： 决定法球的速度加成
 * - 偏好魔法: 对指定魔法类型有一些特殊加成
 * ## 设定
 * - 不放法术 法杖就无法施展魔法
 * - 施展法术不减速（右键）
 * - 法球放在副手， 法杖在主手 用来切换法球
 * @param properties
 *
 * FIXME 当玩家蓄力的时候切换法杖
 */
class MagicWand(properties: Properties) : Item(properties) {
    /**
     * 蓄力链诊断（2026-09-27 临时探针，确认后删）。
     *
     * 主人报「蓄力只有声音没有效果」——`usingTick`（蓄力音）在跑但 `release`（真正的
     * 效果）不触发，静态读代码定位不到。这里把每一 tick 的关键状态打出来，
     * 每条原因限流 [DIAG_LIMIT] 次，避免刷屏。
     */
    companion object {
        private const val DIAG_LIMIT = 40
        private val diagCounts = java.util.concurrent.ConcurrentHashMap<String, Int>()

        /**
         * ★ 总开关。默认关闭。
         *
         * 2026-10-05：定位完「蓄力只有声音没有效果」之后，探针代码保留但默认关闭。
         * 需要排查时改 true 重新构建即可；日志级别已降为 debug，
         * 打开也只进 debug.log，不污染 latest.log。
         */
        @JvmField
        var DIAG_ENABLED: Boolean = false

        @JvmStatic
        internal fun chargeDbg(key: String, msg: String) {
            if (!DIAG_ENABLED) return
            val n = diagCounts.merge(key, 1, Int::plus) ?: 1
            if (n <= DIAG_LIMIT) {
                cn.coostack.usefulmagic.UsefulMagic.logger.debug("[um-charge] {}: {}", key, msg)
            }
        }
    }

    override fun getUseAnimation(stack: ItemStack): UseAnim {
        val magicBall = getLoadedMagic(stack)
        if (!magicBall.isEmpty) {
            val useAnim = magicBall.useAnimation
            if (useAnim != UseAnim.NONE) {
                return useAnim
            }
        }
        return UseAnim.BOW
    }

    override fun getUseDuration(stack: ItemStack): Int {
        return 72000
    }

    fun exchangeMagic(player: Player, level: Level, stack: ItemStack, wandInHand: InteractionHand) {

        val (ball, ballSlotHand) = if (wandInHand == InteractionHand.MAIN_HAND) {
            player.offhandItem to InteractionHand.OFF_HAND
        } else {
            player.mainHandItem to InteractionHand.MAIN_HAND
        }


        if (player.charging) {

            stopCharge(player, level, stack, player.chargingTick, false)
            player.resetChargeState()
        }


        if (ball.isEmpty) {

            val oldBall = getLoadedMagic(stack)
            if (!oldBall.isEmpty) {
                player.setItemInHand(ballSlotHand, oldBall.copy())
                setLoadedMagic(stack, ItemStack.EMPTY)
            }
            return
        }

        if (!MagicHelper.isLevelEnough(stack, ball)) {

            return
        }


        val oldBall = getLoadedMagic(stack)
        if (oldBall.isEmpty) {
            setLoadedMagic(stack, ball.copyAndClear())
        } else {

            player.setItemInHand(ballSlotHand, oldBall.copy())
            setLoadedMagic(stack, ball.copyAndClear())
        }
    }

    override fun overrideStackedOnOther(stack: ItemStack, slot: Slot, action: ClickAction, player: Player): Boolean {
        if (stack.count != 1 || action != ClickAction.SECONDARY) {
            return false
        }
        val loadedMagic = getLoadedMagic(stack)
        val slotItem = slot.item
        if (slotItem.isEmpty) {
            if (loadedMagic.isEmpty) {
                return false
            }
            val extractedMagic = loadedMagic.copy()
            val remain = slot.safeInsert(extractedMagic)
            if (!remain.isEmpty) {
                return false
            }
            cancelChargeIfNeeded(player, stack)
            setLoadedMagic(stack, ItemStack.EMPTY)
            playRemoveOneSound(player)
            return true
        }
        if (!canLoadMagic(stack, slotItem)) {
            return false
        }

        val oldMagic = loadedMagic.copy()
        val newMagic = slot.safeTake(1, 1, player)
        if (newMagic.isEmpty) {
            return false
        }

        if (!oldMagic.isEmpty) {
            val remain = slot.safeInsert(oldMagic)
            if (!remain.isEmpty) {
                slot.safeInsert(newMagic)
                return false
            }
        }

        cancelChargeIfNeeded(player, stack)
        setLoadedMagic(stack, newMagic)
        playInsertSound(player)
        return true
    }

    override fun overrideOtherStackedOnMe(
        stack: ItemStack,
        other: ItemStack,
        slot: Slot,
        action: ClickAction,
        player: Player,
        access: SlotAccess
    ): Boolean {
        if (stack.count != 1 || action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false
        }
        val loadedMagic = getLoadedMagic(stack)
        if (other.isEmpty) {
            if (loadedMagic.isEmpty || !access.set(loadedMagic.copy())) {
                return false
            }
            cancelChargeIfNeeded(player, stack)
            setLoadedMagic(stack, ItemStack.EMPTY)
            playRemoveOneSound(player)
            return true
        }
        if (!canLoadMagic(stack, other)) {
            return false
        }

        val newMagic = other.split(1)
        if (newMagic.isEmpty) {
            return false
        }
        if (!loadedMagic.isEmpty && !access.set(loadedMagic.copy())) {
            other.grow(1)
            return false
        }

        cancelChargeIfNeeded(player, stack)
        setLoadedMagic(stack, newMagic)
        playInsertSound(player)
        return true
    }

    override fun getTooltipImage(stack: ItemStack): Optional<TooltipComponent> {
        if (stack.hasComponent(UMDataComponents.HIDE_TOOLTIP) || stack.hasComponent(UMDataComponents.HIDE_ADDITIONAL_TOOLTIP)) {
            return Optional.empty()
        }
        val magicBall = getLoadedMagic(stack)
        if (magicBall.isEmpty) {
            return Optional.empty()
        }
        return Optional.of(LoadedMagicTooltip(magicBall.copy()))
    }

    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        val wandLevel = stack.getComponent(UsefulMagicDataComponentTypes.WAND_LEVEL.get()) ?: 1
        val wandReduction = stack.getComponent(UsefulMagicDataComponentTypes.WAND_REDUCTION.get()) ?: 1.0

        run {
            val tag = stack.tag
            chargeDbg(
                "tooltip/${stack.item}",
                "level=${stack.getComponent(UsefulMagicDataComponentTypes.WAND_LEVEL.get())} " +
                    "reduction=${stack.getComponent(UsefulMagicDataComponentTypes.WAND_REDUCTION.get())} " +
                    "speed=${stack.getComponent(UsefulMagicDataComponentTypes.WAND_SPEED_FACTOR.get())} " +
                    "prefer=${stack.getComponent(UsefulMagicDataComponentTypes.WAND_PREFER.get())} " +
                    "magic=${stack.getComponent(UsefulMagicDataComponentTypes.WAND_MAGIC.get())} " +
                    "tagKeys=${tag?.allKeys}"
            )
        }
        val wandSpeedFactor = stack.getComponent(UsefulMagicDataComponentTypes.WAND_SPEED_FACTOR.get()) ?: 1.0

        val prefer = PreferMagicData.of(stack, "tooltip")

        val magicBall = getLoadedMagic(stack)

        val preferLevel = prefer.getPreferLevel(magicBall)
        val preferComponent = PreferMagicData.getPreferTranslateKey(preferLevel)
        val preferDamageAddition = (prefer.getDamageFactor(magicBall)) * 100
        val preferCDReduction = (-prefer.getCdReductionFactor(magicBall)) * 100
        val preferUsageReduction = (-prefer.getUsageFactor(magicBall)) * 100
        val preferManaReduction = (-prefer.getUsageFactor(magicBall)) * 100
        val colorSign = if (preferLevel > 0) "§a" else if (preferLevel < 0) "§c" else "§7"
        fun numSign(x: Double): String = if (x > 0) "+" else ""
        fun additionComponent(addition: Double, level: Int) = if (level == 0) Component.literal("") else {
            Component.literal(colorSign + numSign(addition) + "%.2f".format(addition) + "%")
        }

        tooltipComponents.add(Component.translatable("item.usefulmagic.wand.level", Component.literal("$wandLevel")))
        tooltipComponents.add(
            Component.translatable(
                "item.usefulmagic.wand.reduction",
                Component.literal("${(wandReduction * 100).roundToInt()}%")
            )
        )
        tooltipComponents.add(
            Component.translatable(
                "item.usefulmagic.wand.factor",
                Component.literal("${((1 / wandSpeedFactor.coerceAtLeast(1e-6)) * 100).roundToInt()}%"),
            )
        )

        tooltipComponents.add(
            Component.translatable(
                "item.usefulmagic.wand.cost",
                Component.literal("${MagicHelper.getManaCost(stack)}"),
                additionComponent(preferManaReduction, preferLevel)
            )
        )
        tooltipComponents.add(
            Component.translatable(
                "item.usefulmagic.wand.charging_time",
                Component.literal("${"%.2f".format(MagicHelper.getMaxChargingTick(stack) / 20.0)}秒"),
                additionComponent(preferUsageReduction, preferLevel)

            )
        )
        tooltipComponents.add(
            Component.translatable(
                "item.usefulmagic.wand.cd",
                Component.literal("${"%.2f".format(MagicHelper.getFinalCD(stack) / 20.0)}秒"),
                additionComponent(preferCDReduction, preferLevel)
            )
        )
        tooltipComponents.add(
            Component.translatable(
                "item.usefulmagic.wand.damage",
                Component.literal("%.2f".format(MagicHelper.getMagicDamage(stack))),
                additionComponent(preferDamageAddition, preferLevel)
            )
        )
        if (magicBall.isEmpty) {
            tooltipComponents.add(Component.translatable("item.usefulmagic.wand.usage_install"))
        } else {
            tooltipComponents.add(
                Component.translatable("item.usefulmagic.wand.load_magic").append(magicBall.hoverName)
            )
            tooltipComponents.add(Component.translatable("item.usefulmagic.wand.usage_exchange"))

            tooltipComponents.add(
                Component.translatable("item.usefulmagic.wand.prefer.info", preferComponent)
            )
        }

    }

    /**
     *
     * @param shooter
     * @param world
     * @param chargingTick
     */
    fun chargingTick(shooter: LivingEntity, world: Level, stack: ItemStack, chargingTick: Int) {
        if (shooter is Player && UsefulMagicEffects.isMagicSealed(shooter)) {
            stopCharge(shooter, world, stack, chargingTick, false)
            shooter.resetChargeState()
            return
        }

        val magicBall = getLoadedMagic(stack)
        if (magicBall.isEmpty) {
            chargeDbg("chargingTick/emptyBall", "chargingTick=$chargingTick 法杖里没有法球")
            return
        }
        val item = magicBall.item
        if (item !is MagicItem) {
            chargeDbg("chargingTick/notMagicItem", "chargingTick=$chargingTick item=${item::class.java.name}")
            return
        }
        val maxTick = MagicHelper.getMaxChargingTick(stack)

        if (maxTick < 1) {
            chargeDbg("chargingTick/maxTick<1", "chargingTick=$chargingTick maxTick=$maxTick（法球=${item::class.java.simpleName}）→ 早退，release 永不触发")
            return
        }
        chargeDbg(
            "chargingTick/tick",
            "chargingTick=$chargingTick maxTick=$maxTick ball=${item::class.java.simpleName} 命中release=${(chargingTick >= maxTick && chargingTick % maxTick == 0)}"
        )
        if (chargingTick >= maxTick && chargingTick % maxTick == 0) {

            chargeDbg(
                "release/fire",
                "tick=$chargingTick maxTick=$maxTick ball=${item::class.java.simpleName} " +
                    "damage=${MagicHelper.getMagicDamage(stack)} cd=${MagicHelper.getFinalCD(stack)}"
            )
            item.release(shooter, world, stack, magicBall, chargingTick)

            val cd = MagicHelper.getFinalCD(stack)
            if (cd > 0 && shooter is Player) {
                shooter.cooldowns.addCooldown(this, cd)
            }

            if (shooter is Player && !shooter.hasInfiniteMaterials()) {
                shooter.mana -= MagicHelper.getManaCost(stack)
            }
            if (cd > 0) {
                shooter.resetChargeState()
            } else {
                shooter.chargingTick = 0
            }
        } else {
            item.usingTick(shooter, stack, magicBall, world, chargingTick)
        }
    }

    /**
     * 松开按键或者出现其他情况时
     *
     * @param shooter
     * @param world
     * @param chargingTick
     * @param max
     */
    fun stopCharge(shooter: LivingEntity, world: Level, stack: ItemStack, chargingTick: Int, max: Boolean) {
        val magicBall = getLoadedMagic(stack)
        if (magicBall.isEmpty) {
            chargeDbg("stopCharge/emptyBall", "tick=$chargingTick 法杖里没有法球")
            return
        }
        if (!MagicHelper.isLevelEnough(stack, magicBall)) {
            chargeDbg("stopCharge/level", "tick=$chargingTick 等级不够")
            return
        }
        val item = magicBall.item as MagicItem
        chargeDbg("stopCharge", "tick=$chargingTick max=$max ball=${item::class.java.simpleName}")
        item.stopUse(shooter, world, stack, magicBall, chargingTick, max)
    }

    fun startCharge(shooter: LivingEntity, world: Level, stack: ItemStack) {
        if (shooter is Player && UsefulMagicEffects.isMagicSealed(shooter)) {
            chargeDbg("startCharge/sealed", "被禁魔")
            return
        }
        val magicBall = getLoadedMagic(stack)
        if (magicBall.isEmpty) {
            chargeDbg("startCharge/emptyBall", "法杖里没有法球（WAND_MAGIC 读不到）")
            return
        }
        if (!MagicHelper.isLevelEnough(stack, magicBall)) {
            chargeDbg("startCharge/level", "法杖等级不够 ball=${magicBall.item}")
            return
        }
        val item = magicBall.item as MagicItem
        chargeDbg(
            "startCharge/ok",
            "ball=${item::class.java.simpleName} maxTick=${MagicHelper.getMaxChargingTick(stack)} mana=${(shooter as? Player)?.mana ?: -1} cost=${MagicHelper.getManaCost(stack)}"
        )
        item.startUse(shooter, world, stack, magicBall)
    }

    fun getLoadedMagic(stack: ItemStack): ItemStack {
        return stack.getComponent(UsefulMagicDataComponentTypes.WAND_MAGIC.get())?.toStack() ?: ItemStack.EMPTY
    }

    fun setLoadedMagic(stack: ItemStack, magic: ItemStack) {
        val data = WandMagicData.fromStack(magic)
        if (data == null) {
            stack.removeComponent(UsefulMagicDataComponentTypes.WAND_MAGIC.get())
        } else {
            stack.setComponent(UsefulMagicDataComponentTypes.WAND_MAGIC.get(), data)
        }
    }

    fun canLoadMagic(wandStack: ItemStack, magicStack: ItemStack): Boolean {
        if (magicStack.isEmpty || magicStack.item !is MagicItem) {
            return false
        }
        val loadedMagic = getLoadedMagic(wandStack)
        if (!loadedMagic.isEmpty && ItemStack.isSameItemSameTags(loadedMagic, magicStack)) {
            return false
        }
        return MagicHelper.isLevelEnough(wandStack, magicStack)
    }

    fun cancelChargeIfNeeded(player: Player, wandStack: ItemStack) {
        if (!player.charging) {
            return
        }
        val chargedWand = player.chargedItem
        if (chargedWand.isEmpty || !ItemStack.isSameItemSameTags(chargedWand, wandStack)) {
            return
        }
        stopCharge(player, player.level(), chargedWand, player.chargingTick, false)
        player.resetChargeState()
    }

    private fun playRemoveOneSound(player: Player) {
        player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().random.nextFloat() * 0.4F)
    }

    private fun playInsertSound(player: Player) {
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().random.nextFloat() * 0.4F)
    }
}
