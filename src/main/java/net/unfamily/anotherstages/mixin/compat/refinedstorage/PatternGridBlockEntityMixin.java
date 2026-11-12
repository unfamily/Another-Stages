package net.unfamily.anotherstages.mixin.compat.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridBlockEntity;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternType;
import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

/**
 * Refuse RS Pattern Grid encoding when the backing craft/smithing recipe is stage-locked
 * for the placer UUID (native owner).
 */
@Mixin(value = PatternGridBlockEntity.class, remap = false)
public abstract class PatternGridBlockEntityMixin {
    @Shadow(remap = false)
    @Final
    private RecipeMatrix<CraftingRecipe, CraftingInput> craftingRecipe;

    @Shadow(remap = false)
    @Final
    private RecipeMatrix<SmithingRecipe, SmithingRecipeInput> smithingTableRecipe;

    @Shadow(remap = false)
    private PatternType patternType;

    @Shadow(remap = false)
    private UUID placedByPlayerId;

    @Shadow(remap = false)
    public abstract PatternType getPatternType();

    @WrapMethod(method = "createPattern", remap = false)
    private void anotherStages$gateCreatePattern(Operation<Void> original) {
        if (AsConfig.mixinRefinedStorage() && isEncodingBlocked()) {
            return;
        }
        original.call();
    }

    private boolean isEncodingBlocked() {
        PatternGridBlockEntity self = (PatternGridBlockEntity) (Object) this;
        PatternType type = getPatternType();
        RecipeHolder<?> holder = null;
        if (type == PatternType.CRAFTING && craftingRecipe != null) {
            holder = craftingRecipe.getResult().getRecipeUsed();
        } else if (type == PatternType.SMITHING_TABLE && smithingTableRecipe != null) {
            holder = smithingTableRecipe.getResult().getRecipeUsed();
        }
        // Stonecutter selection is gated via RecipeMatrix-less path; soft-skip here (matrix update still applies world stages).
        if (holder == null) {
            return false;
        }
        return RecipeStageGate.isBlockedForOwner(holder.id(), placedByPlayerId, self.getLevel());
    }
}
