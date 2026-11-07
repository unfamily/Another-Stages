package net.unfamily.anotherstages.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {
    @Inject(method = "placeFood", at = @At("HEAD"), cancellable = true, remap = false)
    private void anotherStages$gatePlaceFood(
            ServerLevel serverLevel,
            @Nullable LivingEntity sourceEntity,
            ItemStack placeItem,
            CallbackInfoReturnable<Boolean> cir) {
        Optional<RecipeHolder<CampfireCookingRecipe>> recipe = serverLevel
                .recipeAccess()
                .getRecipeFor(
                        net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING,
                        new SingleRecipeInput(placeItem),
                        serverLevel);
        if (recipe.isPresent()
                && RecipeStageGate.isBlockedForMachine(
                        recipe.get().id(), (CampfireBlockEntity) (Object) this, serverLevel)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = "cookTick",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeManager$CachedCheck;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/server/level/ServerLevel;)Ljava/util/Optional;",
                    remap = false))
    private static <I extends net.minecraft.world.item.crafting.RecipeInput, T extends net.minecraft.world.item.crafting.Recipe<I>>
            Optional<RecipeHolder<T>> anotherStages$gateCookTick(
                    RecipeManager.CachedCheck<I, T> check,
                    I input,
                    ServerLevel level,
                    ServerLevel levelArg,
                    BlockPos pos,
                    BlockState state,
                    CampfireBlockEntity entity,
                    RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipeCache) {
        Optional<RecipeHolder<T>> recipe = check.getRecipeFor(input, level);
        if (recipe.isPresent() && RecipeStageGate.isBlockedForMachine(recipe.get().id(), entity, level)) {
            return Optional.empty();
        }
        return recipe;
    }
}
