package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.anotherstages.compat.create.CreateFillingGate;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.PASS;

/**
 * Create Spout: refuse stage-locked {@code create:filling} recipes.
 */
@Mixin(value = SpoutBlockEntity.class, remap = false)
public abstract class SpoutBlockEntityMixin {
    @Shadow(remap = false)
    SmartFluidTankBehaviour tank;

    @Shadow(remap = false)
    private FluidStack getCurrentFluidInTank() {
        throw new AssertionError();
    }

    private boolean anotherStages$fillingBlocked(TransportedItemStack transported) {
        SpoutBlockEntity self = (SpoutBlockEntity) (Object) this;
        Level level = ((BlockEntity) (Object) self).getLevel();
        if (level == null || tank == null || tank.isEmpty()) {
            return false;
        }
        FluidStack fluid = getCurrentFluidInTank();
        ResourceLocation id = CreateFillingGate.fillingRecipeId(level, transported.stack, fluid);
        if (id == null) {
            return false;
        }
        return RecipeStageGate.isBlockedForOwner(id, MachineOwners.get(self), level);
    }

    @WrapMethod(method = "onItemReceived", remap = false)
    private ProcessingResult anotherStages$gateReceived(
            TransportedItemStack transported,
            TransportedItemStackHandlerBehaviour handler,
            Operation<ProcessingResult> original) {
        if (AsConfig.mixinCreate() && anotherStages$fillingBlocked(transported)) {
            return PASS;
        }
        return original.call(transported, handler);
    }

    @WrapMethod(method = "whenItemHeld", remap = false)
    private ProcessingResult anotherStages$gateHeld(
            TransportedItemStack transported,
            TransportedItemStackHandlerBehaviour handler,
            Operation<ProcessingResult> original) {
        if (AsConfig.mixinCreate() && anotherStages$fillingBlocked(transported)) {
            return PASS;
        }
        return original.call(transported, handler);
    }
}
