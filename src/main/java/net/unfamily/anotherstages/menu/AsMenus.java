package net.unfamily.anotherstages.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.anotherstages.AnotherStages;

/** Menu types for Another Stages. */
public final class AsMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AnotherStages.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<StageGeneratorMenu>> STAGE_GENERATOR =
            MENUS.register(
                    "stage_generator",
                    () -> IMenuTypeExtension.create(StageGeneratorMenu::createClient));

    private AsMenus() {}

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
