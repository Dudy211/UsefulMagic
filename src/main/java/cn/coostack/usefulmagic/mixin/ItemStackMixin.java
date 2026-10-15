package cn.coostack.usefulmagic.mixin;

import cn.coostack.usefulmagic.compat.mc1201.DataComponentBridge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 数据组件后移植：新建 ItemStack 时，把 Item 上绑定的组件值写进 NBT。
 *
 * 对应 1.20.5+ 里「ItemStack 从 Item 的组件初始化」的行为。
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/ItemLike;)V",
            at = @At("RETURN")
    )
    private void cooparticles$applyComponents(ItemLike item, CallbackInfo ci) {
        DataComponentBridge.INSTANCE.applyToStack((ItemStack) (Object) this);
    }

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/ItemLike;I)V",
            at = @At("RETURN")
    )
    private void cooparticles$applyComponentsWithCount(ItemLike item, int count, CallbackInfo ci) {
        DataComponentBridge.INSTANCE.applyToStack((ItemStack) (Object) this);
    }
}
