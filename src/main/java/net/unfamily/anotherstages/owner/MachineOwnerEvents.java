package net.unfamily.anotherstages.owner;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.unfamily.anotherstages.AnotherStages;

/**
 * Claims {@link AsAttachmentTypes#MACHINE_OWNER} when a player places a block entity.
 */
@EventBusSubscriber(modid = AnotherStages.MOD_ID)
public final class MachineOwnerEvents {
    private MachineOwnerEvents() {}

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        MachineOwners.claimIfAbsent(be, player.getUUID());
    }
}
