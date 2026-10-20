package faygolover.rptime.mixin;

import faygolover.rptime.client.ClientTime;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Ход времени суток на клиенте — так же, как на сервере ({@link ClientTime}). */
@Mixin(ClientLevel.class)
public abstract class ClientLevelTimeMixin {
    @ModifyArg(method = "tickTime", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;setDayTime(J)V"))
    private long rptime$nextDayTime(long vanillaNext) {
        return ClientTime.nextDayTime((ClientLevel) (Object) this, vanillaNext - 1);
    }
}
