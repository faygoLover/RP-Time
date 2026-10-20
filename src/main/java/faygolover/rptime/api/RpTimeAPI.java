package faygolover.rptime.api;

import faygolover.rptime.core.Clock;
import faygolover.rptime.core.DayCycle;
import faygolover.rptime.server.SleepService;
import faygolover.rptime.server.TimeService;

/**
 * Для других модов (сервер). Часы: {@link Clock} — 0 тиков = 6:00, номер дня. Сон: моды не дают уснуть ванильным
 * {@code PlayerSleepInBedEvent} и будят {@code stopSleepInBed} — RP Time видит это сам; кого как считать во сне —
 * событие RP Core {@code RpSleepCountEvent}.
 */
public final class RpTimeAPI {
    private RpTimeAPI() {}

    /** Сутки (день + ночь) в реальных секундах — по заданным длинам, без временного {@code /time speed}. */
    public static double dayLengthSeconds() {
        DayCycle c = TimeService.cycle();
        return (c.dayTicks() + c.nightTicks()) / 20.0;
    }

    /** Идёт ли сейчас ускоренная ночь (спят все). */
    public static boolean nightAccelerated() {
        return SleepService.accelerating();
    }

    /** Стоит ли время (пауза, {@code /rptime stop}). */
    public static boolean paused() {
        return TimeService.paused();
    }

    /** «14:05» по времени суток. */
    public static String clock(long dayTime) {
        return Clock.format(dayTime);
    }

    /** Номер дня с первого. */
    public static long day(long dayTime) {
        return Clock.day(dayTime);
    }
}
