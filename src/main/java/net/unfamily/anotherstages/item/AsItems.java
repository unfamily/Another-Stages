package net.unfamily.anotherstages.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.anotherstages.AnotherStages;

/**
 * Internal presentation items. {@link #STAGE_LOCKED_HARDCORE} is always hidden from recipe viewers
 * and used as the generic hardcore / Jade stand-in for staged items.
 */
public final class AsItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AnotherStages.MOD_ID);

    public static final DeferredItem<Item> STAGE_LOCKED_HARDCORE = ITEMS.registerSimpleItem("stage_locked_hardcore");

    private AsItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    public static boolean isHardcorePlaceholder(Item item) {
        return item != null && item == STAGE_LOCKED_HARDCORE.get();
    }
}
