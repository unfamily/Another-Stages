package net.unfamily.anotherstages.compat.create;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Resolves the datapack {@code create:filling} recipe id Create's Spout would apply
 * (same order as {@code FillingBySpout.fillItem}). Returns null for GenericItemFilling-only paths.
 */
public final class CreateFillingGate {
    private CreateFillingGate() {}

    @Nullable
    public static ResourceLocation fillingRecipeId(
            Level level, ItemStack stack, FluidStack availableFluid) {
        if (level == null || stack == null || stack.isEmpty() || availableFluid == null || availableFluid.isEmpty()) {
            return null;
        }
        SingleRecipeInput input = new SingleRecipeInput(stack);
        Optional<RecipeHolder<FillingRecipe>> assembly = SequencedAssemblyRecipe.getRecipe(
                level, input, AllRecipeTypes.FILLING.getType(), FillingRecipe.class);
        if (assembly.isPresent()
                && assembly.get().value().getRequiredFluid().ingredient().test(availableFluid)) {
            return assembly.get().id();
        }
        for (RecipeHolder<Recipe<SingleRecipeInput>> recipe :
                level.getRecipeManager().getRecipesFor(AllRecipeTypes.FILLING.getType(), input, level)) {
            FillingRecipe filling = (FillingRecipe) recipe.value();
            if (filling.getRequiredFluid().ingredient().test(availableFluid)) {
                return recipe.id();
            }
        }
        return null;
    }
}
