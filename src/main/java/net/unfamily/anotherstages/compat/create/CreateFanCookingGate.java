package net.unfamily.anotherstages.compat.create;

import com.simibubi.create.AllRecipeTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.recipe.RecipeStageGate;

import java.util.UUID;

/**
 * Resolves the same cooking recipe Create's fan BLASTING/SMOKING types would use, then stage-gates it.
 * Subject UUID from {@link CreateFanSubject} (ItemEntity thrower) when present.
 */
public final class CreateFanCookingGate {
    private CreateFanCookingGate() {}

    /** Blasting fan: SMELTING first, then BLASTING (Create order). */
    public static boolean isBlastingBlocked(ItemStack stack, Level level) {
        if (level == null || stack == null || stack.isEmpty()) {
            return false;
        }
        UUID owner = CreateFanSubject.current();
        var smelting = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
                .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
        if (smelting.isPresent()) {
            return RecipeStageGate.isBlockedForOwner(smelting.get().id(), owner, level);
        }
        var blasting = level.getRecipeManager()
                .getRecipeFor(RecipeType.BLASTING, new SingleRecipeInput(stack), level)
                .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
        return blasting
                .map(holder -> RecipeStageGate.isBlockedForOwner(holder.id(), owner, level))
                .orElse(false);
    }

    public static boolean isSmokingBlocked(ItemStack stack, Level level) {
        if (level == null || stack == null || stack.isEmpty()) {
            return false;
        }
        UUID owner = CreateFanSubject.current();
        var smoking = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(stack), level)
                .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
        return smoking
                .map(holder -> RecipeStageGate.isBlockedForOwner(holder.id(), owner, level))
                .orElse(false);
    }
}
