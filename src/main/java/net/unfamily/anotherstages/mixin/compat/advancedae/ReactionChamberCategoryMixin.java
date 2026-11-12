package net.unfamily.anotherstages.mixin.compat.advancedae;

import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.pedroksl.advanced_ae.recipes.ReactionChamberRecipe;
import net.pedroksl.advanced_ae.xmod.jei.ReactionChamberCategory;
import net.unfamily.anotherstages.compat.advancedae.AdvAeReactionIds;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * AdvAE registers bare {@link ReactionChamberRecipe} values in JEI (strips holders).
 * Restore registry names so AS lock overlay/hide resolve by recipe id.
 */
@Mixin(value = ReactionChamberCategory.class, remap = false)
public abstract class ReactionChamberCategoryMixin implements IRecipeCategory<ReactionChamberRecipe> {
    @Override
    @Nullable
    public ResourceLocation getRegistryName(ReactionChamberRecipe recipe) {
        return AdvAeReactionIds.idOf(recipe);
    }
}
