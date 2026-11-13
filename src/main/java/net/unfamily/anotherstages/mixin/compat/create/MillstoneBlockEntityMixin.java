package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

/**
 * Create Millstone: skip {@code process} when the milling recipe is stage-locked.
 */
@Mixin(value = MillstoneBlockEntity.class, remap = false)
public abstract class MillstoneBlockEntityMixin {
    @Shadow(remap = false)
    public ItemStackHandler inputInv;

    @Shadow(remap = false)
    private MillingRecipe lastRecipe;

    @WrapMethod(method = "process", remap = false)
    private void anotherStages$gateProcess(Operation<Void> original) {
        if (!AsConfig.mixinCreate()) {
            original.call();
            return;
        }
        MillstoneBlockEntity self = (MillstoneBlockEntity) (Object) this;
        Level level = ((BlockEntity) (Object) self).getLevel();
        if (level == null || level.isClientSide()) {
            original.call();
            return;
        }
        RecipeWrapper wrapper = new RecipeWrapper(this.inputInv);
        Optional<RecipeHolder<MillingRecipe>> found = AllRecipeTypes.MILLING.find(wrapper, level);
        if (found.isEmpty() && this.lastRecipe != null && this.lastRecipe.matches(wrapper, level)) {
            // Keep using cached lastRecipe — resolve via CreateRecipeIds path in gate below
            found = Optional.empty();
        }
        if (found.isPresent()) {
            if (RecipeStageGate.isBlockedForOwner(
                    found.get().id(), MachineOwners.get(self), level)) {
                this.lastRecipe = null;
                return;
            }
        } else if (this.lastRecipe != null) {
            var id = net.unfamily.anotherstages.compat.create.CreateRecipeIds.idOf(this.lastRecipe, level);
            if (id != null
                    && RecipeStageGate.isBlockedForOwner(id, MachineOwners.get(self), level)) {
                this.lastRecipe = null;
                return;
            }
        }
        original.call();
    }
}
