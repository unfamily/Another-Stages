package net.unfamily.anotherstages.mixin.compat.sophisticated;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter.StonecutterRecipeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter.StonecutterUpgradeContainer;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/** Sophisticated stonecutter upgrade: refuse selecting a stage-locked stonecutting recipe. */
@Mixin(value = StonecutterRecipeContainer.class, remap = false)
public abstract class StonecutterRecipeContainerMixin {
    @Shadow(remap = false)
    protected Level level;

    @Shadow(remap = false)
    @Final
    private StonecutterUpgradeContainer upgradeContainer;

    @Shadow(remap = false)
    public abstract List<RecipeHolder<StonecutterRecipe>> getRecipeList();

    @WrapMethod(method = "selectRecipe", remap = false)
    private boolean anotherStages$gateSelectRecipe(int index, Operation<Boolean> original) {
        if (AsConfig.mixinSophisticated()) {
            List<RecipeHolder<StonecutterRecipe>> recipes = getRecipeList();
            if (recipes != null && index >= 0 && index < recipes.size()) {
                RecipeHolder<StonecutterRecipe> holder = recipes.get(index);
                if (holder != null) {
                    Player player = upgradeContainer != null ? upgradeContainer.getPlayer() : null;
                    if (RecipeStageGate.isBlockedForPlayer(holder.id(), player, level)
                            || (player == null
                                    && RecipeStageGate.isBlockedForOwner(holder.id(), null, level))) {
                        return false;
                    }
                }
            }
        }
        return original.call(index);
    }
}
