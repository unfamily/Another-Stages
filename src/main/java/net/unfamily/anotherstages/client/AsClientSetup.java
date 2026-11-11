package net.unfamily.anotherstages.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.unfamily.anotherstages.client.gui.stagegen.StageGeneratorScreen;
import net.unfamily.anotherstages.menu.AsMenus;

/** Client menu screens; registered explicitly on the mod bus (no empty EventBusSubscriber). */
public final class AsClientSetup {
    private AsClientSetup() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(AsClientSetup::onRegisterMenuScreens);
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(AsMenus.STAGE_GENERATOR.get(), StageGeneratorScreen::new);
    }
}
