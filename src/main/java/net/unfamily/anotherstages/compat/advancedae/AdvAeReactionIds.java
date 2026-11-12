package net.unfamily.anotherstages.compat.advancedae;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.pedroksl.advanced_ae.recipes.ReactionChamberRecipe;
import net.pedroksl.advanced_ae.recipes.ReactionChamberRecipes;
import org.jetbrains.annotations.Nullable;

/**
 * Reverse-lookup AdvAE Reaction Chamber recipe ids for JEI entries that only carry the bare recipe value.
 */
public final class AdvAeReactionIds {
    public static final String RECIPE_CLASS = "net.pedroksl.advanced_ae.recipes.ReactionChamberRecipe";

    private AdvAeReactionIds() {}

    public static boolean isReactionRecipe(@Nullable Object recipe) {
        return recipe != null && RECIPE_CLASS.equals(recipe.getClass().getName());
    }

    @Nullable
    public static ResourceLocation idOf(@Nullable Object recipe) {
        if (!(recipe instanceof ReactionChamberRecipe reaction)) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc != null ? mc.level : null;
        if (level == null) {
            return null;
        }
        for (RecipeHolder<ReactionChamberRecipe> holder : ReactionChamberRecipes.getRecipes(level)) {
            if (holder.value() == reaction) {
                return holder.id();
            }
        }
        return null;
    }
}
