package net.unfamily.anotherstages.mixin.compat.mekanism;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mekanism.common.content.assemblicator.RecipeFormula;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.machine.TileEntityFormulaicAssemblicator;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

/** Mekanism Formulaic Assemblicator (1.21.1): block craft start when formula recipe is stage-locked. */
@Mixin(value = TileEntityFormulaicAssemblicator.class, remap = false)
public abstract class TileEntityFormulaicAssemblicatorMixin {
    @Shadow(remap = false)
    public RecipeFormula formula;

    @Shadow
    public abstract Level getLevel();

    @WrapMethod(method = "doSingleCraft", remap = false)
    private boolean anotherStages$gateDoSingleCraft(Operation<Boolean> original) {
        if (AsConfig.mixinMekanism() && isFormulaBlocked()) {
            return false;
        }
        return original.call();
    }

    @WrapMethod(method = "craftSingle", remap = false)
    private boolean anotherStages$gateCraftSingle(Operation<Boolean> original) {
        if (AsConfig.mixinMekanism() && isFormulaBlocked()) {
            return false;
        }
        return original.call();
    }

    private boolean isFormulaBlocked() {
        if (formula == null || !formula.valid()) {
            return false;
        }
        RecipeHolder<CraftingRecipe> holder = formula.recipe();
        if (holder == null) {
            return false;
        }
        UUID owner = ((TileEntityMekanism) (Object) this).getOwnerUUID();
        return RecipeStageGate.isBlockedForOwner(holder.id(), owner, getLevel());
    }
}
