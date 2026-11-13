package net.unfamily.anotherstages.mixin.compat.sliceanddice;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.possible_triangle.sliceanddice.block.slicer.SlicerBlockEntity;
import com.possible_triangle.sliceanddice.recipe.CuttingProcessingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.compat.create.CreateRecipeIds;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Create Slice & Dice Slicer belt/world cutting lookup.
 * Basin path is already gated via {@code BasinOperatingBlockEntityMixin}.
 */
@Mixin(value = SlicerBlockEntity.class, remap = false)
public abstract class SlicerBlockEntityMixin {
    @WrapMethod(
            method = "recipeFor(Lnet/minecraft/world/item/ItemStack;)Lcom/possible_triangle/sliceanddice/recipe/CuttingProcessingRecipe;",
            remap = false)
    private CuttingProcessingRecipe anotherStages$gateRecipeFor(
            ItemStack stack, Operation<CuttingProcessingRecipe> original) {
        CuttingProcessingRecipe recipe = original.call(stack);
        if (!AsConfig.mixinSliceAndDice() || recipe == null) {
            return recipe;
        }
        SlicerBlockEntity self = (SlicerBlockEntity) (Object) this;
        BlockEntity be = (BlockEntity) (Object) self;
        Level level = be.getLevel();
        ResourceLocation id = CreateRecipeIds.idOf(recipe, level);
        if (id != null && RecipeStageGate.isBlockedForOwner(id, MachineOwners.get(be), level)) {
            return null;
        }
        return recipe;
    }
}
