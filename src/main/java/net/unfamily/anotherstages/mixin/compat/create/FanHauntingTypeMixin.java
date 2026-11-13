package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.create.CreateFanCookingGate;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.List;

/**
 * Create fan HAUNTING type: block stage-locked haunting recipes.
 */
@Mixin(value = com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes.HauntingType.class, remap = false)
public abstract class FanHauntingTypeMixin {
    @ModifyReturnValue(method = "canProcess", at = @At("RETURN"), remap = false)
    private boolean anotherStages$gateCanProcess(boolean original, ItemStack stack, Level level) {
        if (!original || !AsConfig.mixinCreate()) {
            return original;
        }
        return !CreateFanCookingGate.isHauntingBlocked(stack, level);
    }

    @ModifyReturnValue(method = "process", at = @At("RETURN"), remap = false)
    private List<ItemStack> anotherStages$gateProcess(List<ItemStack> original, ItemStack stack, Level level) {
        if (!AsConfig.mixinCreate() || original == null || original.isEmpty()) {
            return original;
        }
        if (CreateFanCookingGate.isHauntingBlocked(stack, level)) {
            return Collections.emptyList();
        }
        return original;
    }
}
