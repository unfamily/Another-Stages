package net.unfamily.anotherstages.mixin.compat.fastfurnace;

import dev.shadowsoffire.fastfurnace.mixin.MixinAbstractFurnaceBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FastFurnace replaces CachedCheck lookups; gate its cached {@code getRecipe} result.
 */
@Mixin(value = MixinAbstractFurnaceBlockEntity.class, remap = false)
public abstract class FastFurnaceRecipeMixin {
    @Inject(method = "getRecipe", at = @At("RETURN"), cancellable = true, remap = false)
    private void anotherStages$gateFurnaceRecipe(
            ServerLevel level, CallbackInfoReturnable<RecipeHolder<?>> cir) {
        RecipeHolder<?> holder = cir.getReturnValue();
        if (holder != null
                && RecipeStageGate.isBlockedForMachine(holder.id(), (BlockEntity) (Object) this, level)) {
            cir.setReturnValue(null);
        }
    }
}
