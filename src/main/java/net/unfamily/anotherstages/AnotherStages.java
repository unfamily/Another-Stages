package net.unfamily.anotherstages;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.anotherstages.block.StageBlockBootstrap;
import net.unfamily.anotherstages.command.StageMirrorCommand;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.item.StageItemBootstrap;
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
        // Install order builds reload chain: Library <- items <- recipes <- blocks
        StageItemBootstrap.install();
        StageRecipeBootstrap.install();
        StageBlockBootstrap.install();
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        if (AsConfig.loggingEnabled()) {
            LOGGER.info("Another Stages initialized (logging enabled)");
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        StageMirrorCommand.register(event.getDispatcher());
    }
}
