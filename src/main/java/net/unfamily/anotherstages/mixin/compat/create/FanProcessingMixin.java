package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.unfamily.anotherstages.compat.create.CreateFanSubject;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

/**
 * Push ItemEntity thrower/owner into {@link CreateFanSubject} around fan entity processing.
 */
@Mixin(value = FanProcessing.class, remap = false)
public abstract class FanProcessingMixin {
    @WrapMethod(
            method = "canProcess(Lnet/minecraft/world/entity/item/ItemEntity;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Z",
            remap = false)
    private static boolean anotherStages$withSubjectCanProcess(
            ItemEntity entity, FanProcessingType type, Operation<Boolean> original) {
        return withSubject(entity, () -> original.call(entity, type));
    }

    @WrapMethod(
            method = "applyProcessing(Lnet/minecraft/world/entity/item/ItemEntity;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Z",
            remap = false)
    private static boolean anotherStages$withSubjectApply(
            ItemEntity entity, FanProcessingType type, Operation<Boolean> original) {
        return withSubject(entity, () -> original.call(entity, type));
    }

    private static boolean withSubject(ItemEntity entity, java.util.function.BooleanSupplier call) {
        if (!AsConfig.mixinCreate() || entity == null) {
            return call.getAsBoolean();
        }
        UUID owner = null;
        Entity thrower = entity.getOwner();
        if (thrower != null) {
            owner = thrower.getUUID();
        }
        CreateFanSubject.push(owner);
        try {
            return call.getAsBoolean();
        } finally {
            CreateFanSubject.clear();
        }
    }
}
