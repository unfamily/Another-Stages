package net.unfamily.anotherstages.mixin.compat.enderio;

import com.enderio.enderio.content.storage.crafter.CrafterBlockEntity;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CrafterBlockEntity.class, remap = false)
public abstract class CrafterBlockEntityMixin {
    @Shadow(remap = false)
    private RecipeHolder<CraftingRecipe> recipe;

    @Inject(method = "updateRecipe", at = @At("RETURN"), remap = false)
    private void anotherStages$gateUpdateRecipe(CallbackInfo ci) {
        if (!AsConfig.mixinEnderio() || recipe == null) {
            return;
        }
        CrafterBlockEntity self = (CrafterBlockEntity) (Object) this;
        if (RecipeStageGate.isBlockedForOwner(recipe.id(), MachineOwners.get(self), self.getLevel())) {
            recipe = null;
        }
    }
}
