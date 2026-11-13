package net.unfamily.anotherstages.compat.mekanism;

import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Mek tiles with {@code AttributeSecurity} already store {@code getOwnerUUID()} —
 * skip AS {@code MachineOwners} claim to avoid duplicate attachment data.
 * Evaporation casings lack security and still need the AS fallback.
 */
public final class MekMachineOwnerSkip {
    private MekMachineOwnerSkip() {}

    public static boolean shouldSkipAsClaim(@Nullable BlockEntity be) {
        return be instanceof TileEntityMekanism te && te.hasSecurity();
    }
}
