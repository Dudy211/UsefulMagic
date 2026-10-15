package cn.coostack.usefulmagic.mixin;

import cn.coostack.usefulmagic.compat.mc1201.DataComponentBridge;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 数据组件后移植：Item 构造时把 Properties 上收集的组件值绑定到 Item。
 *
 * 1.20.5+ 的 `Item.Properties.component()` 会直接存进 Item；
 * 1.20.1 没这套，所以先记录、构造时转移。
 */
@Mixin(Item.class)
public class ItemPropertiesMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/item/Item$Properties;)V", at = @At("RETURN"))
    private void cooparticles$bindComponents(Item.Properties props, CallbackInfo ci) {
        DataComponentBridge.bindToItem((Item) (Object) this, props);
    }
}
