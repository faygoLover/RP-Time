package faygolover.rptime;

import com.mojang.logging.LogUtils;
import faygolover.rptime.command.TimeCommands;
import faygolover.rptime.network.TimeNetwork;
import faygolover.rptime.server.TimeService;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * RP Time — день и ночь своей длины, скорость и пауза времени, ускоренная ночь во сне, растения по игровому времени,
 * часы на табличках (план — {@code docs/00_plan.md}). Источник правды —
 * ванильное время суток, поэтому {@code /time set}, кровати, часы и всё, что смотрит на время суток, работают как
 * раньше. Часть RP-семейства, RP Core не требует.
 */
@Mod(RpTime.MODID)
public class RpTime {
    public static final String MODID = "rptime";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpTime() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        modBus.addListener(TimeService::onConfigReload);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TimeConfig.SPEC);
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(TimeCommands::register);
        bus.addListener(TimeService::onServerStarted);
        bus.addListener(TimeService::onServerStopped);
        bus.addListener(TimeService::onLogin);
        bus.addListener(faygolover.rptime.server.SleepService::onServerTick);
        bus.addListener(faygolover.rptime.server.SleepService::onServerStopped);
        bus.addListener(faygolover.rptime.server.GrowthService::onCropGrow);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(faygolover.rptime.client.ClientTime::onLoggingOut));
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        TimeNetwork.register();
        faygolover.rptime.integration.CoreLink.registerSource();
    }
}
