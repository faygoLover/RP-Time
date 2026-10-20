package faygolover.rptime.mixin;

import faygolover.rptime.server.GrowthService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Саженцы растут по игровому времени ({@link GrowthService}). Только случайный тик: костная мука зовёт рост в обход
 * и работает как обычно.
 */
@Mixin(SaplingBlock.class)
public abstract class SaplingBlockMixin {
    @Redirect(method = "randomTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/SaplingBlock;advanceTree(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)V"))
    private void rptime$advanceTree(SaplingBlock self, ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        GrowthService.saplingTick(self, level, pos, state, random);
    }
}
