package net.unfamily.anotherstages.mixin.compat.fastfurnace;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * FastFurnace merges {@code getRecipe(ServerLevel)} onto {@link AbstractFurnaceBlockEntity}
 * and redirects {@code serverTick} / {@code getTotalCookTime} through it. Wrap that merged
 * method (not FastFurnace's mixin class) so stage-locked cooks never start.
 */
@Mixin(value = AbstractFurnaceBlockEntity.class, priority = 1100)
public abstract class FastFurnaceRecipeMixin {
    /** FastFurnace {@code @Unique} field merged onto the furnace BE. */
    @Shadow(remap = false)
    protected RecipeHolder<?> curRecipe;

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
                    "FastFurnace gate recipe={} blocked={}", holder.id(), blocked);
        }
        if (!blocked) {
            return holder;
        }
        // Do not sticky-poison failedMatch: stage unlock must re-evaluate next tick without
        // requiring the input stack to change.
        this.curRecipe = null;
        return null;
    }
}
