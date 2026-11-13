package net.unfamily.anotherstages.mixin.compat.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridBlockEntity;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternType;
import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.compat.refinedstorage.RsCraftGate;
import net.unfamily.anotherstages.compat.refinedstorage.RsCraftSubjects;
import net.unfamily.anotherstages.compat.refinedstorage.RsMatrixOwners;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Bind Pattern Grid matrices to placer UUID; refuse encode with stage toast when locked.
 */
@Mixin(value = PatternGridBlockEntity.class, remap = false)
public abstract class PatternGridBlockEntityMixin {
    @Shadow(remap = false)
    @Final
    private RecipeMatrix<CraftingRecipe, CraftingInput> craftingRecipe;

    @Shadow(remap = false)
    @Final
    private RecipeMatrix<SmithingRecipe, SmithingRecipeInput> smithingTableRecipe;

    @Shadow(remap = false)
    public abstract PatternType getPatternType();

    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("RETURN"))
    private void anotherStages$bindMatrixOwners(BlockPos pos, BlockState state, CallbackInfo ci) {
        bindMatrices();
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void anotherStages$bindOnSetLevel(net.minecraft.world.level.Level level, CallbackInfo ci) {
        bindMatrices();
    }

    private UUID placedBy() {
        return ((RsPlacedByAccessor) this).anotherStages$getPlacedByPlayerId();
    }

    private void bindMatrices() {
        RsMatrixOwners.bind(craftingRecipe, this::placedBy);
        RsMatrixOwners.bind(smithingTableRecipe, this::placedBy);
    }

    @WrapMethod(method = "createPattern", remap = false)
    private void anotherStages$gateCreatePattern(Operation<Void> original) {
        if (!AsConfig.mixinRefinedStorage()) {
            original.call();
            return;
        }
        bindMatrices();
        RecipeHolder<?> holder = encodingHolder();
        if (holder == null) {
            original.call();
            return;
        }
        UUID owner = RsCraftSubjects.currentUuid();
        if (owner == null) {
            owner = placedBy();
        }
        PatternGridBlockEntity self = (PatternGridBlockEntity) (Object) this;
        if (RecipeStageGate.isBlockedForOwner(holder.id(), owner, self.getLevel())) {
            Identifier id = holder.id().identifier();
            Player player = RsCraftSubjects.currentPlayer();
            RsCraftGate.notifyBlocked(player, id);
            return;
        }
        original.call();
    }

    @Nullable
    private RecipeHolder<?> encodingHolder() {
        PatternType type = getPatternType();
        if (type == PatternType.CRAFTING && craftingRecipe != null) {
            ResultContainer result = craftingRecipe.getResult();
            return result != null ? result.getRecipeUsed() : null;
        }
        if (type == PatternType.SMITHING_TABLE && smithingTableRecipe != null) {
            ResultContainer result = smithingTableRecipe.getResult();
            return result != null ? result.getRecipeUsed() : null;
        }
        // Stonecutter uses a package-private container; gate via autocraft plan / world stages instead.
        return null;
    }
}
