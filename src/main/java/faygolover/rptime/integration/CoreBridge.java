package faygolover.rptime.integration;

import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.api.event.RpSleepCountEvent;
import faygolover.rpcore.api.event.RpTimeSkipEvent;
import faygolover.rpcore.api.event.RpTimeSkipOfflineEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

/** Вызовы RP Core. Грузится, только если Core стоит ({@link CoreLink#present}). */
final class CoreBridge {
    private CoreBridge() {}

    /** Сообщить Core, что пауза и длина суток — наши (Core 0.11.0+). */
    static void registerSource() {
        RpCoreAPI.registerTimeSource(
                () -> faygolover.rptime.server.TimeService.server() == null || !faygolover.rptime.server.TimeService.state().paused(),
                running -> faygolover.rptime.server.TimeService.setPaused(!running),
                faygolover.rptime.api.RpTimeAPI::dayLengthSeconds);
    }

    static boolean timeRunning() {
        return RpCoreAPI.timeRunning();
    }

    static boolean timeControlEnabled() {
        return RpCoreAPI.timeControlEnabled();
    }

    static boolean setTimeRunning(boolean running) {
        return RpCoreAPI.setTimeRunning(running);
    }

    static CoreLink.Count count(ServerPlayer player, CoreLink.Count initial) {
        RpSleepCountEvent e = new RpSleepCountEvent(player, RpSleepCountEvent.Count.valueOf(initial.name()));
        MinecraftForge.EVENT_BUS.post(e);
        return CoreLink.Count.valueOf(e.count().name());
    }

    static void sleepSkip(ServerPlayer player, long seconds) {
        MinecraftForge.EVENT_BUS.post(new RpTimeSkipEvent(player, seconds, true, RpTimeSkipEvent.Scope.ALL));
    }

    static void sleepSkipOffline(MinecraftServer server, long seconds) {
        MinecraftForge.EVENT_BUS.post(new RpTimeSkipOfflineEvent(server, seconds, true, RpTimeSkipEvent.Scope.ALL));
    }
}
