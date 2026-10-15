package cn.coostack.usefulmagic.mixin;

import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 用于跨加载器注册自定义原版游戏规则。
 *
 * <h2>⚠️ 1.20.1 必须保留这个 Invoker（2026-09-26 补）</h2>
 *
 * {@link GameRules#register} 在 1.20.1 是 **`private static`**
 * （SRG `m_46189_`），**直接调用会抛 `IllegalAccessError`**：
 *
 * <pre>
 * java.lang.IllegalAccessError: class cn.coostack.usefulmagic.gamerules.UsefulMagicGameRules
 *   tried to access private method 'net.minecraft.world.level.GameRules.m_46189_(...)'
 * </pre>
 *
 * 之前移植时**漏搬**了这个 Invoker，改成直接 `GameRules.register(...)` ——
 * Kotlin 编译期放行了（`kotlin-compiler` 对 MC 的访问检查较宽松），
 * 但字节码里留下的是 `invokestatic GameRules.m_46189_`，**运行时必炸**
 * （而 `UsefulMagic.init()` → `UsefulMagicGameRules.init()` 会触发 `&lt;clinit&gt;`）。
 *
 * Mixin 的 `@Invoker` 会在目标类里生成一个合法的桥接调用，**不受 private 限制**。
 *
 * ⇒ 与上游 `UsefulMagic-multi/common/.../mixin/GameRulesInvoker.java` 逐字一致。
 */
@Mixin(GameRules.class)
public interface GameRulesInvoker {
    @Invoker("register")
    static <T extends GameRules.Value<T>> GameRules.Key<T> usefulmagic$register(
            String name,
            GameRules.Category category,
            GameRules.Type<T> type
    ) {
        throw new AssertionError();
    }
}
