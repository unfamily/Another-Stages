package net.unfamily.anotherstages.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.gui.StageGuiHandler;
import net.unfamily.anotherstages.gui.StageGuiRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Gates slot clicks for active {@code stage_gui} rules (blocked slots / blocked items).
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuClickedMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void anotherStages$gateGuiSlots(
            int slotIndex, int buttonNum, ContainerInput containerInput, Player player, CallbackInfo ci) {
        if (!AsConfig.mixinGuiSlots()) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!StageGuiHandler.hasLoadedRules()) {
            return;
        }
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (self == serverPlayer.inventoryMenu) {
            return;
        }
        List<StageGuiRule> active = StageGuiHandler.findActiveRules(serverPlayer, self);
        if (active.isEmpty()) {
            return;
        }

        if (slotIndex >= 0 && StageGuiHandler.isSlotBlocked(active, slotIndex)) {
            ci.cancel();
            return;
        }

        ItemStack carried = self.getCarried();
        if (!carried.isEmpty() && StageGuiHandler.isItemBlockedInMenu(active, carried.getItem())) {
            // Placing a blocked item into the menu
            if (slotIndex >= 0 && slotIndex < self.slots.size()) {
                Slot slot = self.slots.get(slotIndex);
                if (slot.container != player.getInventory()) {
                    ci.cancel();
                    return;
                }
            }
        }

        if (slotIndex >= 0 && slotIndex < self.slots.size()) {
            Slot slot = self.slots.get(slotIndex);
            ItemStack inSlot = slot.getItem();
            if (!inSlot.isEmpty()
                    && slot.container != player.getInventory()
                    && StageGuiHandler.isItemBlockedInMenu(active, inSlot.getItem())) {
                ci.cancel();
            }
        }
    }
}
