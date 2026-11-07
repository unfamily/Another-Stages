package net.unfamily.anotherstages;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(AnotherStages.MOD_ID)
public class AnotherStages {
    public static final String MOD_ID = "another_stages";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AnotherStages(IEventBus modEventBus, ModContainer modContainer) {
    }
}
