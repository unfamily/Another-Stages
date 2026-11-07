package net.unfamily.anotherstages.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Blocks crafting result when the matched recipe is stage-gated (3x3 table and inventory 2x2).
 * Post-clears like FastBench so a failed gate never leaves a craftable result slot.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @WrapMethod(method = "slotChangedCraftingGrid")
    private static void anotherStages$gateSlotChangedCraftingGrid(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer container,
            ResultContainer resultSlots,
            @Nullable RecipeHolder<CraftingRecipe> recipeHint,
            Operation<Void> original) {
        original.call(menu, level, player, container, resultSlots, recipeHint);
        if (!AsConfig.mixinCrafting()) {
            return;
        }
        RecipeHolder<?> used = resultSlots.getRecipeUsed();
        if (used != null
                && player instanceof ServerPlayer
                && RecipeStageGate.isBlockedForPlayer(used.id(), player, level)) {
            resultSlots.setItem(0, ItemStack.EMPTY);
            resultSlots.setRecipeUsed(null);
            menu.setRemoteSlot(0, ItemStack.EMPTY);
            ((ServerPlayer) player).connection.send(
                    new ClientboundContainerSetSlotPacket(
                            menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY));
        }
    }
}
