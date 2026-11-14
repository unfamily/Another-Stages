package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Create manual item application (right-click block): same stage lock as Deployer /
 * JEI {@code …_using_deployer} dual-id. Abort before Create cancels the interact event.
 */
@Mixin(value = ManualApplicationRecipe.class, remap = false)
public abstract class ManualApplicationRecipeMixin {
    @Inject(
            method = "manualApplicationRecipesApplyInWorld",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/event/entity/player/PlayerInteractEvent$RightClickBlock;setCancellationResult(Lnet/minecraft/world/InteractionResult;)V"),
            cancellable = true,
            remap = false)
    private static void anotherStages$gateManualApplication(
            PlayerInteractEvent.RightClickBlock event,
            CallbackInfo ci,
            @Local Optional<? extends RecipeHolder<?>> foundRecipe) {
        if (!AsConfig.mixinCreate() || foundRecipe == null || foundRecipe.isEmpty()) {
            return;
        }
        RecipeHolder<?> holder = foundRecipe.get();
        if (RecipeStageGate.isBlockedForPlayer(holder.id(), event.getEntity(), event.getLevel())) {
            ci.cancel();
        }
    }
}
