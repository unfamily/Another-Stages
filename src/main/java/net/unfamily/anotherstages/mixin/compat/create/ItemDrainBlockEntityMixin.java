package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.content.fluids.transfer.EmptyingRecipe;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

/**
 * Create Item Drain: refuse stage-locked {@code create:emptying} recipes.
 */
@Mixin(value = ItemDrainBlockEntity.class, remap = false)
public abstract class ItemDrainBlockEntityMixin {
    @Shadow(remap = false)
    TransportedItemStack heldItem;

    @WrapMethod(method = "continueProcessing", remap = false)
    private boolean anotherStages$gateContinue(Operation<Boolean> original) {
        if (!AsConfig.mixinCreate() || heldItem == null || heldItem.stack == null || heldItem.stack.isEmpty()) {
            return original.call();
        }
        ItemDrainBlockEntity self = (ItemDrainBlockEntity) (Object) this;
        Level level = ((BlockEntity) (Object) self).getLevel();
        if (level == null || level.isClientSide()) {
            return original.call();
        }
        Optional<RecipeHolder<EmptyingRecipe>> recipe =
                AllRecipeTypes.EMPTYING.find(new SingleRecipeInput(heldItem.stack), level);
        if (recipe.isPresent()
                && RecipeStageGate.isBlockedForOwner(
                        recipe.get().id(), MachineOwners.get(self), level)) {
            return false;
        }
        return original.call();
    }
}
