package cn.coostack.usefulmagic.mixin;

import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * {@link GameRules.BooleanValue#create(boolean)} 在 1.20.1 是**包级私有 static**
 * （SRG `m_46250_`），这里通过 Invoker 暴露出来，用于构造自定义布尔游戏规则的 Type。
 *
 * <h2>⚠️ 1.20.1 必须保留（2026-09-26 补）</h2>
 *
 * 同 {@link GameRulesInvoker}：直接调用会抛 `IllegalAccessError`，
 * 之前移植时漏搬了这个 Invoker，必须补回。
 *
 * ⇒ 与上游 `UsefulMagic-multi/common/.../mixin/GameRulesBooleanValueInvoker.java` 逐字一致。
 */
@Mixin(GameRules.BooleanValue.class)
public interface GameRulesBooleanValueInvoker {
    @Invoker("create")
    static GameRules.Type<GameRules.BooleanValue> usefulmagic$create(boolean defaultValue) {
        throw new AssertionError();
    }
}
