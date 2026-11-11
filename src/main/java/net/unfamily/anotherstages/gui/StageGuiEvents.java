package net.unfamily.anotherstages.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;

import java.util.List;

/**
 * Closes stage-gated menus on open and applies slot capacity / blocked-item ejects.
 */
@EventBusSubscriber(modid = AnotherStages.MOD_ID)
public final class StageGuiEvents {
    private StageGuiEvents() {}

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!StageGuiHandler.hasLoadedRules()) {
            return;
        }
        AbstractContainerMenu menu = event.getContainer();
        List<StageGuiRule> active = StageGuiHandler.findActiveRules(player, menu);
        if (active.isEmpty()) {
            return;
        }

        if (StageGuiHandler.shouldBlockOpen(active)) {
            StageGuiRule msgRule = StageGuiHandler.firstBlockOpenMessage(active);
            player.closeContainer();
            if (msgRule != null) {
                player.sendSystemMessage(StageGuiHandler.resolveMessage(msgRule), true);
            }
            if (AsConfig.loggingEnabled()) {
                AnotherStages.LOGGER.info(
                        "Closed menu {} for {} (stage_gui block_open)",
                        menu.getClass().getName(),
                        player.getGameProfile().getName());
            }
            return;
        }

        StageGuiHandler.ejectBlockedItems(player, menu, active);
        StageGuiHandler.enforceMaxSlots(player, menu, active);
    }
}
