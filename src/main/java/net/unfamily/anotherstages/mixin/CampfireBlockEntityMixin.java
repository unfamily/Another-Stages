package net.unfamily.anotherstages.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.config.AsConfig;
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
    @Inject(
            method = "placeFood(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;I)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void anotherStages$gatePlaceFood(
            @Nullable LivingEntity entity,
            ItemStack food,
            int cookTime,
            CallbackInfoReturnable<Boolean> cir) {
        if (!AsConfig.mixinCampfire()) {
            return;
        }
        CampfireBlockEntity self = (CampfireBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || food.isEmpty()) {
            return;
        }
        Optional<RecipeHolder<CampfireCookingRecipe>> recipe =
                RecipeManager.createCheck(net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING)
                        .getRecipeFor(new SingleRecipeInput(food), level);
        if (recipe.isPresent()
                && RecipeStageGate.isBlockedForMachine(recipe.get().id(), self, level)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = "cookTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;)V",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeManager$CachedCheck;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
                    remap = false))
    private static <I extends net.minecraft.world.item.crafting.RecipeInput, T extends net.minecraft.world.item.crafting.Recipe<I>>
            Optional<RecipeHolder<T>> anotherStages$gateCookTick(
                    RecipeManager.CachedCheck<I, T> check,
                    I input,
                    Level level,
                    Level levelArg,
                    BlockPos pos,
                    BlockState state,
                    CampfireBlockEntity entity) {
        Optional<RecipeHolder<T>> recipe = check.getRecipeFor(input, level);
        if (AsConfig.mixinCampfire()
                && recipe.isPresent()
                && RecipeStageGate.isBlockedForMachine(recipe.get().id(), entity, level)) {
            return Optional.empty();
        }
        return recipe;
    }
}
