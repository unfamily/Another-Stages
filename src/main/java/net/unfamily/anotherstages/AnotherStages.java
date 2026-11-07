package net.unfamily.anotherstages;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.unfamily.anotherstages.block.StageBlockBootstrap;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.item.StageItemBootstrap;
import net.unfamily.anotherstages.network.AsNetworking;
import net.unfamily.anotherstages.owner.AsAttachmentTypes;
import net.unfamily.anotherstages.recipe.StageRecipeBootstrap;
import org.slf4j.Logger;

@Mod(AnotherStages.MOD_ID)
public class AnotherStages {
    public static final String MOD_ID = "another_stages";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AnotherStages(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, AsConfig.SPEC);
        AsAttachmentTypes.register(modEventBus);
        modEventBus.addListener(AsNetworking::register);
        AsNetworking.installHooks();
        // Install order builds reload chain: Library <- items <- recipes <- blocks <- kube soft-reload (outer)
        StageItemBootstrap.install();
        StageRecipeBootstrap.install();
        StageBlockBootstrap.install();
        net.unfamily.anotherstages.kubejs.KubejsStageSoftReload.install();
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            net.unfamily.anotherstages.client.StageItemClientSetup.register(modEventBus);
        }
        if (AsConfig.loggingEnabled()) {
            LOGGER.info("Another Stages initialized (logging enabled)");
        }
    }
}
