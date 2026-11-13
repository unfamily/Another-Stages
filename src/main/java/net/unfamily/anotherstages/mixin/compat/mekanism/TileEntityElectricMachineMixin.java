package net.unfamily.anotherstages.mixin.compat.mekanism;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.prefab.TileEntityElectricMachine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.mekanism.MekElectricStageRearm;
import net.unfamily.anotherstages.compat.mekanism.MekRecipeIds;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

/**
 * Mekanism electric Item→Item machines (Energized Smelter, Enrichment Chamber, Crusher, …):
 * refuse cook/process start when the Mek recipe is stage-locked.
 */
@Mixin(value = TileEntityElectricMachine.class, remap = false)
public abstract class TileEntityElectricMachineMixin {
    @WrapMethod(
            method = "getRecipe(I)Lmekanism/api/recipes/ItemStackToItemStackRecipe;",
            remap = false)
    private ItemStackToItemStackRecipe anotherStages$gateGetRecipe(
            int cacheIndex, Operation<ItemStackToItemStackRecipe> original) {
        ItemStackToItemStackRecipe recipe = original.call(cacheIndex);
        if (anotherStages$blocked(recipe)) {
            MekElectricStageRearm.markSkipNoRecipe();
            return null;
        }
        return recipe;
    }

    @WrapMethod(
            method = "createNewCachedRecipe(Lmekanism/api/recipes/ItemStackToItemStackRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            remap = false)
    private CachedRecipe<ItemStackToItemStackRecipe> anotherStages$gateCreateCached(
            ItemStackToItemStackRecipe recipe,
            int cacheIndex,
            Operation<CachedRecipe<ItemStackToItemStackRecipe>> original) {
        if (anotherStages$blocked(recipe)) {
            MekElectricStageRearm.markSkipNoRecipe();
            return null;
        }
        return original.call(recipe, cacheIndex);
    }

    private boolean anotherStages$blocked(ItemStackToItemStackRecipe recipe) {
        if (!AsConfig.mixinMekanism() || recipe == null) {
            return false;
        }
        TileEntityMekanism self = (TileEntityMekanism) (Object) this;
        Level level = self.getLevel();
        UUID owner = self.getOwnerUUID();
        IMekanismRecipeTypeProvider<?, ?, ?> type = ((TileEntityElectricMachine) (Object) this).getRecipeType();
        ResourceLocation id = MekRecipeIds.idOf(recipe, level, type);
        // Fail-closed: recipe present but id unresolved must not bypass stage locks.
        if (id == null) {
            return true;
        }
        return RecipeStageGate.isBlockedForOwner(id, owner, level);
    }
}
