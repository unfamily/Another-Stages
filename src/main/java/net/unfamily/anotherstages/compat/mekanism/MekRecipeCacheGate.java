package net.unfamily.anotherstages.compat.mekanism;

import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.recipe.lookup.IRecipeLookupHandler;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Stage checks against a Mek {@link CachedRecipe}.
 * <p>
 * Mek keeps a live cache while inputs stay valid and skips {@code getRecipe}; process still
 * consults {@code canHolderFunction} every tick — bind the current handler via
 * {@link #pushHandler}/{@link #popHandler} around {@code updateAndProcess}.
 * <p>
 * Owner: native {@link TileEntityMekanism#getOwnerUUID()} first. {@link MachineOwners} only when
 * Mek has no security owner (e.g. Thermal Evaporation casings) — same no-owner fallback as furnaces.
 * Never use {@code MultiblockData.inventoryID} (inventory-frequency UUID).
 */
public final class MekRecipeCacheGate {
    private static final ThreadLocal<IRecipeLookupHandler<?>> CURRENT_HANDLER = new ThreadLocal<>();

    private MekRecipeCacheGate() {}

    public static void pushHandler(@Nullable IRecipeLookupHandler<?> handler) {
        CURRENT_HANDLER.set(handler);
    }

    public static void popHandler() {
        CURRENT_HANDLER.remove();
    }

    @Nullable
    public static IRecipeLookupHandler<?> currentHandler() {
        return CURRENT_HANDLER.get();
    }

    public static boolean isCachedBlocked(
            @Nullable IRecipeLookupHandler<?> handler, @Nullable CachedRecipe<?> cached) {
        if (!AsConfig.mixinMekanism() || handler == null || cached == null) {
            return false;
        }
        Object recipe = cached.getRecipe();
        if (recipe == null) {
            return false;
        }
        Level level = handler.getLevel();
        UUID owner = resolveOwner(handler);
        ResourceLocation id = MekRecipeIds.idOf(recipe, level, handler.getRecipeType());
        if (id == null) {
            return true;
        }
        return RecipeStageGate.isBlockedForOwner(id, owner, level);
    }

    /** {@code canHolder} AND: stop process when the live cache is stage-locked. */
    public static boolean canProcessCached(@Nullable CachedRecipe<?> cached) {
        IRecipeLookupHandler<?> handler = CURRENT_HANDLER.get();
        if (handler == null || cached == null) {
            return true;
        }
        return !isCachedBlocked(handler, cached);
    }

    /**
     * Stage subject UUID. TE handlers: Mek security only. Multiblock: Mek first, then
     * {@link MachineOwners} if Mek has no owner (Evaporation), scanning structure locations.
     */
    @Nullable
    public static UUID resolveOwner(@Nullable IRecipeLookupHandler<?> handler) {
        if (handler instanceof TileEntityMekanism te) {
            return te.getOwnerUUID();
        }
        if (handler instanceof MultiblockData multi) {
            UUID fromBound = ownerFromTile(MekMultiblockSubjects.get(multi));
            if (fromBound != null) {
                return fromBound;
            }
            Level level = multi.getLevel();
            if (level == null || multi.locations == null) {
                return null;
            }
            for (BlockPos pos : multi.locations) {
                if (pos == null) {
                    continue;
                }
                UUID fromLoc = ownerFromTile(level.getBlockEntity(pos));
                if (fromLoc != null) {
                    return fromLoc;
                }
            }
            return null;
        }
        return null;
    }

    /** Mek {@code getOwnerUUID} first; {@link MachineOwners} only when Mek owner is absent. */
    @Nullable
    private static UUID ownerFromTile(@Nullable BlockEntity tile) {
        if (tile == null) {
            return null;
        }
        if (tile instanceof TileEntityMekanism te) {
            UUID mek = te.getOwnerUUID();
            if (mek != null) {
                return mek;
            }
        }
        return MachineOwners.get(tile);
    }
}
