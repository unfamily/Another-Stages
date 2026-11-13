package net.unfamily.anotherstages.mixin.compat.mekanism;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mekanism.common.content.qio.QIOCraftingWindow;
import mekanism.common.inventory.container.slot.VirtualCraftingOutputSlot;
import net.minecraft.world.entity.player.Player;
import net.unfamily.anotherstages.compat.mekanism.QioCraftGate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Belt for QIO output take: refuse {@code mayPickup} when the crafting window recipe is
 * stage-locked (primary gate is {@code QIOCraftingWindow.canViewRecipe}).
 */
@Mixin(value = VirtualCraftingOutputSlot.class, remap = false)
public abstract class VirtualCraftingOutputSlotMixin {
    @Shadow(remap = false)
    @Final
    private QIOCraftingWindow craftingWindow;

    @WrapMethod(method = "mayPickup", remap = false)
    private boolean anotherStages$gateMayPickup(Player player, Operation<Boolean> original) {
        if (QioCraftGate.isBlocked(craftingWindow, player)) {
            return false;
        }
        return original.call(player);
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true, remap = false)
    private void anotherStages$cancelMayPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (QioCraftGate.isBlocked(craftingWindow, player)) {
            cir.setReturnValue(false);
        }
    }
}
