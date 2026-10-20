package faygolover.rptime.mixin;

import faygolover.rptime.TimeConfig;
import faygolover.rptime.server.TimeService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Время суток на сервере: ход по длинам дня и ночи ({@link TimeService}); ванильный мгновенный пропуск ночи и его
 * строка «спят N из M» выключены — вместо них ускорение сна ({@code SleepService}).
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelTimeMixin {
    @ModifyArg(method = "tickTime", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    private long rptime$nextDayTime(long vanillaNext) {
        return TimeService.nextDayTime((ServerLevel) (Object) this, vanillaNext - 1);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/SleepStatus;areEnoughSleeping(I)Z"))
    private boolean rptime$noVanillaSkip(SleepStatus status, int percent) {
        return !TimeConfig.SLEEP_ENABLED.get() && status.areEnoughSleeping(percent);
    }

    @Inject(method = "announceSleepStatus", at = @At("HEAD"), cancellable = true)
    private void rptime$noVanillaAnnounce(CallbackInfo ci) {
        if (TimeConfig.SLEEP_ENABLED.get()) ci.cancel();
    }
}
