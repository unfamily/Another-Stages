package net.unfamily.anotherstages.mixin.compat.fastfurnace;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.shadowsoffire.fastfurnace.mixin.MixinAbstractFurnaceBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * FastFurnace redirects furnace {@code CachedCheck#getRecipeFor} to {@code getRecipe}.
 * Wrap that method so a stage-locked cook never starts (no owner still evaluates stage rules).
 * Clears FastFurnace's sticky {@code curRecipe} / {@code failedMatch} cache when blocked.
 */
@Mixin(value = MixinAbstractFurnaceBlockEntity.class, priority = 1100)
public abstract class FastFurnaceRecipeMixin {
    @Shadow(remap = false)
    protected RecipeHolder<?> curRecipe;

    @Shadow(remap = false)
    protected ItemStack failedMatch;

    @WrapMethod(
            method = "getRecipe(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/item/crafting/RecipeHolder;",
            remap = false)
    private RecipeHolder<?> anotherStages$gateFurnaceRecipe(
            ServerLevel level, Operation<RecipeHolder<?>> original) {
        RecipeHolder<?> holder = original.call(level);
        if (!AsConfig.mixinFastfurnace() || holder == null) {
            return holder;
        }
        BlockEntity be = (BlockEntity) (Object) this;
        boolean blocked = RecipeStageGate.isBlockedForMachine(holder.id(), be, level);
        if (AsConfig.loggingEnabled()) {
            AnotherStages.LOGGER.info(
                    "FastFurnace gate recipe={} blocked={}", holder.id().identifier(), blocked);
        }
        if (!blocked) {
            return holder;
        }
        // Sticky cache would keep returning the locked holder on the next early-out match path.
        this.curRecipe = null;
        ItemStack input = be instanceof Container container ? container.getItem(0) : ItemStack.EMPTY;
        this.failedMatch = input.isEmpty() ? ItemStack.EMPTY : input.copy();
        return null;
    }
}
