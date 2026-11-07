package net.unfamily.anotherstages.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.unfamily.anotherstages.AnotherStages;

/**
 * Soft-bridges to the JEI plugin without hard-loading JEI classes when JEI is absent.
 */
@EventBusSubscriber(modid = AnotherStages.MOD_ID, value = Dist.CLIENT)
public final class StageItemJeiRefresh {
    private static int tickCounter;
    private static Boolean jeiPresent;

    private StageItemJeiRefresh() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (jeiPresent == null) {
            jeiPresent = ModList.get().isLoaded("jei");
        }
        if (!jeiPresent) {
            return;
        }
        if (++tickCounter < 20) {
            return;
        }
        tickCounter = 0;
        try {
            Class.forName("net.unfamily.anotherstages.compat.jei.AnotherStagesJeiPlugin")
                    .getMethod("onClientTick")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            jeiPresent = false;
        }
    }
}
