package net.unfamily.anotherstages.mixin.compat.ftbic;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ftb.mods.ftbic.block.entity.machine.MachineBlockEntity;
import dev.ftb.mods.ftbic.recipe.MachineRecipe;
import net.minecraft.resources.Identifier;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * FTBIC MachineBlockEntity (powered furnaces etc.): refuse findRecipe when running/adapted
 * recipe id is stage-locked. Clear sticky cache so unlock re-finds without yanking inputs.
 */
@Mixin(value = MachineBlockEntity.class, remap = false)
public abstract class MachineBlockEntityMixin {
    @Shadow(remap = false)
    private String runningRecipeId;

    @Shadow(remap = false)
    private MachineRecipe cachedRecipe;

    @Shadow(remap = false)
    private boolean recipeDirty;

    @WrapMethod(method = "findRecipe", remap = false)
    private MachineRecipe anotherStages$gateFindRecipe(Operation<MachineRecipe> original) {
        MachineRecipe recipe = original.call();
        if (!AsConfig.mixinFtbic() || recipe == null) {
            return recipe;
        }
        MachineBlockEntity self = (MachineBlockEntity) (Object) this;
        String idStr = runningRecipeId;
        if (idStr == null || idStr.isEmpty()) {
            return recipe;
        }
        Identifier id = Identifier.tryParse(idStr);
        if (id == null) {
            return recipe;
        }
        if (RecipeStageGate.isBlockedForOwner(id, MachineOwners.get(self), self.getLevel())) {
            // Clear sticky selection so unlock re-runs findRecipe / selectRecipe.
            this.cachedRecipe = null;
            this.runningRecipeId = "";
            this.recipeDirty = true;
            return null;
        }
        return recipe;
    }
}
