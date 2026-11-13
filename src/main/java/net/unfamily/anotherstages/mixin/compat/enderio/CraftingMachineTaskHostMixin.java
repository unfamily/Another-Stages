package net.unfamily.anotherstages.mixin.compat.enderio;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.compat.enderio.EioStageRearm;
import net.unfamily.anotherstages.compat.enderio.EioTaskHostSubjects;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import java.util.Optional;

/**
 * Shared EIO craft host (Sag Mill, Slicer, Painting, Soul Binder, Vat): gate {@code findRecipe}.
 */
@Pseudo
@Mixin(targets = "com.enderio.enderio.foundation.task.host.CraftingMachineTaskHost", remap = false)
public abstract class CraftingMachineTaskHostMixin {
    @WrapMethod(method = "findRecipe", remap = false)
    private Optional<RecipeHolder<?>> anotherStages$gateFindRecipe(
            Operation<Optional<RecipeHolder<?>>> original) {
        Optional<RecipeHolder<?>> recipe = original.call();
        if (!AsConfig.mixinEnderio() || recipe.isEmpty()) {
            return recipe;
        }
        RecipeHolder<?> holder = recipe.get();
        BlockEntity be = EioTaskHostSubjects.get(this);
        Level level = be != null ? be.getLevel() : null;
        if (RecipeStageGate.isBlockedForMachine(holder.id(), be, level)) {
            EioStageRearm.mark(this);
            return Optional.empty();
        }
        return recipe;
    }
}
