package net.unfamily.anotherstages.mixin.compat.mekanism;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mekanism.common.content.qio.IQIOCraftingWindowHolder;
import mekanism.common.content.qio.QIOCraftingWindow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Mekanism QIO crafting window (1.21.1): block craft when recipe is stage-locked for the player. */
@Mixin(value = QIOCraftingWindow.class, remap = false)
public abstract class QIOCraftingWindowMixin {
    @Shadow(remap = false)
    @Final
    private IQIOCraftingWindowHolder holder;

    @Shadow(remap = false)
    private RecipeHolder<CraftingRecipe> lastRecipe;

    @WrapMethod(
            method = "performCraft(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;",
            remap = false)
    private ItemStack anotherStages$gatePerformCraft(
            Player player, ItemStack result, int amount, Operation<ItemStack> original) {
        if (AsConfig.mixinMekanism() && isRecipeBlocked(player)) {
            return ItemStack.EMPTY;
        }
        return original.call(player, result, amount);
    }

    private boolean isRecipeBlocked(Player player) {
        if (lastRecipe == null || player == null) {
            return false;
        }
        Level level = holder != null ? holder.getLevel() : player.level();
        return RecipeStageGate.isBlockedForPlayer(lastRecipe.id(), player, level);
    }
}
