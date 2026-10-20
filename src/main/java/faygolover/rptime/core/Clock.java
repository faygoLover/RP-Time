package faygolover.rptime.core;

/**
 * Часы по времени суток: 0 тиков = 6:00, 1000 тиков = час (как у часов PCD и SovietEra). Номер дня — с первого.
 */
public final class Clock {
    private Clock() {}

    public static int hour(long dayTime) {
        return (int) ((Math.floorMod(dayTime, DayCycle.DAY) / 1000 + 6) % 24);
    }

    public static int minute(long dayTime) {
        return (int) (Math.floorMod(dayTime, DayCycle.DAY) % 1000 * 60 / 1000);
    }

    /** «14:05». */
    public static String format(long dayTime) {
        return String.format("%d:%02d", hour(dayTime), minute(dayTime));
    }

    /** Номер дня с первого; новый день начинается в 6:00, как ванильные сутки. */
    public static long day(long dayTime) {
        return Math.floorDiv(dayTime, DayCycle.DAY) + 1;
    }

    /** Время суток (тики) по часам и минутам. */
    public static int toTicks(int hour, int minute) {
        return Math.floorMod((hour - 6) * 1000 + minute * 1000 / 60, DayCycle.DAY);
    }
}
