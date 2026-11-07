package net.unfamily.anotherstages.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {
    @Redirect(
            method = "dispenseFrom",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/CrafterBlock;getPotentialResults(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/crafting/CraftingInput;)Ljava/util/Optional;",
                    remap = false))
    private Optional<RecipeHolder<CraftingRecipe>> anotherStages$gateCrafter(
            ServerLevel level,
            net.minecraft.world.item.crafting.CraftingInput input,
            BlockState state,
            ServerLevel levelArg,
            BlockPos pos) {
        Optional<RecipeHolder<CraftingRecipe>> recipe = CrafterBlock.getPotentialResults(level, input);
        if (recipe.isEmpty()) {
            return recipe;
        }
        if (!(level.getBlockEntity(pos) instanceof CrafterBlockEntity be)) {
            return recipe;
        }
        if (RecipeStageGate.isBlockedForMachine(recipe.get().id(), be, level)) {
            return Optional.empty();
        }
        return recipe;
    }
}
