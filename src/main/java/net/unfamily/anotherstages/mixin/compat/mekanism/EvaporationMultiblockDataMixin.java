package net.unfamily.anotherstages.mixin.compat.mekanism;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mekanism.api.recipes.FluidToFluidRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.content.evaporation.EvaporationMultiblockData;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.mekanism.MekElectricStageRearm;
import net.unfamily.anotherstages.compat.mekanism.MekRecipeCacheGate;
import net.unfamily.anotherstages.compat.mekanism.MekRecipeIds;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

/**
 * Thermal Evaporation Plant: gate FluidToFluid recipe start.
 * Handler is {@link EvaporationMultiblockData} (not a TE) and does not set {@code canHolderFunction}
 * natively — we AND stage into canHolder after create so re-lock stops process.
 * Owner via {@link MekRecipeCacheGate#resolveOwner} (Mek security first, {@code MachineOwners} if absent).
 */
@Mixin(value = EvaporationMultiblockData.class, remap = false)
public abstract class EvaporationMultiblockDataMixin {
    @WrapMethod(
            method = "getRecipe(I)Lmekanism/api/recipes/FluidToFluidRecipe;",
            remap = false)
    private FluidToFluidRecipe anotherStages$gateGetRecipe(
            int cacheIndex, Operation<FluidToFluidRecipe> original) {
        FluidToFluidRecipe recipe = original.call(cacheIndex);
        if (anotherStages$blocked(recipe)) {
            MekElectricStageRearm.markSkipNoRecipe();
            return null;
        }
        return recipe;
    }

    @WrapMethod(
            method = "createNewCachedRecipe(Lmekanism/api/recipes/FluidToFluidRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            remap = false)
    private CachedRecipe<FluidToFluidRecipe> anotherStages$gateCreateCached(
            FluidToFluidRecipe recipe,
            int cacheIndex,
            Operation<CachedRecipe<FluidToFluidRecipe>> original) {
        if (anotherStages$blocked(recipe)) {
            MekElectricStageRearm.markSkipNoRecipe();
            return null;
        }
        CachedRecipe<FluidToFluidRecipe> cached = original.call(recipe, cacheIndex);
        if (cached != null && AsConfig.mixinMekanism()) {
            cached.setCanHolderFunction(() -> MekRecipeCacheGate.canProcessCached(cached));
        }
        return cached;
    }

    private boolean anotherStages$blocked(FluidToFluidRecipe recipe) {
        if (!AsConfig.mixinMekanism() || recipe == null) {
            return false;
        }
        EvaporationMultiblockData self = (EvaporationMultiblockData) (Object) this;
        MultiblockData multi = self;
        Level level = multi.getLevel();
        UUID owner = MekRecipeCacheGate.resolveOwner(self);
        IMekanismRecipeTypeProvider<?, ?, ?> type = self.getRecipeType();
        Identifier id = MekRecipeIds.idOf(recipe, level, type);
        if (id == null) {
            return true;
        }
        return RecipeStageGate.isBlockedForOwner(id, owner, level);
    }
}
