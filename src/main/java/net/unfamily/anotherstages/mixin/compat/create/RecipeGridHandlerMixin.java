package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.create.CreateCrafterSubject;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

/**
 * Create Mechanical Crafter: refuse applying a stage-locked crafting / mechanical-crafting recipe.
 * Subject UUID comes from {@link CreateCrafterSubject} (MachineOwners on the crafter BE).
 * No owner is normal — world stages still apply.
 */
@Mixin(value = RecipeGridHandler.class, remap = false)
public abstract class RecipeGridHandlerMixin {
    @WrapMethod(method = "tryToApplyRecipe", remap = false)
    private static ItemStack anotherStages$gateTryToApplyRecipe(
            Level world, GroupedItems items, Operation<ItemStack> original) {
        ItemStack result = original.call(world, items);
        if (!AsConfig.mixinCreate() || result == null || result.isEmpty() || world == null) {
            return result;
        }
        UUID owner = CreateCrafterSubject.current();
        items.calcStats();
        CraftingInput craftingInput = com.simibubi.create.content.kinetics.crafter.MechanicalCraftingInput.of(items);
        var crafting = world.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingInput, world);
        if (crafting.isPresent()
                && RecipeStageGate.isBlockedForOwner(crafting.get().id(), owner, world)) {
            return null;
        }
        if (crafting.isEmpty()) {
            var mechanical = com.simibubi.create.AllRecipeTypes.MECHANICAL_CRAFTING.find(craftingInput, world);
            if (mechanical.isPresent()
                    && RecipeStageGate.isBlockedForOwner(mechanical.get().id(), owner, world)) {
                return null;
            }
        }
        return result;
    }
}
