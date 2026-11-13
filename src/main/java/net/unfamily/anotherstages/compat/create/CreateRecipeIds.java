package net.unfamily.anotherstages.compat.create;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Resolve a Create {@link Recipe} instance back to its registry id for stage matching.
 */
public final class CreateRecipeIds {
    private CreateRecipeIds() {}

    @Nullable
    public static ResourceLocation idOf(@Nullable Recipe<?> recipe, @Nullable Level level) {
        if (recipe == null || level == null || level.isClientSide()) {
            return null;
        }
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (holder.value() == recipe) {
                return holder.id();
            }
        }
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (holder.value().equals(recipe)) {
                return holder.id();
            }
        }
        return null;
    }
}
