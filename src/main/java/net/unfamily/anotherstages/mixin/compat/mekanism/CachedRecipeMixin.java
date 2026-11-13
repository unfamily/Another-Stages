package net.unfamily.anotherstages.mixin.compat.mekanism;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mekanism.api.recipes.cache.CachedRecipe;
import net.unfamily.anotherstages.compat.mekanism.MekRecipeCacheGate;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.BooleanSupplier;

/**
 * AND stage lock into {@code canHolderFunction} so {@link CachedRecipe#process()} stops on re-lock
 * even while Mek keeps the same cache (inputs unchanged → no {@code getRecipe} recall).
 */
@Mixin(value = CachedRecipe.class, remap = false)
public abstract class CachedRecipeMixin {
    @WrapMethod(
            method = "setCanHolderFunction(Ljava/util/function/BooleanSupplier;)Lmekanism/api/recipes/cache/CachedRecipe;",
            remap = false)
    private CachedRecipe<?> anotherStages$andStageGate(
            BooleanSupplier canHolder, Operation<CachedRecipe<?>> original) {
        if (!AsConfig.mixinMekanism() || canHolder == null) {
            return original.call(canHolder);
        }
        CachedRecipe<?> self = (CachedRecipe<?>) (Object) this;
        BooleanSupplier gated = () -> canHolder.getAsBoolean() && MekRecipeCacheGate.canProcessCached(self);
        return original.call(gated);
    }
}
