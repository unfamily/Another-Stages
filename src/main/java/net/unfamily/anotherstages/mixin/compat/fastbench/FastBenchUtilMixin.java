package net.unfamily.anotherstages.mixin.compat.fastbench;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.shadowsoffire.fastbench.util.CraftingInventoryExt;
import dev.shadowsoffire.fastbench.util.FastBenchUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FastWorkbench replaces {@code CraftingMenu.slotChangedCraftingGrid}; gate results here.
 */
@Mixin(value = FastBenchUtil.class, remap = false)
public abstract class FastBenchUtilMixin {
    @WrapMethod(method = "slotChangedCraftingGrid", remap = false)
    private static void anotherStages$gateSlotChange(
            ServerLevel level,
            Player player,
            CraftingInventoryExt inv,
            ResultContainer result,
            Operation<Void> original) {
        original.call(level, player, inv, result);
        if (!AsConfig.mixinFastbench()) {
            return;
        }
        RecipeHolder<?> used = result.getRecipeUsed();
        if (used != null
                && player instanceof ServerPlayer
                && RecipeStageGate.isBlockedForPlayer(used.id(), player, level)) {
            result.setItem(0, ItemStack.EMPTY);
            result.setRecipeUsed(null);
        }
    }

    @Inject(
            method = "handleShiftCraft(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/inventory/Slot;Ldev/shadowsoffire/fastbench/util/CraftingInventoryExt;Lnet/minecraft/world/inventory/ResultContainer;Ldev/shadowsoffire/fastbench/util/FastBenchUtil$OutputMover;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private static void anotherStages$gateShiftCraft(
            Player player,
            AbstractContainerMenu menu,
            Slot slot,
            CraftingInventoryExt inv,
            ResultContainer result,
            FastBenchUtil.OutputMover mover,
            CallbackInfoReturnable<ItemStack> cir) {
        if (!AsConfig.mixinFastbench()) {
            return;
        }
        RecipeHolder<?> used = result.getRecipeUsed();
        if (used != null
                && player instanceof ServerPlayer
                && RecipeStageGate.isBlockedForPlayer(used.id(), player, player.level())) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
