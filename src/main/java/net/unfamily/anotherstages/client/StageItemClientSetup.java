package net.unfamily.anotherstages.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.unfamily.anotherstages.AnotherStages;

@EventBusSubscriber(modid = AnotherStages.MOD_ID, value = Dist.CLIENT)
public final class StageItemClientSetup {
    private StageItemClientSetup() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(StageItemClientSetup::onRegisterItemDecorations);
    }

    private static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            event.register(item, StageItemLockDecorator.INSTANCE);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStageCache.clear();
    }
}
