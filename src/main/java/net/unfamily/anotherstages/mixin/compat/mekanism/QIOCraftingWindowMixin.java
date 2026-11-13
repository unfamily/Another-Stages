package net.unfamily.anotherstages.mixin.compat.mekanism;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mekanism.common.content.qio.QIOCraftingWindow;
import mekanism.common.inventory.container.slot.HotBarSlot;
import mekanism.common.inventory.container.slot.MainInventorySlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.unfamily.anotherstages.compat.mekanism.QioCraftGate;
import net.unfamily.anotherstages.compat.mekanism.QioCraftSubjects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mekanism QIO crafting window: block craft / view when recipe is stage-locked.
 * Primary gate is {@code canViewRecipe} (mayPickup / canCraft sync); {@code performCraft}
 * remains as defense for shift-click paths.
 */
@Mixin(value = QIOCraftingWindow.class, remap = false)
public abstract class QIOCraftingWindowMixin {
    @WrapMethod(method = "canViewRecipe", remap = false)
    private boolean anotherStages$gateCanViewRecipe(ServerPlayer player, Operation<Boolean> original) {
        if (QioCraftGate.isBlocked((QIOCraftingWindow) (Object) this, player)) {
            return false;
        }
        return original.call(player);
    }

    @Inject(method = "canViewRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void anotherStages$cancelCanViewRecipe(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (QioCraftGate.isBlocked((QIOCraftingWindow) (Object) this, player)) {
            cir.setReturnValue(false);
        }
    }

    @WrapMethod(
            method = "performCraft(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;",
            remap = false)
    private ItemStack anotherStages$gatePerformCraftStack(
            Player player, ItemStack result, int amount, Operation<ItemStack> original) {
        try {
            if (QioCraftGate.isBlocked((QIOCraftingWindow) (Object) this, player)) {
                return ItemStack.EMPTY;
            }
            return original.call(player, result, amount);
        } finally {
            QioCraftSubjects.clear();
        }
    }

    @Inject(
            method = "performCraft(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void anotherStages$cancelPerformCraftStack(
            Player player, ItemStack result, int amount, CallbackInfoReturnable<ItemStack> cir) {
        if (QioCraftGate.isBlocked((QIOCraftingWindow) (Object) this, player)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @WrapMethod(
            method = "performCraft(Lnet/minecraft/world/entity/player/Player;Ljava/util/List;Ljava/util/List;)V",
            remap = false)
    private void anotherStages$gatePerformCraftList(
            Player player,
            List<HotBarSlot> hotBar,
            List<MainInventorySlot> mainInv,
            Operation<Void> original) {
        try {
            if (QioCraftGate.isBlocked((QIOCraftingWindow) (Object) this, player)) {
                return;
            }
            original.call(player, hotBar, mainInv);
        } finally {
            QioCraftSubjects.clear();
        }
    }

    @Inject(
            method = "performCraft(Lnet/minecraft/world/entity/player/Player;Ljava/util/List;Ljava/util/List;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void anotherStages$cancelPerformCraftList(
            Player player,
            List<HotBarSlot> hotBar,
            List<MainInventorySlot> mainInv,
            CallbackInfo ci) {
        if (QioCraftGate.isBlocked((QIOCraftingWindow) (Object) this, player)) {
            ci.cancel();
        }
    }

    @WrapMethod(
            method = "performCraft(Lnet/minecraft/world/entity/player/Player;Ljava/util/List;Ljava/util/List;Lnet/neoforged/neoforge/transfer/transaction/TransactionContext;)V",
            remap = false)
    private void anotherStages$gatePerformCraftTx(
            Player player,
            List<HotBarSlot> hotBar,
            List<MainInventorySlot> mainInv,
            TransactionContext tx,
            Operation<Void> original) {
        try {
            if (QioCraftGate.isBlocked((QIOCraftingWindow) (Object) this, player)) {
                return;
            }
            original.call(player, hotBar, mainInv, tx);
        } finally {
            QioCraftSubjects.clear();
        }
    }

    @Inject(
            method = "performCraft(Lnet/minecraft/world/entity/player/Player;Ljava/util/List;Ljava/util/List;Lnet/neoforged/neoforge/transfer/transaction/TransactionContext;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void anotherStages$cancelPerformCraftTx(
            Player player,
            List<HotBarSlot> hotBar,
            List<MainInventorySlot> mainInv,
            TransactionContext tx,
            CallbackInfo ci) {
        if (QioCraftGate.isBlocked((QIOCraftingWindow) (Object) this, player)) {
            ci.cancel();
        }
    }
}
