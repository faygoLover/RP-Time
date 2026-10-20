package faygolover.rptime.server;

import faygolover.rptime.TimeConfig;
import faygolover.rptime.core.DayCycle;
import faygolover.rptime.mixin.CropBlockAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;

/**
 * Растения растут по игровому времени (план §7): пшеница в среднем за {@code wheat_days} игровых суток, остальные — в той
 * же пропорции к ванилле. Множитель к ванильному шансу роста:
 * {@code p = (сколько времени суток прошло за тик) × (ванильная пшеница в тиках) / (наша пшеница в тиках времени суток)}.
 * Пауза — 0, днём и ночью — одинаково на игровой час, во сне и при {@code /time speed} — быстрее.
 * <p>
 * Культуры — через событие Forge (стебли, какао, тростник, кактус, ягоды, адский нарост, модовые — все, кто его шлют):
 * {@code p < 1} — часть попыток отменяется; {@code p > 1} — часть попыток проходит, даже если ванильный бросок не прошёл
 * (предел — каждая попытка). Саженцы — миксин в их случайный тик ({@code SaplingBlockMixin}). Костная мука — как обычно.
 */
public final class GrowthService {
    private GrowthService() {}

    /** Шанс роста культуры за попытку, если ванильный не узнать (какао — 1/5). */
    private static final double FALLBACK_CHANCE = 0.2;

    /** Множитель к ванильному росту сейчас; 1 — как в ванилле. */
    public static double multiplier() {
        if (!TimeConfig.GROWTH_ENABLED.get() || TimeService.server() == null) return 1;
        double vanillaTicks = TimeConfig.VANILLA_WHEAT_MINUTES.get() * 60 * 20;
        double ourTicks = TimeService.wheatDays() * DayCycle.DAY;
        return TimeService.lastDelta() * vanillaTicks / ourTicks;
    }

    public static void onCropGrow(BlockEvent.CropGrowEvent.Pre e) {
        if (!(e.getLevel() instanceof ServerLevel level) || e.getResult() != Event.Result.DEFAULT) return;
        double p = multiplier();
        if (p == 1) return;
        RandomSource r = level.getRandom();
        if (p < 1) {
            if (r.nextDouble() >= p) e.setResult(Event.Result.DENY);
            return;
        }
        // Нужно p × c, ванилла даёт c: сверху добавляем a — тогда a + (1 − a) × c = p × c.
        double c = vanillaChance(e.getState(), level, e.getPos());
        if (c >= 1) return;
        double a = Math.min(1, (p - 1) * c / (1 - c));
        if (r.nextDouble() < a) e.setResult(Event.Result.ALLOW);
    }

    /** Ванильный шанс роста культуры за попытку: 1 / (25 / скорость + 1), как в {@code CropBlock.randomTick}. */
    private static double vanillaChance(BlockState state, ServerLevel level, BlockPos pos) {
        if (!(state.getBlock() instanceof CropBlock) && !(state.getBlock() instanceof StemBlock)) return FALLBACK_CHANCE;
        float speed = CropBlockAccess.rptime$growthSpeed(state.getBlock(), level, pos);
        return speed <= 0 ? FALLBACK_CHANCE : 1.0 / ((int) (25.0F / speed) + 1);
    }

    /** Саженец: сколько раз продвинуть рост вместо ванильного одного. */
    public static void saplingTick(SaplingBlock block, ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        double p = multiplier();
        int times = (int) Math.floor(p);
        if (random.nextDouble() < p - times) times++;
        for (int i = 0; i < times; i++) {
            BlockState now = i == 0 ? state : level.getBlockState(pos);
            if (now.getBlock() != block) return;
            block.advanceTree(level, pos, now, random);
        }
    }
}
