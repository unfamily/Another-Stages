package net.unfamily.anotherstages.mixin.compat.createenchantmentindustry;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.compat.createenchantmentindustry.CeiPrinterSubject;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import org.spongepowered.asm.mixin.Mixin;
import plus.dragons.createenchantmentindustry.common.fluids.printer.PrinterBlockEntity;

/**
 * Bind {@link MachineOwners} for printer recipe stage checks during belt processing.
 */
@Mixin(value = PrinterBlockEntity.class, remap = false)
public abstract class PrinterBlockEntityMixin {
    @WrapMethod(
            method = "onItemEnters(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour;)Lcom/simibubi/create/content/kinetics/belt/behaviour/BeltProcessingBehaviour$ProcessingResult;",
            remap = false)
    private BeltProcessingBehaviour.ProcessingResult anotherStages$bindEnter(
            TransportedItemStack stack,
            TransportedItemStackHandlerBehaviour handler,
            Operation<BeltProcessingBehaviour.ProcessingResult> original) {
        return bindAndCall(stack, handler, original);
    }

    @WrapMethod(
            method = "onItemHeld(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour;)Lcom/simibubi/create/content/kinetics/belt/behaviour/BeltProcessingBehaviour$ProcessingResult;",
            remap = false)
    private BeltProcessingBehaviour.ProcessingResult anotherStages$bindHeld(
            TransportedItemStack stack,
            TransportedItemStackHandlerBehaviour handler,
            Operation<BeltProcessingBehaviour.ProcessingResult> original) {
        return bindAndCall(stack, handler, original);
    }

    private BeltProcessingBehaviour.ProcessingResult bindAndCall(
            TransportedItemStack stack,
            TransportedItemStackHandlerBehaviour handler,
            Operation<BeltProcessingBehaviour.ProcessingResult> original) {
        if (!AsConfig.mixinCreateEnchantmentIndustry()) {
            return original.call(stack, handler);
        }
        BlockEntity be = (BlockEntity) (Object) this;
        CeiPrinterSubject.push(MachineOwners.get(be));
        try {
            return original.call(stack, handler);
        } finally {
            CeiPrinterSubject.clear();
        }
    }
}
