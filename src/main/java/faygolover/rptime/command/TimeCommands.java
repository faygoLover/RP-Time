package faygolover.rptime.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import faygolover.rptime.core.Clock;
import faygolover.rptime.core.DayCycle;
import faygolover.rptime.core.Durations;
import faygolover.rptime.server.TimeService;
import faygolover.rptime.server.TimeState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import faygolover.rptime.integration.CoreLink;
import faygolover.rptime.server.SleepService;
import java.util.Collection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * Подкоманды к ванильной {@code /time} (её {@code set|add|query} не трогаем):
 * <ul>
 *   <li>{@code /time info} — который час, день, длины, скорость;</li>
 *   <li>{@code /time length day <длина> [night <длина>]}, {@code /time length night <длина>}, {@code /time length reset};</li>
 *   <li>{@code /time speed <множитель>|reset} — временно быстрее или медленнее, длины не меняются;</li>
 *   <li>{@code /time pause}, {@code /time resume} — с RP Core (механика {@code /rptime} включена) это и есть
 *   {@code /rptime stop|start};</li>
 *   <li>{@code /time sleep <длина>|reset} — сколько длится ночь, пока спят все;</li>
 *   <li>{@code /time growth <сутки>|reset} — за сколько игровых суток растёт пшеница;</li>
 *   <li>{@code /time exclude <игроки>|clear} — не считать до утра (неактивные держат ночь).</li>
 * </ul>
 * Всё меняется для мира и переживает перезапуск; значения по умолчанию — в {@code config/rptime-common.toml}.
 */
public final class TimeCommands {
    private TimeCommands() {}

    /** Длина дня или ночи: от 10 секунд до недели. */
    private static final double MIN_SECONDS = 10, MAX_SECONDS = 7 * 24 * 3600;
    private static final String[] LENGTH_HINTS = {"2h", "1h", "90m", "30m", "10m"};

    private static final SimpleCommandExceptionType BAD_LENGTH =
            new SimpleCommandExceptionType(Component.translatable("rptime.cmd.bad_length"));

    public static void register(RegisterCommandsEvent e) {
        // Узел «time» уже есть (ванильный, уровень прав 2) — brigadier добавит наши ветки к нему.
        e.getDispatcher().register(Commands.literal("time").requires(s -> s.hasPermission(2))
                .then(Commands.literal("info").executes(TimeCommands::info))
                .then(Commands.literal("length")
                        .executes(TimeCommands::info)
                        .then(Commands.literal("day")
                                .then(Commands.argument("day", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(LENGTH_HINTS, b))
                                        .executes(c -> length(c, true, false))
                                        .then(Commands.literal("night")
                                                .then(Commands.argument("night", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(LENGTH_HINTS, b))
                                                        .executes(c -> length(c, true, true))))))
                        .then(Commands.literal("night")
                                .then(Commands.argument("night", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(LENGTH_HINTS, b))
                                        .executes(c -> length(c, false, true))))
                        .then(Commands.literal("reset").executes(TimeCommands::lengthReset)))
                .then(Commands.literal("speed")
                        .then(Commands.literal("reset").executes(c -> speed(c, 1)))
                        .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.01, 1000))
                                .executes(c -> speed(c, DoubleArgumentType.getDouble(c, "multiplier")))))
                .then(Commands.literal("pause").executes(c -> pause(c, true)))
                .then(Commands.literal("resume").executes(c -> pause(c, false)))
                .then(Commands.literal("sleep")
                        .executes(TimeCommands::info)
                        .then(Commands.literal("reset").executes(c -> sleep(c, -1)))
                        .then(Commands.argument("night", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"3m", "5m", "2m", "1m"}, b))
                                .executes(c -> sleep(c, parseLength(StringArgumentType.getString(c, "night"))))))
                .then(Commands.literal("growth")
                        .executes(TimeCommands::info)
                        .then(Commands.literal("reset").executes(c -> growth(c, -1)))
                        .then(Commands.argument("days", DoubleArgumentType.doubleArg(0.05, 365))
                                .executes(c -> growth(c, DoubleArgumentType.getDouble(c, "days")))))
                .then(Commands.literal("exclude")
                        .then(Commands.literal("clear").executes(TimeCommands::excludeClear))
                        .then(Commands.argument("targets", EntityArgument.players()).executes(TimeCommands::exclude))));
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        CommandSourceStack src = c.getSource();
        DayCycle cycle = TimeService.cycle();
        TimeState s = TimeService.state();
        long dayTime = src.getServer().overworld().getDayTime();
        Component phase = Component.translatable(cycle.isDay(dayTime) ? "rptime.phase.day" : "rptime.phase.night");
        src.sendSuccess(() -> Component.translatable("rptime.info.now", Clock.format(dayTime), Clock.day(dayTime), phase)
                .withStyle(ChatFormatting.GOLD), false);
        Component from = Component.translatable(s.dayTicks() > 0 || s.nightTicks() > 0 ? "rptime.info.from_world" : "rptime.info.from_config");
        src.sendSuccess(() -> Component.translatable("rptime.info.lengths",
                Durations.formatTicks(cycle.dayTicks()), Durations.formatTicks(cycle.nightTicks()), from), false);
        src.sendSuccess(() -> Component.translatable("rptime.info.speed", formatSpeed(s.speed())), false);
        src.sendSuccess(() -> Component.translatable("rptime.info.sleep", Durations.formatTicks(TimeService.sleepTicks()),
                Component.translatable(TimeService.sleeping() ? "rptime.info.sleep_now" : "rptime.info.sleep_idle")), false);
        src.sendSuccess(() -> Component.translatable("rptime.info.growth", formatNumber(TimeService.wheatDays())), false);
        if (s.paused()) src.sendSuccess(() -> Component.translatable("rptime.info.paused").withStyle(ChatFormatting.RED), false);
        if (CoreLink.present() && !CoreLink.timeRunning())
            src.sendSuccess(() -> Component.translatable("rptime.info.core_stopped").withStyle(ChatFormatting.RED), false);
        if (!src.getServer().getGameRules().getBoolean(GameRules.RULE_DAYLIGHT))
            src.sendSuccess(() -> Component.translatable("rptime.info.daylight_off").withStyle(ChatFormatting.RED), false);
        return (int) Clock.day(dayTime);
    }

    private static int length(CommandContext<CommandSourceStack> c, boolean day, boolean night) throws CommandSyntaxException {
        TimeState s = TimeService.state();
        long dayTicks = day ? parseLength(StringArgumentType.getString(c, "day")) : TimeService.cycle().dayTicks();
        long nightTicks = night ? parseLength(StringArgumentType.getString(c, "night")) : TimeService.cycle().nightTicks();
        s.setLengths(dayTicks, nightTicks);
        TimeService.refresh();
        c.getSource().sendSuccess(() -> Component.translatable("rptime.cmd.length_set",
                Durations.formatTicks(dayTicks), Durations.formatTicks(nightTicks)).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int lengthReset(CommandContext<CommandSourceStack> c) {
        TimeService.state().setLengths(-1, -1);
        TimeService.refresh();
        DayCycle cycle = TimeService.cycle();
        c.getSource().sendSuccess(() -> Component.translatable("rptime.cmd.length_reset",
                Durations.formatTicks(cycle.dayTicks()), Durations.formatTicks(cycle.nightTicks())).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int speed(CommandContext<CommandSourceStack> c, double value) {
        TimeService.state().setSpeed(value);
        TimeService.refresh();
        c.getSource().sendSuccess(() -> Component.translatable("rptime.cmd.speed_set", formatSpeed(value)).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int pause(CommandContext<CommandSourceStack> c, boolean paused) {
        // С RP Core пауза одна на всех: /time pause = /rptime stop (солнце, раны, голод — всё стоит).
        boolean viaCore = CoreLink.setTimeRunning(!paused);
        TimeService.state().setPaused(!viaCore && paused);
        TimeService.tick();
        TimeService.refresh();
        c.getSource().sendSuccess(() -> Component.translatable(paused ? "rptime.cmd.paused" : "rptime.cmd.resumed").withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int sleep(CommandContext<CommandSourceStack> c, long ticks) {
        TimeService.state().setSleepTicks(ticks);
        TimeService.refresh();
        c.getSource().sendSuccess(() -> Component.translatable("rptime.cmd.sleep_set",
                Durations.formatTicks(TimeService.sleepTicks())).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int growth(CommandContext<CommandSourceStack> c, double days) {
        TimeService.state().setWheatDays(days);
        c.getSource().sendSuccess(() -> Component.translatable("rptime.cmd.growth_set",
                formatNumber(TimeService.wheatDays())).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int exclude(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        for (ServerPlayer p : targets) {
            SleepService.exclude(p);
            c.getSource().sendSuccess(() -> Component.translatable("rptime.cmd.excluded", p.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
        }
        return targets.size();
    }

    private static int excludeClear(CommandContext<CommandSourceStack> c) {
        int n = SleepService.clearExcluded();
        c.getSource().sendSuccess(() -> Component.translatable("rptime.cmd.exclude_cleared", n).withStyle(ChatFormatting.GOLD), true);
        return n;
    }

    private static long parseLength(String text) throws CommandSyntaxException {
        double seconds = Durations.parseSeconds(text);
        if (seconds < MIN_SECONDS || seconds > MAX_SECONDS) throw BAD_LENGTH.create();
        return Math.round(seconds * 20);
    }

    private static String formatNumber(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    private static String formatSpeed(double v) {
        return v == Math.rint(v) ? "×" + (long) v : "×" + String.format(java.util.Locale.ROOT, "%.2f", v);
    }
}
