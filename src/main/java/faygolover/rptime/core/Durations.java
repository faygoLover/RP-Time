package faygolover.rptime.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Длительности в командах — как в {@code /rptime} RP Core: {@code 2h}, {@code 90m}, {@code 1h30m}, {@code 45s}, просто число — часы. */
public final class Durations {
    private Durations() {}

    private static final Pattern PART = Pattern.compile("(\\d+(?:\\.\\d+)?)([hms]?)");

    /** Строка → секунды; не разобралась — -1. */
    public static double parseSeconds(String s) {
        String text = s.toLowerCase(Locale.ROOT);
        Matcher m = PART.matcher(text);
        double seconds = 0;
        int pos = 0;
        while (pos < text.length() && m.find(pos) && m.start() == pos) {
            double v = Double.parseDouble(m.group(1));
            seconds += switch (m.group(2)) {
                case "m" -> v * 60;
                case "s" -> v;
                default -> v * 3600;
            };
            pos = m.end();
            if (m.group(2).isEmpty()) break;
        }
        return pos == text.length() && pos > 0 ? seconds : -1;
    }

    /** Реальные тики → «1h30m», «45s». */
    public static String formatTicks(long ticks) {
        long seconds = Math.round(ticks / 20.0);
        long h = seconds / 3600, m = seconds % 3600 / 60, sec = seconds % 60;
        StringBuilder b = new StringBuilder();
        if (h > 0) b.append(h).append("h");
        if (m > 0) b.append(m).append("m");
        if (sec > 0 || b.isEmpty()) b.append(sec).append("s");
        return b.toString();
    }
}
