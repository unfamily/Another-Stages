package net.unfamily.anotherstages.compat.createdragonsplus;

import com.simibubi.create.AllRecipeTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.create.CreateFanSubject;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import plus.dragons.createdragonsplus.common.registry.CDPRecipes;

import java.util.Optional;
import java.util.UUID;

/**
 * Stage-gates Create Dragons Plus fan ending / freezing datapack recipes
 * (same subject as Create fans: {@link CreateFanSubject}).
 */
public final class CreateDragonsPlusFanGate {
    private CreateDragonsPlusFanGate() {}

    public static boolean isEndingBlocked(ItemStack stack, Level level) {
        return isTypeBlocked(CDPRecipes.ENDING.getType(), stack, level);
    }

    public static boolean isFreezingBlocked(ItemStack stack, Level level) {
        return isTypeBlocked(CDPRecipes.FREEZING.getType(), stack, level);
    }

    private static <I extends RecipeInput, R extends Recipe<I>> boolean isTypeBlocked(
            RecipeType<R> type, ItemStack stack, Level level) {
        if (level == null || stack == null || stack.isEmpty()) {
            return false;
        }
        UUID owner = CreateFanSubject.current();
        @SuppressWarnings("unchecked")
        Optional<RecipeHolder<R>> found =
                level.getRecipeManager().getRecipeFor(type, (I) new SingleRecipeInput(stack), level);
        return found
                .filter(AllRecipeTypes.CAN_BE_AUTOMATED)
                .map(holder -> RecipeStageGate.isBlockedForOwner(holder.id(), owner, level))
                .orElse(false);
    }
}
