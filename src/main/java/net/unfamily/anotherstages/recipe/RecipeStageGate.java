package net.unfamily.anotherstages.recipe;

import net.minecraft.resources.ResourceLocation;
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
 * <b>no owner</b>. Missing owner must never skip a stage lock — Library
 * {@link RecipeBlockBridge} delegates to providers when {@code ownerUuid == null}
 * (world stages apply; player-scoped conditions resolve as missing when no online player).
 */
public final class RecipeStageGate {
    private RecipeStageGate() {}

    public static boolean isBlockedForPlayer(@Nullable ResourceLocation recipeId, @Nullable Player player, @Nullable Level level) {
        if (recipeId == null || player == null) {
            return false;
        }
        return RecipeBlockBridge.isRecipeBlocked(recipeId, player.getUUID(), level);
    }

    public static boolean isBlockedForPlayer(@Nullable ResourceKey<Recipe<?>> recipeKey, @Nullable Player player, @Nullable Level level) {
        if (recipeKey == null) {
            return false;
        }
        return isBlockedForPlayer(recipeKey.location(), player, level);
    }

    /**
     * Process/machine gate: prevent the recipe from starting when stage-locked.
     * No {@link MachineOwners} entry is normal — bridge still consults providers.
     */
    public static boolean isBlockedForMachine(
            @Nullable ResourceLocation recipeId, @Nullable BlockEntity be, @Nullable Level level) {
        if (recipeId == null) {
            return false;
        }
        UUID owner = MachineOwners.get(be);
        return RecipeBlockBridge.isRecipeBlocked(recipeId, owner, level);
    }

    public static boolean isBlockedForMachine(
            @Nullable ResourceKey<Recipe<?>> recipeKey, @Nullable BlockEntity be, @Nullable Level level) {
        if (recipeKey == null) {
            return false;
        }
        return isBlockedForMachine(recipeKey.location(), be, level);
    }
}
