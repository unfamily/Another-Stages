package net.unfamily.anotherstages.compat.ae2;

import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Resolve AE2 network / action subject UUID for stage gates.
 * Uses AE2's {@link IGridNode#getOwningPlayerProfileId()} (already UUID) — no format conversion.
 */
public final class Ae2NetworkOwners {
    private Ae2NetworkOwners() {}

    @Nullable
    public static UUID fromActionSource(@Nullable IActionSource src, @Nullable IGridNode fallbackNode) {
        if (src != null) {
            Player player = src.player().orElse(null);
            if (player != null) {
                return player.getUUID();
            }
        }
        return fromNode(fallbackNode);
    }

    @Nullable
    public static UUID fromNode(@Nullable IGridNode node) {
        if (node == null) {
            return null;
        }
        return node.getOwningPlayerProfileId();
    }

    @Nullable
    public static UUID fromPlayer(@Nullable Player player) {
        return player == null ? null : player.getUUID();
    }
}
