package net.unfamily.anotherstages.owner;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Helpers for {@link AsAttachmentTypes#MACHINE_OWNER} on block entities.
 */
public final class MachineOwners {
    private MachineOwners() {}

    @Nullable
    public static UUID get(@Nullable BlockEntity be) {
        if (be == null) {
            return null;
        }
        String raw = be.getData(AsAttachmentTypes.MACHINE_OWNER.get());
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static void set(@Nullable BlockEntity be, @Nullable UUID owner) {
        if (be == null) {
            return;
        }
        be.setData(AsAttachmentTypes.MACHINE_OWNER.get(), owner == null ? "" : owner.toString());
        be.setChanged();
    }

    /** Sets owner only when missing. */
    public static void claimIfAbsent(@Nullable BlockEntity be, @Nullable UUID owner) {
        if (be == null || owner == null || get(be) != null) {
            return;
        }
        set(be, owner);
    }
}
