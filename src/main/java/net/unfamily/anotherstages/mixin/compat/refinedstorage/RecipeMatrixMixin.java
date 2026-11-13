package net.unfamily.anotherstages.mixin.compat.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.refinedstorage.RsCraftSubjects;
import net.unfamily.anotherstages.compat.refinedstorage.RsMatrixOwners;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

/**
 * RS Crafting Grid / Pattern Grid matrices: clear result when the matched recipe is stage-locked
 * for the interacting player (ThreadLocal) or the node placer UUID.
 */
@Mixin(value = RecipeMatrix.class, remap = false)
public abstract class RecipeMatrixMixin<T extends Recipe<I>, I extends RecipeInput> {
    @Shadow(remap = false)
    private RecipeHolder<T> currentRecipe;

    @Shadow(remap = false)
    public abstract ResultContainer getResult();

    @WrapMethod(method = "updateResult", remap = false)
    private void anotherStages$gateRsMatrix(Level level, Operation<Void> original) {
        original.call(level);
        if (!AsConfig.mixinRefinedStorage() || currentRecipe == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        RecipeMatrix<T, I> self = (RecipeMatrix<T, I>) (Object) this;
        UUID owner = RsCraftSubjects.currentUuid();
        if (owner == null) {
            owner = RsMatrixOwners.ownerOf(self);
        }
        if (RecipeStageGate.isBlockedForOwner(currentRecipe.id(), owner, level)) {
            currentRecipe = null;
            ResultContainer result = getResult();
            if (result != null) {
                result.setItem(0, ItemStack.EMPTY);
                result.setRecipeUsed(null);
            }
        }
    }
}
