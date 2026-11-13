package net.unfamily.anotherstages.compat.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingPlan;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.network.Ae2CraftBlockedS2CPayload;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Shared AE2 stage checks for encode / submit / execute path coverage (1.21.1).
 */
public final class Ae2CraftGate {
    private Ae2CraftGate() {}

    public static boolean isRecipeBlocked(@Nullable ResourceLocation recipeId, @Nullable UUID owner, @Nullable Level level) {
        if (!AsConfig.mixinAe2() || recipeId == null) {
            return false;
        }
        return RecipeStageGate.isBlockedForOwner(recipeId, owner, level);
    }

    public static boolean isPlanBlocked(@Nullable ICraftingPlan plan, @Nullable UUID owner, @Nullable Level level) {
        if (!AsConfig.mixinAe2() || plan == null) {
            return false;
        }
        Map<IPatternDetails, Long> times = plan.patternTimes();
        if (times == null || times.isEmpty()) {
            return false;
        }
        for (IPatternDetails pattern : times.keySet()) {
            ResourceLocation id = Ae2PatternRecipes.vanillaRecipeId(pattern);
            if (id != null && RecipeStageGate.isBlockedForOwner(id, owner, level)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPatternBlocked(
            @Nullable IPatternDetails pattern, @Nullable UUID owner, @Nullable Level level) {
        return isRecipeBlocked(Ae2PatternRecipes.vanillaRecipeId(pattern), owner, level);
    }

    /**
     * @param actionBar when false (submit → CraftErrorScreen), only S2C flag; when true (encode), action-bar tip
     */
    public static void notifyBlocked(@Nullable Player player, @Nullable ResourceLocation recipeId, boolean actionBar) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        PacketDistributor.sendToPlayer(
                serverPlayer,
                new Ae2CraftBlockedS2CPayload(recipeId == null ? "" : recipeId.toString()));
        if (actionBar) {
            Component msg = recipeId == null
                    ? Component.translatable("another_stages.ae2.craft_blocked")
                    : Component.translatable("another_stages.ae2.craft_blocked_recipe", recipeId.toString());
            serverPlayer.sendSystemMessage(msg, true);
        }
    }

    public static void notifyBlocked(@Nullable Player player, @Nullable ResourceLocation recipeId) {
        notifyBlocked(player, recipeId, true);
    }
}
