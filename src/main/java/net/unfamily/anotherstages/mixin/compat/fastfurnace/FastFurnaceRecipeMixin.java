package net.unfamily.anotherstages.mixin.compat.fastfurnace;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.shadowsoffire.fastfurnace.mixin.MixinAbstractFurnaceBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

/**
 * FastFurnace redirects furnace {@code CachedCheck#getRecipeFor} to {@code getRecipe}.
 * Wrap that method so a stage-locked cook never starts (no owner still evaluates stage rules).
 */
@Mixin(value = MixinAbstractFurnaceBlockEntity.class, priority = 1100)
public abstract class FastFurnaceRecipeMixin {
    @WrapMethod(
            method = "getRecipe(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/item/crafting/RecipeHolder;",
            remap = false)
    private RecipeHolder<?> anotherStages$gateFurnaceRecipe(
            ServerLevel level, Operation<RecipeHolder<?>> original) {
        RecipeHolder<?> holder = original.call(level);
        if (AsConfig.mixinFastfurnace()
                && holder != null
                && RecipeStageGate.isBlockedForMachine(holder.id(), (BlockEntity) (Object) this, level)) {
            return null;
        }
        return holder;
    }
}
