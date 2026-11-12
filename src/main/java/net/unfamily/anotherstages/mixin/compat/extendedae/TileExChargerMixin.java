package net.unfamily.anotherstages.mixin.compat.extendedae;

import appeng.blockentity.misc.ChargerRecipes;
import appeng.recipes.handlers.ChargerRecipe;
import com.glodblock.github.extendedae.common.tileentities.TileExCharger;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.compat.ae2.Ae2CraftGate;
import net.unfamily.anotherstages.compat.ae2.Ae2NetworkOwners;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Ex Charger uses the same AE2 charger recipe ids as vanilla Charger — gate process in {@code doWork} only.
 */
@Mixin(value = TileExCharger.class, remap = false)
public abstract class TileExChargerMixin {
    @WrapOperation(
            method = "doWork",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/blockentity/misc/ChargerRecipes;findRecipe(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lappeng/recipes/handlers/ChargerRecipe;",
                    remap = false))
    private ChargerRecipe anotherStages$gateExChargerWork(
            ServerLevel level,
            ItemStack input,
            Operation<ChargerRecipe> original) {
        ChargerRecipe recipe = original.call(level, input);
        if (!AsConfig.mixinAe2() || recipe == null) {
            return recipe;
        }
        for (RecipeHolder<ChargerRecipe> holder : ChargerRecipes.getRecipes(level)) {
            if (holder.value() == recipe
                    && Ae2CraftGate.isRecipeBlocked(
                            holder.id(),
                            Ae2NetworkOwners.fromNode(
                                    ((TileExCharger) (Object) this).getMainNode().getNode()),
                            level)) {
                return null;
            }
        }
        return recipe;
    }
}
