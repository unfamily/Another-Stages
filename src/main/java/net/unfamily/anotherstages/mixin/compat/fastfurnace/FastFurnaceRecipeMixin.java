package net.unfamily.anotherstages.mixin.compat.fastfurnace;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * FastFurnace (1.21.1): merges {@code getRecipe()} (no ServerLevel) onto the furnace BE.
 * Previous WrapMethod targeted getRecipe(ServerLevel) and never applied — cook gates were dead.
 */
@Mixin(value = AbstractFurnaceBlockEntity.class, priority = 1100)
public abstract class FastFurnaceRecipeMixin {
    @Shadow(remap = false)
    protected RecipeHolder<?> curRecipe;

    @WrapMethod(method = "getRecipe()Lnet/minecraft/world/item/crafting/RecipeHolder;", remap = false)
    private RecipeHolder<?> anotherStages$gateFurnaceRecipe(Operation<RecipeHolder<?>> original) {
        RecipeHolder<?> holder = original.call();
        if (!AsConfig.mixinFastfurnace() || holder == null) {
            return holder;
        }
        BlockEntity be = (BlockEntity) (Object) this;
        Level level = be.getLevel();
        boolean blocked = RecipeStageGate.isBlockedForMachine(holder.id(), be, level);
        if (AsConfig.loggingEnabled()) {
            AnotherStages.LOGGER.info(
                    "FastFurnace gate recipe={} blocked={}", holder.id(), blocked);
        }
        if (!blocked) {
            return holder;
        }
        this.curRecipe = null;
        return null;
    }
}
