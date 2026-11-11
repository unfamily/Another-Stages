package net.unfamily.anotherstages.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.client.block.StageBlockClientEvents;
import net.unfamily.anotherstages.item.StageItemRvHelper;

@EventBusSubscriber(modid = AnotherStages.MOD_ID, value = Dist.CLIENT)
public final class StageItemClientSetup {
    private StageItemClientSetup() {}

    public static void register(IEventBus modEventBus) {
        ClientStageCache.setOnChanged(StageBlockClientEvents::markAllDirty);
        modEventBus.addListener(StageItemClientSetup::onRegisterItemDecorations);
        modEventBus.addListener(StageItemClientSetup::onRegisterAdditionalModels);
    }

    private static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            event.register(item, StageItemLockDecorator.INSTANCE);
        }
    }

    /**
     * Side-loaded hardcore / fish models for {@code ItemRendererMixin}.
     * NeoForge requires the {@code standalone} variant for RegisterAdditional (not {@code inventory}).
     */
    private static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(hardcoreStandaloneModel(StageItemRvHelper.HARDCORE_ITEM_MODEL));
        event.register(hardcoreStandaloneModel(StageItemRvHelper.FISH_HARDCORE_ITEM_MODEL));
    }

    /** {@code another_stages:stage_locked_hardcore} → models/item/stage_locked_hardcore.json */
    public static ModelResourceLocation hardcoreStandaloneModel(ResourceLocation itemModelId) {
        return ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(itemModelId.getNamespace(), "item/" + itemModelId.getPath()));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStageCache.clear();
    }
}
