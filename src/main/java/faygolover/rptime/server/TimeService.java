package faygolover.rptime.server;

import faygolover.rptime.TimeConfig;
import faygolover.rptime.core.DayCycle;
import faygolover.rptime.integration.CoreLink;
import faygolover.rptime.network.CyclePacket;
import faygolover.rptime.network.TimeNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Ход времени на сервере. Время суток ведёт только верхний мир (остальные берут его у него), поэтому и состояние одно.
 * Миксин в {@code ServerLevel.tickTime} спрашивает {@link #nextDayTime} вместо «+1».
 * <p>
 * Пауза — своя ({@code /time pause}) или RP Core ({@code /rptime stop}): любая останавливает солнце.
 * Сон — {@link SleepService} включает {@link #setSleeping}: время идёт ровно со скоростью сна.
 */
public final class TimeService {
    private TimeService() {}

    private static MinecraftServer server;
    private static DayCycle cycle = DayCycle.VANILLA;
    private static boolean sleeping;
    private static boolean corePaused;

    /** Дробная часть времени суток и что мы сами поставили в прошлый тик: не совпало — время меняли со стороны. */
    private static double fraction;
    private static long lastSet = Long.MIN_VALUE;
    /** Сколько тиков времени суток прошло за последний тик (дробно) — для роста растений. */
    private static double lastDelta;
    /**
     * Сколько реальных тиков «обычного хода» накопилось сверх прошедших — пока спят все. Это время RP-моды
     * прокручивают спящим (раны, голод, мышцы), {@link SleepService} забирает его раз в секунду.
     */
    private static double extraTicks;
    /** Правило {@code doDaylightCycle}; замер хода, когда оно выключено. */
    private static boolean daylight = true;
    private static long prevDayTime = Long.MIN_VALUE;
    private static double measuredRate;

    /** Миксин: время суток на следующий тик вместо {@code current + 1}. */
    public static long nextDayTime(ServerLevel level, long current) {
        if (server == null || level.dimension() != Level.OVERWORLD) return current + 1;
        if (current != lastSet) fraction = 0;
        double from = current + fraction;
        double next = cycle.advance(from, 1);
        lastDelta = next - from;
        if (cycle.sleeping() && lastDelta > 0) extraTicks += lastDelta / cycle.baseRate(from) - 1;
        long whole = (long) Math.floor(next);
        fraction = next - whole;
        lastSet = whole;
        return whole;
    }

    public static DayCycle cycle() {
        return cycle;
    }

    public static TimeState state() {
        return TimeState.get(server);
    }

    public static MinecraftServer server() {
        return server;
    }

    /**
     * Тиков времени суток за реальный тик сейчас; 0 — время стоит. Ведём время сами — точно; {@code doDaylightCycle}
     * выключено (время двигает кто-то другой, например датапак) — замер по факту, сглаженный.
     */
    public static double lastDelta() {
        return daylight ? lastDelta : measuredRate;
    }

    /** Забрать накопленное «лишнее» время сна, реальные тики. */
    static double takeExtraTicks() {
        double t = extraTicks;
        extraTicks = 0;
        return t;
    }

    /** Стоит ли время: своя пауза, {@code /rptime stop} или выключенный {@code doDaylightCycle}. */
    public static boolean paused() {
        return cycle.paused();
    }

    public static boolean sleeping() {
        return cycle.sleeping();
    }

    static void setSleeping(boolean value) {
        if (sleeping == value) return;
        sleeping = value;
        refresh();
    }

    /** Длина ночи во сне, реальные тики. */
    public static long sleepTicks() {
        long t = server == null ? -1 : state().sleepTicks();
        return t > 0 ? t : minutesToTicks(TimeConfig.SLEEP_NIGHT_MINUTES.get());
    }

    /** За сколько игровых суток растёт пшеница. */
    public static double wheatDays() {
        double d = server == null ? -1 : state().wheatDays();
        return d > 0 ? d : TimeConfig.WHEAT_DAYS.get();
    }

    /** Поставить или снять свою паузу (её же видит Core, если мы источник). */
    public static void setPaused(boolean paused) {
        if (server == null) return;
        state().setPaused(paused);
        tick();
        refresh();
    }

    /** Пересобрать сутки из конфига и мира, разослать клиентам. Звать после любой смены. */
    public static void refresh() {
        if (server == null) return;
        TimeState s = state();
        long day = s.dayTicks > 0 ? s.dayTicks : minutesToTicks(TimeConfig.DAY_MINUTES.get());
        long night = s.nightTicks > 0 ? s.nightTicks : minutesToTicks(TimeConfig.NIGHT_MINUTES.get());
        int dayStart = TimeConfig.DAY_START.get();
        int nightStart = TimeConfig.NIGHT_START.get();
        if (dayStart == nightStart) nightStart = DayCycle.VANILLA.nightStart();
        boolean stopped = s.paused || corePaused;
        DayCycle base = new DayCycle(dayStart, nightStart, day, night, stopped ? 0 : s.speed, 0);
        // Ночь во сне: ночь целиком (в тиках времени суток) — за длину ночи во сне.
        cycle = sleeping && !stopped ? base.withSleepRate((double) base.nightLength() / sleepTicks()) : base;
        TimeNetwork.sendToAll(server, new CyclePacket(cycle));
    }

    static long minutesToTicks(double minutes) {
        return Math.max(1, Math.round(minutes * 60 * 20));
    }

    /** Конец тика сервера: пауза RP Core ({@code /rptime stop}) — опрос, дёшево; замер хода времени. */
    public static void tick() {
        if (server == null) return;
        daylight = server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
        long now = server.overworld().getDayTime();
        long d = prevDayTime == Long.MIN_VALUE ? 0 : now - prevDayTime;
        prevDayTime = now;
        // Скачки (/time set, сон без мода) не в счёт.
        if (d >= 0 && d <= 100) measuredRate += (d - measuredRate) * 0.01;
        boolean p = CoreLink.present() && !CoreLink.timeRunning();
        if (p != corePaused) {
            corePaused = p;
            refresh();
        }
    }

    public static void onServerStarted(ServerStartedEvent e) {
        server = e.getServer();
        fraction = 0;
        lastSet = Long.MIN_VALUE;
        sleeping = false;
        extraTicks = 0;
        prevDayTime = Long.MIN_VALUE;
        measuredRate = 0;
        daylight = true;
        corePaused = CoreLink.present() && !CoreLink.timeRunning();
        refresh();
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        server = null;
        cycle = DayCycle.VANILLA;
        sleeping = false;
        corePaused = false;
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) TimeNetwork.send(sp, new CyclePacket(cycle));
    }

    public static void onConfigReload(ModConfigEvent.Reloading e) {
        if (e.getConfig().getSpec() == TimeConfig.SPEC && server != null) server.execute(TimeService::refresh);
    }
}
