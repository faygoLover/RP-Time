package faygolover.rptime.mixin;

import faygolover.rptime.client.SignClock;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

/**
 * Часы на табличках ({@link SignClock}). {@code getRenderMessages} зовёт только отрисовка на клиенте; текст таблички
 * неизменяемый (правка — новый объект), поэтому «есть ли метки» считается один раз, а готовые строки сбрасываются
 * раз в игровую минуту.
 */
@Mixin(SignText.class)
public abstract class SignTextMixin {
    @Shadow private FormattedCharSequence[] renderMessages;
    @Shadow @Final private Component[] messages;

    /** 0 — ещё не смотрели, 1 — меток нет, 2 — есть. */
    @Unique private byte rptime$tags;
    @Unique private long rptime$stamp = Long.MIN_VALUE;

    @Inject(method = "getRenderMessages", at = @At("HEAD"))
    private void rptime$refresh(boolean filtered, Function<Component, FormattedCharSequence> split,
                                CallbackInfoReturnable<FormattedCharSequence[]> cir) {
        if (rptime$tags == 0) rptime$tags = (byte) (SignClock.hasTags(messages) ? 2 : 1);
        if (rptime$tags != 2) return;
        long stamp = SignClock.stamp();
        if (stamp != rptime$stamp) {
            rptime$stamp = stamp;
            renderMessages = null;
        }
    }

    @Redirect(method = "getRenderMessages", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/SignText;getMessage(IZ)Lnet/minecraft/network/chat/Component;"))
    private Component rptime$withClock(SignText self, int line, boolean filtered) {
        Component c = self.getMessage(line, filtered);
        return rptime$tags == 2 ? SignClock.apply(c) : c;
    }
}
