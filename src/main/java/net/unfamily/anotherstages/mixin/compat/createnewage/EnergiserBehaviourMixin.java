package net.unfamily.anotherstages.mixin.compat.createnewage;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.compat.create.CreateRecipeIds;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.antarcticgardens.cna.content.energising.EnergiserBehaviour;
import org.antarcticgardens.cna.content.energising.EnergiserBlockEntity;
import org.antarcticgardens.cna.content.energising.recipe.EnergisingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Create New Age Energiser: refuse stage-locked energising recipes.
 */
@Mixin(value = EnergiserBehaviour.class, remap = false)
public abstract class EnergiserBehaviourMixin {
    @Shadow(remap = false)
    protected EnergiserBlockEntity be;

    @WrapMethod(
            method = "getRecipe(Lnet/minecraft/world/item/ItemStack;)Lorg/antarcticgardens/cna/content/energising/recipe/EnergisingRecipe;",
            remap = false)
    private EnergisingRecipe anotherStages$gateGetRecipe(
            ItemStack stack, Operation<EnergisingRecipe> original) {
        EnergisingRecipe recipe = original.call(stack);
        if (!AsConfig.mixinCreateNewAge() || recipe == null) {
            return recipe;
        }
        BlockEntity blockEntity = (BlockEntity) (Object) this.be;
        Level level = blockEntity != null ? blockEntity.getLevel() : null;
        ResourceLocation id = CreateRecipeIds.idOf(recipe, level);
        if (id != null
                && RecipeStageGate.isBlockedForOwner(id, MachineOwners.get(blockEntity), level)) {
            return null;
        }
        return recipe;
    }
}
