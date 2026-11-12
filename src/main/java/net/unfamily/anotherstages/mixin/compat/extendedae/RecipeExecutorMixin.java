package net.unfamily.anotherstages.mixin.compat.extendedae;

import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import com.glodblock.github.extendedae.api.IRecipeMachine;
import com.glodblock.github.extendedae.util.RecipeExecutor;
import com.glodblock.github.glodium.recipe.RecipeSearchContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * Gates ExtAE Circuit Cutter / Crystal Assembler / Crystal Fixer process start via shared
 * {@link RecipeExecutor}: locked recipes never gain progress and never run/consume.
 */
@Mixin(value = RecipeExecutor.class, remap = false)
public abstract class RecipeExecutorMixin {
    @Shadow
    @Final
    private IRecipeMachine<?, ?> machine;

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private void anotherStages$gateExtAeProcess(
            int steps, boolean usePower, CallbackInfoReturnable<TickRateModulation> cir) {
        if (!AsConfig.mixinAe2() || this.machine == null) {
            return;
        }
        RecipeSearchContext<?, ?> ctx = this.machine.getContext();
        if (ctx == null) {
            return;
        }
        RecipeHolder<?> holder = ctx.currentRecipe;
        if (holder == null) {
            return;
        }
        Level level = this.machine instanceof BlockEntity be ? be.getLevel() : null;
        IManagedGridNode node = this.machine.getNode();
        if (Ae2CraftGate.isRecipeBlocked(
                holder.id(),
                Ae2NetworkOwners.fromNode(node != null ? node.getNode() : null),
                level)) {
            // Clear current only — do not set stuck (that blocks findRecipe until inventory change).
            ctx.currentRecipe = null;
            ctx.stuck = false;
            this.machine.setWorking(false);
            this.machine.setProgress(0);
            cir.setReturnValue(TickRateModulation.IDLE);
        }
    }
}
