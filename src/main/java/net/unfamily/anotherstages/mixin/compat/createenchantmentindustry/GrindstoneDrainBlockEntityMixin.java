package net.unfamily.anotherstages.mixin.compat.createenchantmentindustry;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import plus.dragons.createenchantmentindustry.common.kinetics.grindstone.GrindstoneDrainBlockEntity;
import plus.dragons.createenchantmentindustry.common.kinetics.grindstone.GrindingRecipe;
import plus.dragons.createenchantmentindustry.common.registry.CEIRecipes;

/**
 * Create Enchantment Industry Grindstone Drain (disenchant / grinding fluid recipes).
 */
@Mixin(value = GrindstoneDrainBlockEntity.class, remap = false)
public abstract class GrindstoneDrainBlockEntityMixin {
    @Shadow(remap = false)
    public com.simibubi.create.content.processing.recipe.ProcessingInventory inventory;

    @WrapMethod(method = "applyRecipe", remap = false)
    private void anotherStages$gateApplyRecipe(Operation<Void> original) {
        if (!AsConfig.mixinCreateEnchantmentIndustry()) {
            original.call();
            return;
        }
        BlockEntity be = (BlockEntity) (Object) this;
        Level level = be.getLevel();
        if (level == null || level.isClientSide() || this.inventory == null) {
            original.call();
            return;
        }
        ItemStack stack = this.inventory.getStackInSlot(0);
        if (!stack.isEmpty()) {
            RecipeType<GrindingRecipe> type = CEIRecipes.GRINDING.getType();
            var found = level.getRecipeManager()
                    .getRecipeFor(type, new SingleRecipeInput(stack), level);
            if (found.isPresent()) {
                RecipeHolder<GrindingRecipe> holder = found.get();
                ResourceLocation id = holder.id();
                if (RecipeStageGate.isBlockedForOwner(id, MachineOwners.get(be), level)) {
                    return;
                }
            }
        }
        original.call();
    }
}
