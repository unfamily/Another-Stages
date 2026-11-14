package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/**
 * Create sandpaper hand polish: refuse stage-locked {@code create:sandpaper_polishing} for the player.
 * Deployer path is already gated via {@code DeployerBlockEntityMixin}.
 */
@Mixin(value = SandPaperItem.class, remap = false)
public abstract class SandPaperItemMixin {
    @WrapMethod(method = "finishUsingItem", remap = false)
    private ItemStack anotherStages$gatePolish(
            ItemStack stack, Level level, LivingEntity entityLiving, Operation<ItemStack> original) {
        if (!AsConfig.mixinCreate()
                || !(entityLiving instanceof Player player)
                || !stack.has(AllDataComponents.SAND_PAPER_POLISHING)) {
            return original.call(stack, level, entityLiving);
        }
        ItemStack toPolish = stack.get(AllDataComponents.SAND_PAPER_POLISHING).item();
        if (toPolish == null || toPolish.isEmpty()) {
            return original.call(stack, level, entityLiving);
        }
        List<RecipeHolder<Recipe<SingleRecipeInput>>> matching =
                SandPaperPolishingRecipe.getMatchingRecipes(level, toPolish);
        if (!matching.isEmpty()
                && RecipeStageGate.isBlockedForPlayer(matching.get(0).id(), player, level)) {
            if (!level.isClientSide()) {
                player.getInventory().placeItemBackInInventory(toPolish);
                stack.remove(AllDataComponents.SAND_PAPER_POLISHING);
            }
            return stack;
        }
        return original.call(stack, level, entityLiving);
    }
}
