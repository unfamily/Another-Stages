package net.unfamily.anotherstages.mixin.compat.createaddition;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mrh0.createaddition.blocks.rolling_mill.RollingMillBlockEntity;
import com.mrh0.createaddition.recipe.rolling.RollingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

/**
 * Create Crafts & Additions Rolling Mill: refuse stage-locked rolling recipes.
 */
@Mixin(value = RollingMillBlockEntity.class, remap = false)
public abstract class RollingMillBlockEntityMixin {
    @WrapMethod(
            method = "find(Lnet/neoforged/neoforge/items/wrapper/RecipeWrapper;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
            remap = false)
    private Optional<RecipeHolder<RollingRecipe>> anotherStages$gateFind(
            RecipeWrapper wrapper,
            Level level,
            Operation<Optional<RecipeHolder<RollingRecipe>>> original) {
        Optional<RecipeHolder<RollingRecipe>> recipe = original.call(wrapper, level);
        if (!AsConfig.mixinCreateAddition() || recipe.isEmpty()) {
            return recipe;
        }
        RollingMillBlockEntity self = (RollingMillBlockEntity) (Object) this;
        BlockEntity be = (BlockEntity) (Object) self;
        if (RecipeStageGate.isBlockedForOwner(recipe.get().id(), MachineOwners.get(be), level)) {
            return Optional.empty();
        }
        return recipe;
    }
}
