package faygolover.rptime.integration;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Мягкая связь с RP Core. Без Core — ванильные правила: своя пауза, в кровати — спит, ран и голода во сне не крутим.
 * Классы Core трогает только {@link CoreBridge}, и только если Core стоит.
 */
public final class CoreLink {
    private CoreLink() {}

    /** Как считать игрока для ускорения ночи. */
    public enum Count { SLEEPING, BLOCKS, IGNORED }

    private static Boolean present;

    public static boolean present() {
        if (present == null) present = ModList.get() != null && ModList.get().isLoaded("rpcore");
        return present;
    }

    private static boolean source;

    /**
     * Стать источником времени для Core: пауза ({@code /time pause} = {@code /rptime stop}) и длина суток — наши,
     * все моды семейства видят одну паузу. Старый Core (до 0.11.0) этого не умеет — тогда как раньше.
     */
    public static void registerSource() {
        if (!present()) return;
        try {
            CoreBridge.registerSource();
            source = true;
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            source = false;
        }
    }

    /** Core берёт паузу у нас. */
    public static boolean isSource() {
        return source;
    }

    /** Идёт ли время RP ({@code /rptime}). Без Core — всегда. */
    public static boolean timeRunning() {
        return !present() || CoreBridge.timeRunning();
    }

    /** Включена ли в Core механика {@code /rptime start|stop}. */
    public static boolean timeControlEnabled() {
        return present() && CoreBridge.timeControlEnabled();
    }

    /** Пустить или остановить время RP Core; {@code false} — механика выключена или Core нет. */
    public static boolean setTimeRunning(boolean running) {
        return present() && CoreBridge.setTimeRunning(running);
    }

    /** Спросить моды семейства (Medicine: койка, без сознания, клиническая смерть). */
    public static Count count(ServerPlayer player, Count initial) {
        return present() ? CoreBridge.count(player, initial) : initial;
    }

    /** Прокрутить спящему проспанное время (раны, голод, мышцы). */
    public static void sleepSkip(ServerPlayer player, long seconds) {
        if (present() && seconds > 0) CoreBridge.sleepSkip(player, seconds);
    }

    /** То же для всех, кого нет в сети (тела Medicine). */
    public static void sleepSkipOffline(MinecraftServer server, long seconds) {
        if (present() && seconds > 0) CoreBridge.sleepSkipOffline(server, seconds);
    }
}
