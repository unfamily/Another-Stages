package net.unfamily.anotherstages.compat.mekanism;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Maps Mek {@code MultiblockData} instances to the forming tile from their ctor.
 * {@code inventoryID} is an inventory-frequency UUID, not the security owner.
 */
public final class MekMultiblockSubjects {
    private static final Map<Object, BlockEntity> MULTI_TO_TILE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private MekMultiblockSubjects() {}

    public static void bind(@Nullable Object multiblock, @Nullable BlockEntity tile) {
        if (multiblock == null) {
            return;
        }
        if (tile == null) {
            MULTI_TO_TILE.remove(multiblock);
            return;
        }
        MULTI_TO_TILE.put(multiblock, tile);
    }

    @Nullable
    public static BlockEntity get(@Nullable Object multiblock) {
        return multiblock == null ? null : MULTI_TO_TILE.get(multiblock);
    }
}
