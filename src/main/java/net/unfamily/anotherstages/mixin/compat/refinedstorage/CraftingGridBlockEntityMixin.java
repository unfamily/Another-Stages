package net.unfamily.anotherstages.mixin.compat.refinedstorage;

import com.refinedmods.refinedstorage.common.grid.CraftingGridBlockEntity;
import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.compat.refinedstorage.RsMatrixOwners;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bind Crafting Grid matrix to placer UUID for stage-aware result updates. */
@Mixin(value = CraftingGridBlockEntity.class, remap = false)
public abstract class CraftingGridBlockEntityMixin {
    @Shadow(remap = false)
    @Final
    private RecipeMatrix<CraftingRecipe, CraftingInput> craftingRecipe;

    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("RETURN"))
    private void anotherStages$bindMatrixOwner(BlockPos pos, BlockState state, CallbackInfo ci) {
        RsMatrixOwners.bind(craftingRecipe, () -> ((RsPlacedByAccessor) this).anotherStages$getPlacedByPlayerId());
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void anotherStages$bindOnSetLevel(net.minecraft.world.level.Level level, CallbackInfo ci) {
        RsMatrixOwners.bind(craftingRecipe, () -> ((RsPlacedByAccessor) this).anotherStages$getPlacedByPlayerId());
    }
}
