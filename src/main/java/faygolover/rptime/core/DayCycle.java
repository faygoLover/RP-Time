package faygolover.rptime.core;

/**
 * Ход времени суток без Minecraft: сколько тиков времени суток проходит за реальные тики.
 * <p>
 * Сутки — 24000 тиков времени суток, как в ванилле. День — от {@link #dayStart} до {@link #nightStart}, ночь — остальное.
 * День проходит за {@link #dayTicks} реальных тиков, ночь — за {@link #nightTicks}; поверх — множитель {@link #speed}
 * ({@code /time speed}, 0 — пауза). На стыке дня и ночи остаток тика идёт уже по новой скорости, без скачка.
 * Пока спят все — {@link #sleepRate}: ровная скорость сна, день и ночь не различаются (пауза её тоже останавливает).
 *
 * @param dayStart   время суток начала дня, 0–23999
 * @param nightStart время суток начала ночи, 0–23999, не равно {@code dayStart}
 * @param dayTicks   длина дня в реальных тиках, больше 0
 * @param nightTicks длина ночи в реальных тиках, больше 0
 * @param speed      множитель скорости, 0 и больше
 * @param sleepRate  тиков времени суток за реальный тик, пока спят все; 0 — не спят
 */
public record DayCycle(int dayStart, int nightStart, long dayTicks, long nightTicks, double speed, double sleepRate) {
    public static final int DAY = 24000;

    /** Ванильные сутки: 20 минут, свет днём с 23500 до 12500 — как в ванилле, тик за тик. */
    public static final DayCycle VANILLA = new DayCycle(23500, 12500, 13000, 11000, 1, 0);

    public DayCycle {
        dayStart = Math.floorMod(dayStart, DAY);
        nightStart = Math.floorMod(nightStart, DAY);
        if (dayStart == nightStart) throw new IllegalArgumentException("dayStart == nightStart");
        if (dayTicks <= 0 || nightTicks <= 0) throw new IllegalArgumentException("length <= 0");
        if (!(speed >= 0)) throw new IllegalArgumentException("speed < 0");
        if (!(sleepRate >= 0)) throw new IllegalArgumentException("sleepRate < 0");
    }

    /** Тиков времени суток в дне. */
    public int dayLength() {
        return Math.floorMod(nightStart - dayStart, DAY);
    }

    public int nightLength() {
        return DAY - dayLength();
    }

    /** Тиков времени суток за реальный тик днём (без множителя). */
    public double dayRate() {
        return (double) dayLength() / dayTicks;
    }

    public double nightRate() {
        return (double) nightLength() / nightTicks;
    }

    public boolean paused() {
        return speed == 0;
    }

    public boolean sleeping() {
        return sleepRate > 0 && speed > 0;
    }

    public DayCycle withSleepRate(double rate) {
        return new DayCycle(dayStart, nightStart, dayTicks, nightTicks, speed, rate);
    }

    /** Тиков времени суток за реальный тик в это время, без множителя и сна — обычный ход. */
    public double baseRate(double time) {
        return isDay(time) ? dayRate() : nightRate();
    }

    /** Тиков времени суток за реальный тик сейчас: с множителем, паузой и сном. */
    public double rate(double time) {
        if (speed <= 0) return 0;
        return sleepRate > 0 ? sleepRate : baseRate(time) * speed;
    }

    /** День ли в это время суток (любое число, берётся по модулю суток). */
    public boolean isDay(double time) {
        return mod(time - dayStart) < dayLength() - EPS;
    }

    /**
     * Время суток через {@code realTicks} реальных тиков, если сейчас {@code time}. Время — сплошное (с номером суток),
     * дробное: дробную часть держит тот, кто зовёт.
     */
    public double advance(double time, double realTicks) {
        if (speed <= 0 || realTicks <= 0) return time;
        if (sleepRate > 0) return time + sleepRate * realTicks;
        double left = realTicks;
        // Больше одного стыка за тик бывает только при огромном множителе; тогда хватит и нескольких кругов.
        for (int i = 0; i < 8 && left > 0; i++) {
            boolean day = isDay(time);
            double rate = (day ? dayRate() : nightRate()) * speed;
            double pos = mod(time - (day ? dayStart : nightStart));
            double toEdge = (day ? dayLength() : nightLength()) - pos;
            double need = toEdge / rate;
            if (need >= left) return time + rate * left;
            time += toEdge;
            left -= need;
        }
        return time;
    }

    /** Реальных тиков на полные сутки при текущем множителе; при паузе — бесконечность. */
    public double realTicksPerDay() {
        return speed <= 0 ? Double.POSITIVE_INFINITY : (dayTicks + nightTicks) / speed;
    }

    private static final double EPS = 1e-6;

    private static double mod(double v) {
        double r = v % DAY;
        return r < 0 ? r + DAY : r;
    }
}
