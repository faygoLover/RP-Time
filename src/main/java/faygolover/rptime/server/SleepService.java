package faygolover.rptime.server;

import faygolover.rptime.TimeConfig;
import faygolover.rptime.core.DayCycle;
import faygolover.rptime.integration.CoreLink;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Сон вместо ванильного пропуска ночи (план §5). Ночь идёт со скоростью сна, только когда спят все игроки в выживании
 * и приключении; ГМы (творческий, наблюдатель), мёртвые, исключённые на ночь и те, кого моды семейства велят не считать
 * (клиническая смерть Medicine), в счёт не входят. Кто-то встал — сразу обычный ход.
 * <p>
 * Пока ускорено, раз в секунду спящим прокручивается «лишнее» время (раны, голод, мышцы — через RP Core); вышедшим
 * (тела Medicine) — одним разом, когда ускорение кончится. Ванилла сама будит спящих утром; если утро настало, а они
 * ещё спят (гроза), будим сами.
 */
public final class SleepService {
    private SleepService() {}

    private static final Set<UUID> EXCLUDED = new HashSet<>();
    private static final List<ServerPlayer> SLEEPERS = new ArrayList<>();
    private static final List<ServerPlayer> BLOCKERS = new ArrayList<>();

    private static boolean accelerating;
    private static boolean wasDay = true;
    private static int lastSleepers = -1, lastTotal = -1;
    private static boolean lastAll;
    private static int flushTimer;
    private static double pendingTicks;
    private static long offlineSeconds;
    private static int notifyTimer;

    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = e.getServer();
        TimeService.tick();
        if (TimeService.server() == null) return;
        ServerLevel overworld = server.overworld();
        DayCycle cycle = TimeService.cycle();
        long dayTime = overworld.getDayTime();
        boolean day = cycle.isDay(dayTime);
        boolean morning = day && !wasDay;
        wasDay = day;
        if (morning) EXCLUDED.clear();

        if (!TimeConfig.SLEEP_ENABLED.get()) {
            if (accelerating) stop(server, false);
            return;
        }
        if (morning && accelerating) {
            stop(server, true);
            return;
        }

        tally(server);
        boolean daylight = server.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        // Как в ванилле: спать до утра — только ночью или в грозу (больничная койка «спит» и днём).
        boolean sleepTime = !day || overworld.isThundering();
        boolean all = !SLEEPERS.isEmpty() && BLOCKERS.isEmpty() && daylight && sleepTime && !TimeService.paused();
        if (all && !accelerating) {
            accelerating = true;
            flushTimer = 0;
            TimeService.takeExtraTicks();
            TimeService.setSleeping(true);
        } else if (!all && accelerating) {
            stop(server, overworld.isDay() || day);
        }
        if (accelerating && ++flushTimer >= 20) {
            flushTimer = 0;
            flush(SLEEPERS);
        }
        announce(all);
        notifyInactive(server, all);
    }

    /** Кто как считается сейчас — в {@link #SLEEPERS} и {@link #BLOCKERS}. */
    private static void tally(MinecraftServer server) {
        SLEEPERS.clear();
        BLOCKERS.clear();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || p.isCreative() || p.isDeadOrDying() || EXCLUDED.contains(p.getUUID())) continue;
            CoreLink.Count c = CoreLink.count(p, p.isSleepingLongEnough() ? CoreLink.Count.SLEEPING : CoreLink.Count.BLOCKS);
            if (c == CoreLink.Count.SLEEPING) SLEEPERS.add(p);
            else if (c == CoreLink.Count.BLOCKS) BLOCKERS.add(p);
        }
    }

    /** Накопленное «лишнее» время — спящим сейчас, и в копилку вышедшим. */
    private static void flush(List<ServerPlayer> sleepers) {
        pendingTicks += TimeService.takeExtraTicks();
        long seconds = (long) Math.floor(pendingTicks / 20);
        if (seconds <= 0) return;
        pendingTicks -= seconds * 20;
        for (ServerPlayer p : sleepers) CoreLink.sleepSkip(p, seconds);
        offlineSeconds += seconds;
    }

    /** Конец ускорения: остаток спящим, копилку вышедшим; утро — разбудить, погода, событие конца сна. */
    private static void stop(MinecraftServer server, boolean morning) {
        accelerating = false;
        TimeService.setSleeping(false);
        tally(server);
        flush(SLEEPERS);
        pendingTicks = 0;
        CoreLink.sleepSkipOffline(server, offlineSeconds);
        offlineSeconds = 0;
        if (!morning) return;
        ServerLevel overworld = server.overworld();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSleeping()) p.stopSleepInBed(false, true);
        }
        ForgeEventFactory.onSleepFinished(overworld, overworld.getDayTime(), overworld.getDayTime());
        if (TimeConfig.CLEAR_WEATHER.get() && server.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE) && overworld.isRaining())
            overworld.setWeatherParameters(0, 0, false, false);
    }

    /** Строка над хотбаром: «Спят: 2 из 3» или «Все спят — ночь идёт быстрее». Только при смене. */
    private static void announce(boolean all) {
        int sleepers = SLEEPERS.size(), total = sleepers + BLOCKERS.size();
        if (sleepers == lastSleepers && total == lastTotal && all == lastAll) return;
        lastSleepers = sleepers;
        lastTotal = total;
        lastAll = all;
        if (sleepers == 0) return;
        Component msg = all ? Component.translatable("rptime.sleep.all")
                : Component.translatable("rptime.sleep.status", sleepers, total);
        for (ServerPlayer p : SLEEPERS) p.displayClientMessage(msg, true);
        for (ServerPlayer p : BLOCKERS) p.displayClientMessage(msg, true);
    }

    /** Все спят, а ночь держат только неактивные — ГМам раз в минуту, с подсказкой «проспать до утра» ({@code /rptime sleep set 6:00}). */
    private static void notifyInactive(MinecraftServer server, boolean all) {
        if (all || SLEEPERS.isEmpty() || BLOCKERS.isEmpty()) {
            notifyTimer = 0;
            return;
        }
        long idleMs = Math.round(TimeConfig.INACTIVE_MINUTES.get() * 60_000);
        long now = Util.getMillis();
        for (ServerPlayer p : BLOCKERS) {
            if (now - p.getLastActionTime() < idleMs) {
                notifyTimer = 0;
                return;
            }
        }
        if (notifyTimer-- > 0) return;
        notifyTimer = 20 * 60;
        MutableComponent msg = Component.translatable("rptime.sleep.inactive").withStyle(ChatFormatting.GOLD);
        for (ServerPlayer p : BLOCKERS) {
            String name = p.getGameProfile().getName();
            msg.append(" ").append(Component.literal(name).withStyle(ChatFormatting.WHITE)).append(" ")
                    .append(Component.translatable("rptime.sleep.exclude_button").withStyle(Style.EMPTY
                            .withColor(ChatFormatting.GREEN).withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/rptime sleep set 6:00"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("rptime.sleep.exclude_hover", name)))));
        }
        for (ServerPlayer gm : server.getPlayerList().getPlayers()) {
            if (gm.hasPermissions(2)) gm.sendSystemMessage(msg);
        }
    }

    /** {@code /time exclude}: не считать до утра. */
    public static boolean exclude(ServerPlayer p) {
        return EXCLUDED.add(p.getUUID());
    }

    public static int clearExcluded() {
        int n = EXCLUDED.size();
        EXCLUDED.clear();
        return n;
    }

    public static boolean accelerating() {
        return accelerating;
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        EXCLUDED.clear();
        SLEEPERS.clear();
        BLOCKERS.clear();
        accelerating = false;
        wasDay = true;
        lastSleepers = lastTotal = -1;
        lastAll = false;
        pendingTicks = 0;
        offlineSeconds = 0;
        notifyTimer = 0;
    }
}
