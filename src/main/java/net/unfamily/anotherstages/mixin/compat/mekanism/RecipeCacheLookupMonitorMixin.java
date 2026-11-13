package net.unfamily.anotherstages.mixin.compat.mekanism;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.recipe.lookup.IRecipeLookupHandler;
import mekanism.common.recipe.lookup.monitor.RecipeCacheLookupMonitor;
import net.unfamily.anotherstages.compat.mekanism.MekElectricStageRearm;
import net.unfamily.anotherstages.compat.mekanism.MekRecipeCacheGate;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mek cache + stage:
 * <ul>
 *   <li>Bind handler for {@link CachedRecipe} {@code canHolder} stage AND (re-lock stops process).</li>
 *   <li>Drop live cache when re-locked (belt-and-suspenders).</li>
 *   <li>Skip sticky {@code hasNoRecipe} when AS gated {@code getRecipe} to null (unlock without poke).</li>
 * </ul>
 */
@Mixin(value = RecipeCacheLookupMonitor.class, remap = false)
public abstract class RecipeCacheLookupMonitorMixin {
    @Shadow
    @Final
    private IRecipeLookupHandler<?> handler;

    @Shadow
    @Final
    protected int cacheIndex;

    @Shadow
    protected CachedRecipe<?> cachedRecipe;

    @Shadow
    protected boolean hasNoRecipe;

    @WrapMethod(method = "updateAndProcess()Z", remap = false)
    private boolean anotherStages$bindHandlerAndDropLocked(Operation<Boolean> original) {
        if (!AsConfig.mixinMekanism()) {
            return original.call();
        }
        if (this.cachedRecipe != null
                && MekRecipeCacheGate.isCachedBlocked(this.handler, this.cachedRecipe)) {
            this.cachedRecipe = null;
            this.hasNoRecipe = false;
            this.handler.onCachedRecipeChanged(null, this.cacheIndex);
            return false;
        }
        MekRecipeCacheGate.pushHandler(this.handler);
        try {
            boolean ran = original.call();
            if (this.cachedRecipe != null
                    && MekRecipeCacheGate.isCachedBlocked(this.handler, this.cachedRecipe)) {
                this.cachedRecipe = null;
                this.hasNoRecipe = false;
                this.handler.onCachedRecipeChanged(null, this.cacheIndex);
                return false;
            }
            return ran;
        } finally {
            MekRecipeCacheGate.popHandler();
        }
    }

    /** Force Mek to discard a stage-locked cache inside {@code getUpdatedCache}. */
    @Inject(method = "invalidateCache()Z", at = @At("RETURN"), cancellable = true, remap = false)
    private void anotherStages$invalidateWhenStageLocked(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || !AsConfig.mixinMekanism()) {
            return;
        }
        if (this.cachedRecipe != null
                && MekRecipeCacheGate.isCachedBlocked(this.handler, this.cachedRecipe)) {
            cir.setReturnValue(true);
        }
    }

    @WrapMethod(method = "setHasNoRecipe", remap = false)
    private void anotherStages$skipStickyNoRecipe(int cacheIndex, Operation<Void> original) {
        if (MekElectricStageRearm.consumeSkipNoRecipe()) {
            return;
        }
        original.call(cacheIndex);
    }
}
