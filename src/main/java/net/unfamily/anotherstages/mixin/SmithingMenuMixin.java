package net.unfamily.anotherstages.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Clears smithing result when the matched recipe is stage-gated.
 * Syncs the empty result slot to the client (same pattern as {@link CraftingMenuMixin}).
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {
    @WrapMethod(method = "createResult")
    private void anotherStages$gateSmithing(Operation<Void> original) {
        original.call();
        if (!AsConfig.mixinSmithing()) {
            return;
        }
        ResultContainer resultSlots = ((ItemCombinerMenuResultAccessor) this).anotherStages$getResultSlots();
        RecipeHolder<?> used = resultSlots.getRecipeUsed();
        if (used == null) {
            return;
        }
        Player player = ((ItemCombinerMenuAccessor) this).anotherStages$getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!RecipeStageGate.isBlockedForPlayer(used.id(), player, player.level())) {
            return;
        }
        SmithingMenu menu = (SmithingMenu) (Object) this;
        int resultSlot = menu.getResultSlot();
        resultSlots.setItem(0, ItemStack.EMPTY);
        resultSlots.setRecipeUsed(null);
        menu.setRemoteSlot(resultSlot, ItemStack.EMPTY);
        serverPlayer.connection.send(
                new ClientboundContainerSetSlotPacket(
                        menu.containerId, menu.incrementStateId(), resultSlot, ItemStack.EMPTY));
    }
}
