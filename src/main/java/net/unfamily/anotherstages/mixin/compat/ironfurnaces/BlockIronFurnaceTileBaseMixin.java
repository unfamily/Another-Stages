package net.unfamily.anotherstages.mixin.compat.ironfurnaces;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

/**
 * Iron Furnaces uses its own BE (not {@code AbstractFurnaceBlockEntity}).
 * Gate cook start via {@code getRecipeNonCached}; {@code hasRecipe} stays ungated so insert stays OK.
 * Native {@code owner} UUID is used as-is (no conversion).
 */
@Mixin(value = BlockIronFurnaceTileBase.class, remap = false)
public abstract class BlockIronFurnaceTileBaseMixin {
    @Shadow(remap = false)
    public UUID owner;

    @WrapMethod(method = "getRecipeNonCached", remap = false)
    private RecipeHolder<? extends AbstractCookingRecipe> anotherStages$gateIronFurnaceRecipe(
            ItemStack stack, Operation<RecipeHolder<? extends AbstractCookingRecipe>> original) {
        RecipeHolder<? extends AbstractCookingRecipe> holder = original.call(stack);
        if (!AsConfig.mixinIronFurnaces() || holder == null) {
            return holder;
        }
        BlockIronFurnaceTileBase self = (BlockIronFurnaceTileBase) (Object) this;
        boolean blocked = RecipeStageGate.isBlockedForOwner(holder.id(), owner, self.getLevel());
        if (AsConfig.loggingEnabled()) {
            AnotherStages.LOGGER.info(
                    "IronFurnaces gate recipe={} blocked={}", holder.id(), blocked);
        }
        return blocked ? null : holder;
    }
}
