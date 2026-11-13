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
import org.spongepowered.asm.mixin.Unique;

/** Mekanism QIO crafting window: block craft when recipe is stage-locked for the player. */
@Mixin(value = QIOCraftingWindow.class, remap = false)
public abstract class QIOCraftingWindowMixin {
    @Shadow(remap = false)
    @Final
    private IQIOCraftingWindowHolder holder;

    @WrapMethod(
            method = "performCraft(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;",
            remap = false)
    private ItemStack anotherStages$gatePerformCraft(
            Player player, ItemStack result, int amount, Operation<ItemStack> original) {
        if (AsConfig.mixinMekanism() && anotherStages$isRecipeBlocked(player)) {
            return ItemStack.EMPTY;
        }
        return original.call(player, result, amount);
    }

    @Unique
    private boolean anotherStages$isRecipeBlocked(Player player) {
        if (player == null) {
            return false;
        }
        RecipeHolder<CraftingRecipe> recipe = QIOLastRecipeJournalMixin.anotherStages$currentRecipe();
        if (recipe == null) {
            // Fallback: resolve via accessor interface applied to the journal instance through reflection-free cast
            // when ThreadLocal was not set yet (first craft after open).
            recipe = anotherStages$peekJournalRecipe();
        }
        if (recipe == null) {
            return false;
        }
        Level level = holder != null ? holder.getLevel() : player.level();
        return RecipeStageGate.isBlockedForPlayer(recipe.id(), player, level);
    }

    @Unique
    private RecipeHolder<CraftingRecipe> anotherStages$peekJournalRecipe() {
        try {
            Object journal = QIOCraftingWindow.class.getDeclaredField("lastRecipeJournal").get(this);
            if (journal instanceof QIOLastRecipeAccessor accessor) {
                return accessor.anotherStages$getRecipe();
            }
            var field = journal.getClass().getDeclaredField("recipe");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            RecipeHolder<CraftingRecipe> recipe = (RecipeHolder<CraftingRecipe>) field.get(journal);
            return recipe;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
