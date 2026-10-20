package faygolover.rptime.client;

import faygolover.rptime.core.DayCycle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

/**
 * Ход времени на клиенте: те же сутки, что на сервере (приходят {@code CyclePacket}). Клиент считает сам, а ванильная
 * сверка раз в секунду поправляет расхождение — солнце идёт плавно без пакетов каждый тик. Сервер без RP Time
 * (сутки не пришли) — время идёт по-ванильному.
 */
public final class ClientTime {
    private ClientTime() {}

    private static volatile DayCycle cycle;
    private static double fraction;
    private static long lastSet = Long.MIN_VALUE;

    public static void set(DayCycle value) {
        cycle = value;
    }

    /** Сутки с сервера или {@code null}, если сервер без мода. */
    public static DayCycle cycle() {
        return cycle;
    }

    /** Миксин: время суток на следующий тик вместо {@code current + 1}. */
    public static long nextDayTime(ClientLevel level, long current) {
        DayCycle c = cycle;
        if (c == null) return current + 1;
        // Сервер прислал своё время (или сменился мир) — дробь от старого не тащим.
        if (current != lastSet) fraction = 0;
        double next = c.advance(current + fraction, 1);
        long whole = (long) Math.floor(next);
        fraction = next - whole;
        lastSet = whole;
        return whole;
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut e) {
        cycle = null;
        fraction = 0;
        lastSet = Long.MIN_VALUE;
    }
}
