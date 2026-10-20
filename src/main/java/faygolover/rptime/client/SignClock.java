package faygolover.rptime.client;

import faygolover.rptime.core.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Optional;

/**
 * Часы на табличках (план §8): метки в тексте таблички заменяются при отрисовке на клиенте — серверу ни работы, ни
 * трафика. Стиль строки (цвет, жирность) сохраняется. Пересчёт — раз в игровую минуту, только у видимых табличек
 * с метками ({@code SignTextMixin}).
 * <ul>
 *   <li>{@code {время}} или {@code {time}} — «14:05»;</li>
 *   <li>{@code {день}} или {@code {day}} — номер дня с первого.</li>
 * </ul>
 */
public final class SignClock {
    private SignClock() {}

    private static final String[] TAGS = {"{время}", "{time}", "{день}", "{day}"};

    public static boolean hasTags(Component[] lines) {
        for (Component c : lines) {
            String s = c.getString();
            if (s.indexOf('{') < 0) continue;
            for (String t : TAGS) if (s.contains(t)) return true;
        }
        return false;
    }

    private static long dayTime() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getDayTime();
    }

    /** Меняется раз в игровую минуту (1000 тиков = час). */
    public static long stamp() {
        return Math.floorDiv(dayTime() * 60, 1000);
    }

    public static Component apply(Component line) {
        if (line.getString().indexOf('{') < 0) return line;
        long t = dayTime();
        String time = Clock.format(t);
        String day = Long.toString(Clock.day(t));
        MutableComponent out = Component.empty();
        line.visit((style, text) -> {
            String r = text.replace("{время}", time).replace("{time}", time).replace("{день}", day).replace("{day}", day);
            out.append(Component.literal(r).withStyle(style));
            return Optional.empty();
        }, line.getStyle());
        return out;
    }
}
