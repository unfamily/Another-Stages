package net.unfamily.anotherstages.mixin.compat.enderio;

import com.enderio.enderio.content.machines.alloy.AlloySmelterBlockEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.compat.enderio.EioStageRearm;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

/**
 * Ender IO Alloy Smelter: refuse starting a stage-locked alloy / remapped-smelting recipe.
 * Outer BE via {@code this$0} typed as {@link AlloySmelterBlockEntity} (must match bytecode field).
 * Cast through {@link Object} for {@link BlockEntity} APIs — EIO parent classes are incomplete on compileOnly flat.
 * Dual-id / parent {@code minecraft:smelting} matching lives in {@code StageRecipeHandler}.
 * Marks {@link EioStageRearm} so {@code MachineTaskHost} re-arms after a gated empty lookup.
 */
@Pseudo
@Mixin(
        targets = "com.enderio.enderio.content.machines.alloy.AlloySmelterBlockEntity$AlloySmeltingMachineTaskHost",
        remap = false)
public abstract class AlloySmeltingMachineTaskHostMixin {
    @Shadow(remap = false)
    @Final
    AlloySmelterBlockEntity this$0;

    @WrapMethod(method = "findRecipe", remap = false)
    private Optional<RecipeHolder<?>> anotherStages$gateFindRecipe(Operation<Optional<RecipeHolder<?>>> original) {
        Optional<RecipeHolder<?>> recipe = original.call();
        if (!AsConfig.mixinEnderio() || recipe.isEmpty()) {
            return recipe;
        }
        RecipeHolder<?> holder = recipe.get();
        BlockEntity be = (BlockEntity) (Object) this.this$0;
        Level level = be != null ? be.getLevel() : null;
        if (RecipeStageGate.isBlockedForMachine(holder.id(), be, level)) {
            EioStageRearm.mark(this);
            return Optional.empty();
        }
        return recipe;
    }
}
