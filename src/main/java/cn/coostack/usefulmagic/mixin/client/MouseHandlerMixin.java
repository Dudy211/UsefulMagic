package cn.coostack.usefulmagic.mixin.client;

import cn.coostack.usefulmagic.gui.magicexchange.MagicExchangeOverlay;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.SmoothDouble;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在法术轮盘活动时接管鼠标滚轮和视角转动，同时保留原版鼠标帧的收尾处理。
 *
 * <h2>1.21 → 1.20.1 差异（勿回退）</h2>
 * <ul>
 *   <li>1.20.1 的 {@code MouseHandler} **没有** {@code handleAccumulatedMovement} 方法
 *       （1.21 才把「累计位移 → 转向」抽成独立方法）。1.20.1 里 {@code accumulatedDX/DY}
 *       的消费点在 {@code onMove(long,double,double)} 内部，所以要挂在 {@code onMove} 的 HEAD。</li>
 *   <li>1.20.1 的 {@code turnPlayer} 是**无参** {@code ()V}（1.21 是 {@code (double frameTime)}），
 *       handler 不能声明那个 double，否则 Mixin 找不到目标。</li>
 * </ul>
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    /** 原版水平方向的平滑镜头状态。 */
    @Shadow
    @Final
    private SmoothDouble smoothTurnX;

    /** 原版垂直方向的平滑镜头状态。 */
    @Shadow
    @Final
    private SmoothDouble smoothTurnY;

    /** 当前鼠标处理帧是否需要禁止玩家视角转动。 */
    @Unique
    private boolean usefulmagic$suppressCameraTurn;

    /** 在法术轮盘活动时消费滚轮输入，避免同时切换快捷栏。 */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void usefulmagic$onScroll(long windowPointer, double xOffset, double yOffset, CallbackInfo ci) {
        if (MagicExchangeOverlay.onMouseScroll(yOffset)) {
            ci.cancel();
        }
    }

    /**
     * 在处理累计鼠标位移前锁定本帧的视角抑制状态。
     *
     * <p>{@code onMove} 是 1.20.1 里「消费 accumulatedDX/DY」的入口，
     * 在这里采样等价于 1.21 挂在 {@code handleAccumulatedMovement} 的语义。
     */
    @Inject(method = "onMove", at = @At("HEAD"))
    private void usefulmagic$captureCameraTurnSuppression(long windowPointer, double xOffset, double yOffset, CallbackInfo ci) {
        usefulmagic$suppressCameraTurn = MagicExchangeOverlay.shouldSuppressCameraTurn();
    }

    /**
     * 禁止轮盘选择期间的玩家转向，并清空平滑镜头惯性，防止关闭轮盘后视角漂移。
     *
     * <p>1.20.1 的 {@code turnPlayer()} 无参，所以这里也没有 {@code frameTime}。
     */
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void usefulmagic$suppressCameraTurn(CallbackInfo ci) {
        if (usefulmagic$suppressCameraTurn) {
            smoothTurnX.reset();
            smoothTurnY.reset();
            ci.cancel();
        }
    }
}
