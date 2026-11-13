package net.unfamily.anotherstages.mixin.compat.createenchantmentindustry;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.anotherstages.compat.createenchantmentindustry.CeiPrinterSubject;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import plus.dragons.createenchantmentindustry.common.fluids.printer.PrintingRecipe;
import plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour.RecipePrintingBehaviour;

import java.util.Optional;

/**
 * Create Enchantment Industry Printer: refuse stage-locked printing recipes.
 * Owner from {@link CeiPrinterSubject} (set by {@code PrinterBlockEntityMixin}).
 */
@Mixin(value = RecipePrintingBehaviour.class, remap = false)
public abstract class RecipePrintingBehaviourMixin {
    @Shadow(remap = false)
    private RecipeHolder<PrintingRecipe> lastRecipe;

    @WrapMethod(
            method = "findRecipe(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/FluidStack;)Ljava/util/Optional;",
            remap = false)
    private Optional<PrintingRecipe> anotherStages$gateFind(
            Level level,
            ItemStack stack,
            FluidStack fluid,
            Operation<Optional<PrintingRecipe>> original) {
        Optional<PrintingRecipe> recipe = original.call(level, stack, fluid);
        if (!AsConfig.mixinCreateEnchantmentIndustry() || recipe.isEmpty()) {
            return recipe;
        }
        if (this.lastRecipe != null
                && RecipeStageGate.isBlockedForOwner(
                        this.lastRecipe.id(), CeiPrinterSubject.current(), level)) {
            return Optional.empty();
        }
        return recipe;
    }
}
