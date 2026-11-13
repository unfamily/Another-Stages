package net.unfamily.anotherstages.mixin.compat.mekanism;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import mekanism.common.content.qio.QIOCraftingTransferHelper;
import mekanism.common.content.qio.QIOServerCraftingTransferHandler;
import mekanism.common.inventory.container.QIOItemViewerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Block QIO JEI/REI fill-transfer when the target crafting recipe is stage-locked.
 * WrapMethod + Inject HEAD (Inject cancels if WrapMethod fails to apply).
 */
@Mixin(value = QIOServerCraftingTransferHandler.class, remap = false)
public abstract class QIOServerCraftingTransferHandlerMixin {
    @WrapMethod(
            method = "tryTransfer(Lmekanism/common/inventory/container/QIOItemViewerContainer;BZLnet/minecraft/world/entity/player/Player;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/world/item/crafting/CraftingRecipe;Lit/unimi/dsi/fastutil/bytes/Byte2ObjectMap;)V",
            remap = false)
    private static void anotherStages$gateTryTransfer(
            QIOItemViewerContainer container,
            byte window,
            boolean rejectToInventory,
            Player player,
            ResourceLocation recipeId,
            CraftingRecipe recipe,
            Byte2ObjectMap<List<QIOCraftingTransferHelper.SingularHashedItemSource>> sources,
            Operation<Void> original) {
        if (anotherStages$blocked(player, recipeId)) {
            return;
        }
        original.call(container, window, rejectToInventory, player, recipeId, recipe, sources);
    }

    @Inject(
            method = "tryTransfer(Lmekanism/common/inventory/container/QIOItemViewerContainer;BZLnet/minecraft/world/entity/player/Player;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/world/item/crafting/CraftingRecipe;Lit/unimi/dsi/fastutil/bytes/Byte2ObjectMap;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private static void anotherStages$cancelTryTransfer(
            QIOItemViewerContainer container,
            byte window,
            boolean rejectToInventory,
            Player player,
            ResourceLocation recipeId,
            CraftingRecipe recipe,
            Byte2ObjectMap<List<QIOCraftingTransferHelper.SingularHashedItemSource>> sources,
            CallbackInfo ci) {
        if (anotherStages$blocked(player, recipeId)) {
            ci.cancel();
        }
    }

    private static boolean anotherStages$blocked(Player player, ResourceLocation recipeId) {
        if (!AsConfig.mixinMekanism() || player == null) {
            return false;
        }
        if (recipeId == null) {
            return true;
        }
        return RecipeStageGate.isBlockedForPlayer(recipeId, player, player.level());
    }
}
