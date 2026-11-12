package net.unfamily.anotherstages.mixin.compat.energizedpower;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.jddev0.ep.block.entity.AbstractAutoCrafterBlockEntity;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AbstractAutoCrafterBlockEntity.class, remap = false)
public abstract class AbstractAutoCrafterBlockEntityMixin {
    @Shadow(remap = false)
    private RecipeHolder<CraftingRecipe>[] craftingRecipe;

    @WrapMethod(method = "hasWork", remap = false)
    private boolean anotherStages$gateHasWork(int index, Operation<Boolean> original) {
        if (!original.call(index)) {
            return false;
        }
        if (!AsConfig.mixinEnergizedPower()
                || craftingRecipe == null
                || index < 0
                || index >= craftingRecipe.length
                || craftingRecipe[index] == null) {
            return true;
        }
        AbstractAutoCrafterBlockEntity self = (AbstractAutoCrafterBlockEntity) (Object) this;
        return !RecipeStageGate.isBlockedForOwner(
                craftingRecipe[index].id(), MachineOwners.get(self), self.getLevel());
    }
}
