package cn.coostack.usefulmagic.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.IntStream;

/**
 * 用「自定义堆叠上限」重算漏斗的「容器满」判定。
 *
 * <h2>⚠️ 为什么不能沿用上游的 {@code @ModifyVariable(STORE)}（真机崩过）</h2>
 *
 * 上游注入的是 {@code isFullContainer} 方法体里的 {@code STORE}。但 **1.20.1 的
 * {@code isFullContainer} 本体里根本没有 itemstack 局部变量** —— 判定被写进了 lambda：
 *
 * <pre>
 * isFullContainer(Container, Direction):
 *    0: aload_0
 *    1: aload_1
 *    2: invokestatic HopperBlockEntity.getSlots(Container, Direction)IntStream
 *    5: aload_0
 *    6: invokedynamic test:(Container)IntPredicate      // ← 真正的逻辑在这里
 *   11: invokeinterface IntStream.allMatch
 *   16: ireturn
 *
 * lambda$isFullContainer$1(Container, int):
 *    2: invokeinterface Container.getItem(I)Lnet/minecraft/world/item/ItemStack;
 *    7: astore_2                                        // ← STORE 在 lambda 里
 *    9: aload_2 ... getCount()
 *   13: aload_2 ... getMaxStackSize()
 *   16: if_icmplt
 * </pre>
 *
 * 于是 {@code @At(value="STORE", ordinal=0)} 会得到
 * {@code Critical injection failure ... (0/1) succeeded}。
 *
 * <p>改注入 {@code lambda$isFullContainer$1} 也不行：lambda 的官方名<b>没有进 refmap</b>
 * （Mixin 注解处理器不收集它），注释里的字符串在 SRG 环境里不会变成 {@code m_59377_}。
 *
 * <h2>这里的做法</h2>
 *
 * 直接改在 {@code isFullContainer} 的 RETURN 上重算。语义与上游那套「把局部变量
 * 替换成假 stack」的写法<b>完全等价</b>：
 *
 * <pre>
 * 原版:      allMatch(stack -> stack.getCount() >= stack.getMaxStackSize())
 * 上游改写后: allMatch(stack -> stack.getCount() >= min(container.getMaxStackSize(),
 *                                                    stack.getMaxStackSize()))
 * </pre>
 *
 * （推导：改写只在 {@code count >= min(maxC, stackMax)} 时把 count 抬到 stackMax，
 *  此后比较恒真；否则保持原 count，比较仍为假。）
 *
 * <p>只在「原版判定为满」时向下修正，因此不会影响原版的其他行为。
 * 依赖的 {@code getSlots} 是 private static，这里内联其等价实现，顺带避开了
 * private 成员访问（也就完全不需要 refmap 条目）。
 */
@Mixin(HopperBlockEntity.class)
public class HooperBlockEntityMixin {

    @Inject(method = "isFullContainer", at = @At("RETURN"), cancellable = true)
    private static void usefulmagic$relaxFullContainerCheck(Container container,
                                                            Direction direction,
                                                            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {

            return;
        }
        int containerMax = container.getMaxStackSize();
        boolean relaxedFull = usefulmagic$slots(container, direction).allMatch(slot -> {
            ItemStack stack = container.getItem(slot);
            return stack.getCount() >= Math.min(containerMax, stack.getMaxStackSize());
        });
        if (!relaxedFull) {
            cir.setReturnValue(false);
        }
    }

    /** 等价于 HopperBlockEntity.getSlots(Container, Direction)（private static，无法直接引用）。 */
    @Unique
    private static IntStream usefulmagic$slots(Container container, Direction direction) {
        return container instanceof WorldlyContainer worldly
                ? IntStream.of(worldly.getSlotsForFace(direction))
                : IntStream.range(0, container.getContainerSize());
    }
}
