package net.unfamily.anotherstages.mixin.compat.createdragonsplus;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.createdragonsplus.CreateDragonsPlusFanGate;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import plus.dragons.createdragonsplus.common.kinetics.fan.freezing.FreezingFanProcessingType;

import java.util.Collections;
import java.util.List;

/**
 * Create Dragons Plus fan FREEZING: block stage-locked freezing recipes.
 */
@Mixin(value = FreezingFanProcessingType.class, remap = false)
public abstract class FanFreezingTypeMixin {
    @ModifyReturnValue(method = "canProcess", at = @At("RETURN"), remap = false)
    private boolean anotherStages$gateCanProcess(boolean original, ItemStack stack, Level level) {
        if (!original || !AsConfig.mixinCreateDragonsPlus()) {
            return original;
        }
        return !CreateDragonsPlusFanGate.isFreezingBlocked(stack, level);
    }

    @ModifyReturnValue(method = "process", at = @At("RETURN"), remap = false)
    private List<ItemStack> anotherStages$gateProcess(List<ItemStack> original, ItemStack stack, Level level) {
        if (!AsConfig.mixinCreateDragonsPlus() || original == null || original.isEmpty()) {
            return original;
        }
        if (CreateDragonsPlusFanGate.isFreezingBlocked(stack, level)) {
            return Collections.emptyList();
        }
        return original;
    }
}
