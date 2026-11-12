package net.unfamily.anotherstages.mixin.compat.extendedae;

import appeng.blockentity.misc.InscriberRecipes;
import appeng.recipes.handlers.InscriberRecipe;
import com.glodblock.github.extendedae.common.me.InscriberThread;
import com.glodblock.github.extendedae.common.tileentities.TileExInscriber;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.ae2.Ae2CraftGate;
import net.unfamily.anotherstages.compat.ae2.Ae2NetworkOwners;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ex Inscriber threads use the same AE2 inscriber recipe ids — gate {@code getTask} process lookup only.
 * Clears {@code cachedTask} when locked so stage changes re-evaluate without inventory churn.
 */
@Mixin(value = InscriberThread.class, remap = false)
public abstract class InscriberThreadMixin {
    @Shadow
    @Final
    private TileExInscriber host;

    @Shadow
    private InscriberRecipe cachedTask;

    @WrapOperation(
            method = "getTask",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/blockentity/misc/InscriberRecipes;findRecipe(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Z)Lappeng/recipes/handlers/InscriberRecipe;",
                    remap = false))
    private InscriberRecipe anotherStages$gateExInscriberTask(
            Level level,
            ItemStack input,
            ItemStack plateA,
            ItemStack plateB,
            boolean supportNamePress,
            Operation<InscriberRecipe> original) {
        InscriberRecipe recipe = original.call(level, input, plateA, plateB, supportNamePress);
        if (!AsConfig.mixinAe2() || recipe == null || level == null || this.host == null) {
            return recipe;
        }
        return anotherStages$blocked(recipe, level) ? null : recipe;
    }

    @Inject(method = "getTask", at = @At("RETURN"), cancellable = true, remap = false)
    private void anotherStages$clearCachedWhenLocked(CallbackInfoReturnable<InscriberRecipe> cir) {
        InscriberRecipe recipe = cir.getReturnValue();
        if (!AsConfig.mixinAe2() || recipe == null || this.host == null) {
            return;
        }
        Level level = this.host.getLevel();
        if (level == null) {
            return;
        }
        if (anotherStages$blocked(recipe, level)) {
            this.cachedTask = null;
            cir.setReturnValue(null);
        }
    }

    private boolean anotherStages$blocked(InscriberRecipe recipe, Level level) {
        for (RecipeHolder<InscriberRecipe> holder : InscriberRecipes.getRecipes(level)) {
            if (holder.value() == recipe
                    && Ae2CraftGate.isRecipeBlocked(
                            holder.id(),
                            Ae2NetworkOwners.fromNode(this.host.getMainNode().getNode()),
                            level)) {
                return true;
            }
        }
        return false;
    }
}
