package net.unfamily.anotherstages.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.iskalib.crafting.RecipeBlockBridge;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Convenience gates for vanilla/mixin call sites.
 * <p>
 * <b>Machines / process blocks</b> (furnace, campfire, crafter, mod BEs): often have
 * <b>no owner</b>. Missing owner must never skip a stage lock — evaluate AS rules directly
 * (world stages apply; player-scoped rules stay fail-closed when no online player).
 * With an owner UUID, use Library {@link RecipeBlockBridge}.
 */
public final class RecipeStageGate {
    private RecipeStageGate() {}

    public static boolean isBlockedForPlayer(@Nullable Identifier recipeId, @Nullable Player player, @Nullable Level level) {
        if (recipeId == null || player == null) {
            return false;
        }
        return RecipeBlockBridge.isRecipeBlocked(recipeId, player.getUUID(), level);
    }

    public static boolean isBlockedForPlayer(@Nullable ResourceKey<Recipe<?>> recipeKey, @Nullable Player player, @Nullable Level level) {
        if (recipeKey == null) {
            return false;
        }
        return isBlockedForPlayer(recipeKey.identifier(), player, level);
    }

    /**
     * Process/machine gate: prevent the recipe from starting when stage-locked.
     * No {@link MachineOwners} entry is normal — still block when stage rules say so.
     */
    public static boolean isBlockedForMachine(
            @Nullable Identifier recipeId, @Nullable BlockEntity be, @Nullable Level level) {
        if (recipeId == null) {
            return false;
        }
        UUID owner = MachineOwners.get(be);
        if (owner == null) {
            // Do not use RecipeBlockBridge here: it fail-closes all recipes when owner is null.
            return StageRecipeHandler.isBlocked(recipeId, null, level, null);
        }
        return RecipeBlockBridge.isRecipeBlocked(recipeId, owner, level);
    }

    public static boolean isBlockedForMachine(
            @Nullable ResourceKey<Recipe<?>> recipeKey, @Nullable BlockEntity be, @Nullable Level level) {
        if (recipeKey == null) {
            return false;
        }
        return isBlockedForMachine(recipeKey.identifier(), be, level);
    }
}
