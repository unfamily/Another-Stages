package net.unfamily.anotherstages.mixin.compat.ae2;

import appeng.menu.me.items.PatternEncodingTermMenu;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.ae2.Ae2CraftGate;
import net.unfamily.anotherstages.compat.ae2.Ae2NetworkOwners;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Refuse AE2 pattern encoding when the backing vanilla recipe is stage-locked for the encoding player.
 */
@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin {
    @Shadow(remap = false)
    private RecipeHolder<CraftingRecipe> currentRecipe;

    @WrapMethod(method = "encodeCraftingPattern", remap = false)
    private ItemStack anotherStages$gateEncodeCrafting(Operation<ItemStack> original) {
        PatternEncodingTermMenu self = (PatternEncodingTermMenu) (Object) this;
        Player player = self.getPlayer();
        if (AsConfig.mixinAe2()
                && currentRecipe != null
                && Ae2CraftGate.isRecipeBlocked(
                        currentRecipe.id(), Ae2NetworkOwners.fromPlayer(player), player.level())) {
            Ae2CraftGate.notifyBlocked(player, currentRecipe.id());
            return null;
        }
        return original.call();
    }

    @WrapMethod(method = "encodeStonecuttingPattern", remap = false)
    private ItemStack anotherStages$gateEncodeStonecutting(Operation<ItemStack> original) {
        PatternEncodingTermMenu self = (PatternEncodingTermMenu) (Object) this;
        Player player = self.getPlayer();
        ResourceKey<Recipe<?>> recipeKey = self.stonecuttingRecipeId;
        if (AsConfig.mixinAe2()
                && recipeKey != null
                && Ae2CraftGate.isRecipeBlocked(
                        recipeKey, Ae2NetworkOwners.fromPlayer(player), player.level())) {
            Ae2CraftGate.notifyBlocked(player, recipeKey);
            return null;
        }
        return original.call();
    }

    @WrapOperation(
            method = "encodeSmithingTablePattern",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/crafting/RecipeAccess;getRecipeFor(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;)Lnet/minecraft/world/item/crafting/RecipeHolder;",
                    remap = false))
    private <I extends RecipeInput, T extends Recipe<I>> RecipeHolder<T> anotherStages$gateEncodeSmithingRecipe(
            Level level,
            RecipeType<T> type,
            I input,
            Operation<RecipeHolder<T>> original) {
        RecipeHolder<T> recipe = original.call(level, type, input);
        PatternEncodingTermMenu self = (PatternEncodingTermMenu) (Object) this;
        Player player = self.getPlayer();
        if (AsConfig.mixinAe2()
                && recipe != null
                && Ae2CraftGate.isRecipeBlocked(
                        recipe.id(), Ae2NetworkOwners.fromPlayer(player), player.level())) {
            Ae2CraftGate.notifyBlocked(player, recipe.id());
            return null;
        }
        return recipe;
    }
}
