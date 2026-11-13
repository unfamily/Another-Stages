package net.unfamily.anotherstages.owner;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.compat.mekanism.MekMachineOwnerSkip;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Claims {@link AsAttachmentTypes#MACHINE_OWNER} when a player places or first uses a block entity.
 * <p>
 * Place alone is not enough for pre-existing / structure machines: player-stage recipe unlocks
 * need an owner UUID. Claim-on-open / claim-on-right-click fills that gap without overwriting
 * an existing owner. Mod menus often keep the BE behind {@code getBlockEntity()} rather than
 * {@link Slot#container}.
 * <p>
 * Mek tiles with native security ({@code hasSecurity()}) are skipped — stage gates use
 * {@code getOwnerUUID()}. Evaporation casings without security still claim for the AS fallback.
 */
@EventBusSubscriber(modid = AnotherStages.MOD_ID)
public final class MachineOwnerEvents {
    private static final String[] BE_GETTERS = {
        "getBlockEntity", "getTileEntity", "getMachine", "getTile", "getBe"
    };
    private static final String[] BE_FIELDS = {
        "blockEntity", "tileEntity", "machine", "te", "be", "tile"
    };

    private MachineOwnerEvents() {}

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        claimIfNeeded(be, player.getUUID());
    }

    /** First right-click on a BE claims owner when missing (furnace family, mod machines, …). */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer) || event.getLevel().isClientSide()) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        claimIfNeeded(be, player.getUUID());
    }

    /**
     * Backup when a menu opens without a prior place/right-click claim (remote open, etc.).
     */
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer) || player.level().isClientSide()) {
            return;
        }
        BlockEntity be = findBlockEntity(event.getContainer());
        claimIfNeeded(be, player.getUUID());
    }

    private static void claimIfNeeded(@Nullable BlockEntity be, @Nullable UUID playerUuid) {
        if (be == null || playerUuid == null) {
            return;
        }
        if (skipsAsOwnerClaim(be)) {
            return;
        }
        MachineOwners.claimIfAbsent(be, playerUuid);
    }

    /** Soft-gated: do not load Mek classes when the mod is absent. */
    private static boolean skipsAsOwnerClaim(BlockEntity be) {
        if (!ModList.get().isLoaded("mekanism")) {
            return false;
        }
        return MekMachineOwnerSkip.shouldSkipAsClaim(be);
    }

    @Nullable
    static BlockEntity findBlockEntity(@Nullable AbstractContainerMenu menu) {
        if (menu == null) {
            return null;
        }
        for (Slot slot : menu.slots) {
            if (slot != null && slot.container instanceof BlockEntity be) {
                return be;
            }
        }
        BlockEntity fromApi = findBlockEntityReflect(menu);
        if (fromApi != null) {
            return fromApi;
        }
        return null;
    }

    @Nullable
    private static BlockEntity findBlockEntityReflect(Object menu) {
        Class<?> c = menu.getClass();
        while (c != null && c != Object.class) {
            for (String name : BE_GETTERS) {
                try {
                    Method m = c.getDeclaredMethod(name);
                    m.setAccessible(true);
                    Object value = m.invoke(menu);
                    if (value instanceof BlockEntity be) {
                        return be;
                    }
                } catch (ReflectiveOperationException ignored) {
                    // next
                }
            }
            for (String name : BE_FIELDS) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    Object value = f.get(menu);
                    if (value instanceof BlockEntity be) {
                        return be;
                    }
                } catch (ReflectiveOperationException ignored) {
                    // next
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
