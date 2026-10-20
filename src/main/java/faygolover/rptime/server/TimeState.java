package faygolover.rptime.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Что ведущий поменял командами — хранится в мире ({@code data/rptime.dat}), переживает перезапуск.
 * Длина не задана (-1) — берётся из конфига.
 */
public final class TimeState extends SavedData {
    private static final String NAME = "rptime";

    long dayTicks = -1;
    long nightTicks = -1;
    double speed = 1;
    boolean paused;
    long sleepTicks = -1;
    double wheatDays = -1;

    public long dayTicks() {
        return dayTicks;
    }

    public long nightTicks() {
        return nightTicks;
    }

    public double speed() {
        return speed;
    }

    public boolean paused() {
        return paused;
    }

    /** Длина ночи во сне, реальные тики; -1 — из конфига. */
    public long sleepTicks() {
        return sleepTicks;
    }

    public void setSleepTicks(long value) {
        sleepTicks = value;
        setDirty();
    }

    /** За сколько игровых суток растёт пшеница; -1 — из конфига. */
    public double wheatDays() {
        return wheatDays;
    }

    public void setWheatDays(double value) {
        wheatDays = value;
        setDirty();
    }

    /** Длины в реальных тиках; -1 — брать из конфига. */
    public void setLengths(long day, long night) {
        dayTicks = day;
        nightTicks = night;
        setDirty();
    }

    public void setSpeed(double value) {
        speed = value;
        setDirty();
    }

    public void setPaused(boolean value) {
        paused = value;
        setDirty();
    }

    static TimeState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TimeState::load, TimeState::new, NAME);
    }

    private static TimeState load(CompoundTag tag) {
        TimeState s = new TimeState();
        s.dayTicks = tag.contains("DayTicks") ? tag.getLong("DayTicks") : -1;
        s.nightTicks = tag.contains("NightTicks") ? tag.getLong("NightTicks") : -1;
        s.speed = tag.contains("Speed") ? tag.getDouble("Speed") : 1;
        s.paused = tag.getBoolean("Paused");
        s.sleepTicks = tag.contains("SleepTicks") ? tag.getLong("SleepTicks") : -1;
        s.wheatDays = tag.contains("WheatDays") ? tag.getDouble("WheatDays") : -1;
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (dayTicks > 0) tag.putLong("DayTicks", dayTicks);
        if (nightTicks > 0) tag.putLong("NightTicks", nightTicks);
        tag.putDouble("Speed", speed);
        tag.putBoolean("Paused", paused);
        if (sleepTicks > 0) tag.putLong("SleepTicks", sleepTicks);
        if (wheatDays > 0) tag.putDouble("WheatDays", wheatDays);
        return tag;
    }
}
