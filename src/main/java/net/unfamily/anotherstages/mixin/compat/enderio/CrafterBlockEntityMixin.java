package net.unfamily.anotherstages.mixin.compat.enderio;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Ender IO Crafter: refuse craft start when stage-locked.
 * Gate on {@code tryCraft}/{@code craftItem} (re-checks every act tick) so unlock works
 * without yanking inputs. {@link Pseudo} avoids Mixin AP hierarchy failures from EIO jars.
 */
@Pseudo
@Mixin(targets = "com.enderio.enderio.content.storage.crafter.CrafterBlockEntity", remap = false)
public abstract class CrafterBlockEntityMixin {
    @Shadow(remap = false)
    private RecipeHolder<CraftingRecipe> recipe;

    @WrapMethod(method = "tryCraft", remap = false)
    private void anotherStages$gateTryCraft(Operation<Void> original) {
        if (AsConfig.mixinEnderio() && isRecipeBlocked()) {
            return;
        }
        original.call();
    }

    @WrapMethod(method = "craftItem", remap = false)
    private void anotherStages$gateCraftItem(Operation<Void> original) {
        if (AsConfig.mixinEnderio() && isRecipeBlocked()) {
            return;
        }
        original.call();
    }

    private boolean isRecipeBlocked() {
        if (recipe == null) {
            return false;
        }
        BlockEntity self = (BlockEntity) (Object) this;
        return RecipeStageGate.isBlockedForOwner(recipe.id(), MachineOwners.get(self), self.getLevel());
    }
}
