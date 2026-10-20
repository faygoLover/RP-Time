package faygolover.rptime;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Конфиг RP Time: {@code config/rptime-common.toml}. Здесь — значения по умолчанию; команды {@code /time length|speed|pause}
 * меняют их для мира (хранится в мире), {@code /time length reset} возвращает к конфигу.
 */
public final class TimeConfig {
    private TimeConfig() {}

    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue DAY_MINUTES;
    public static final ForgeConfigSpec.DoubleValue NIGHT_MINUTES;
    public static final ForgeConfigSpec.IntValue DAY_START;
    public static final ForgeConfigSpec.IntValue NIGHT_START;
    public static final ForgeConfigSpec.BooleanValue SLEEP_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SLEEP_NIGHT_MINUTES;
    public static final ForgeConfigSpec.DoubleValue INACTIVE_MINUTES;
    public static final ForgeConfigSpec.BooleanValue CLEAR_WEATHER;
    public static final ForgeConfigSpec.BooleanValue GROWTH_ENABLED;
    public static final ForgeConfigSpec.DoubleValue WHEAT_DAYS;
    public static final ForgeConfigSpec.DoubleValue VANILLA_WHEAT_MINUTES;

    static {
        B.push("cycle");
        DAY_MINUTES = B.comment("Сколько реальных минут длится день. В ванилле весь день и вечер — около 11 минут.")
                .defineInRange("day_minutes", 120.0, 0.5, 10080.0);
        NIGHT_MINUTES = B.comment("Сколько реальных минут длится ночь. В ванилле — около 9 минут.")
                .defineInRange("night_minutes", 60.0, 0.5, 10080.0);
        DAY_START = B.comment("Где начинается день, в тиках времени суток (0 = 6:00, 1000 = час). 23500 = 5:30 — светает.")
                .defineInRange("day_start", 23500, 0, 23999);
        NIGHT_START = B.comment("Где начинается ночь, в тиках времени суток. 12500 = 18:30 — темнеет, можно лечь спать.")
                .defineInRange("night_start", 12500, 0, 23999);
        B.pop();

        B.push("sleep");
        SLEEP_ENABLED = B.comment("Сон ускоряет время вместо ванильного пропуска ночи. Ускоряет, только когда спят все игроки",
                        "в выживании и приключении (ГМы в творческом и наблюдатели, мёртвые и исключённые на ночь не считаются).",
                        "Кто-то встал — время сразу идёт обычным ходом. Выключено — ванильный сон.")
                .define("enabled", true);
        SLEEP_NIGHT_MINUTES = B.comment("Сколько реальных минут длится ночь, пока спят все (/time sleep). 3 — в 20 раз быстрее ночи в 1 час.")
                .defineInRange("night_minutes", 3.0, 0.1, 600.0);
        INACTIVE_MINUTES = B.comment("Игрок ничего не нажимал столько минут — неактивный. Если все спят, а ночь держат только",
                        "неактивные, ГМам раз в минуту приходит сообщение с кнопкой «Исключить на ночь».")
                .defineInRange("inactive_minutes", 5.0, 0.5, 1440.0);
        CLEAR_WEATHER = B.comment("Проспали до утра — дождь и гроза кончаются (как в ванилле).")
                .define("clear_weather", true);
        B.pop();

        B.push("growth");
        GROWTH_ENABLED = B.comment("Растения (культуры, саженцы) растут по игровому времени: в паузе не растут, во сне и при",
                        "/time speed — быстрее. Огонь, лёд, листва, трава — как в ванилле. Костная мука — как обычно.")
                .define("enabled", true);
        WHEAT_DAYS = B.comment("За сколько игровых суток в среднем вырастает пшеница (/time growth). Остальное — в той же пропорции к ванилле.")
                .defineInRange("wheat_days", 7.0, 0.05, 365.0);
        VANILLA_WHEAT_MINUTES = B.comment("Сколько в среднем растёт ванильная пшеница при обычной скорости случайных тиков, минут",
                        "(грядка с водой, ряды) — от этого считается замедление. Обычно трогать не нужно.")
                .defineInRange("vanilla_wheat_minutes", 30.0, 1.0, 600.0);
        B.pop();
        SPEC = B.build();
    }
}
