package net.unfamily.anotherstages.mixin.compat.ae2;

import appeng.blockentity.misc.ChargerBlockEntity;
import appeng.blockentity.misc.ChargerRecipes;
import appeng.recipes.handlers.ChargerRecipe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.ae2.Ae2CraftGate;
import net.unfamily.anotherstages.compat.ae2.Ae2NetworkOwners;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gate Charger recipe conversion in {@code doWork} only. {@code allowInsert} still uses
 * ungated {@link ChargerRecipes#findRecipe} so items can be inserted.
 */
@Mixin(value = ChargerBlockEntity.class, remap = false)
public abstract class ChargerBlockEntityMixin {
    @WrapOperation(
            method = "doWork",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/blockentity/misc/ChargerRecipes;findRecipe(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lappeng/recipes/handlers/ChargerRecipe;",
                    remap = false))
    private ChargerRecipe anotherStages$gateChargerWork(
            Level level,
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
                                    ((ChargerBlockEntity) (Object) this).getMainNode().getNode()),
                            level)) {
                return null;
            }
        }
        return recipe;
    }
}
