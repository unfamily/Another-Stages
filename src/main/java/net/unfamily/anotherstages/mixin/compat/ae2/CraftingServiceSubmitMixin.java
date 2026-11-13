package net.unfamily.anotherstages.mixin.compat.ae2;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.crafting.execution.CraftingSubmitResult;
import appeng.me.service.CraftingService;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.ae2.Ae2CraftGate;
import net.unfamily.anotherstages.compat.ae2.Ae2NetworkOwners;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

/**
 * Block AE2 autocraft job submit (player confirm and automatic requesters) when any
 * craft/stonecutting/smithing pattern in the plan is stage-locked for the action subject.
 */
@Mixin(value = CraftingService.class, remap = false)
public abstract class CraftingServiceSubmitMixin {
    @Shadow(remap = false)
    private appeng.api.networking.IGrid grid;

    @WrapMethod(method = "submitJob", remap = false)
    private ICraftingSubmitResult anotherStages$gateSubmitJob(
            ICraftingPlan job,
            ICraftingRequester requestingMachine,
            ICraftingCPU target,
            boolean prioritizePower,
            IActionSource src,
            Operation<ICraftingSubmitResult> original) {
        if (AsConfig.mixinAe2() && job != null) {
            Level level = grid != null && grid.getPivot() != null ? grid.getPivot().getLevel() : null;
            UUID owner = Ae2NetworkOwners.fromActionSource(src, grid != null ? grid.getPivot() : null);
            if (Ae2CraftGate.isPlanBlocked(job, owner, level)) {
                Player player = src != null ? src.player().orElse(null) : null;
                Ae2CraftGate.notifyBlocked(player, (net.minecraft.resources.ResourceLocation) null, false);
                return CraftingSubmitResult.INCOMPLETE_PLAN;
            }
        }
        return original.call(job, requestingMachine, target, prioritizePower, src);
    }
}
