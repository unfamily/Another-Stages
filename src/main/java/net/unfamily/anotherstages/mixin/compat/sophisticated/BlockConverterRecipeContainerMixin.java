package net.unfamily.anotherstages.mixin.compat.sophisticated;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterRecipeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterUpgradeContainer;
import net.unfamily.anotherstages.compat.sophisticated.SophBlockConverterSubjects;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sophisticated block-converter upgrades (stonecutter and siblings): strip stage-locked
 * recipes from the internal {@code recipes} list after it is built (abstract
 * {@code filterAndSortRecipes} wrap does not hit concrete identity overrides), and keep
 * result / take paths from serving a locked recipe.
 */
@Mixin(value = BlockConverterRecipeContainer.class, remap = false)
public abstract class BlockConverterRecipeContainerMixin {
    @Shadow(remap = false)
    protected Level level;

    @Shadow(remap = false)
    @SuppressWarnings("rawtypes")
    private List recipes;

    @Shadow(remap = false)
    @Final
    private DataSlot selectedRecipe;

    @Shadow(remap = false)
    @Final
    private Slot outputSlot;

    @Shadow(remap = false)
    private void updateRecipeResultSlot() {}

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void anotherStages$capturePlayer(
            BlockConverterUpgradeContainer<?, ?, ?, ?> upgradeContainer,
            Consumer<Slot> addSlot,
            IServerUpdater serverUpdater,
            ContainerLevelAccess worldPosCallable,
            Level level,
            SoundEvent craftSound,
            CallbackInfo ci) {
        if (upgradeContainer != null) {
            Player player = ((UpgradeContainerBaseAccessor) (Object) upgradeContainer).anotherStages$getPlayer();
            SophBlockConverterSubjects.set(this, player);
        }
    }

    /**
     * After Soph assigns {@code recipes} (and may restore last selection into a locked
     * holder), replace the field with the unlocked subset and fix selection/output.
     */
    @Inject(method = "updateAvailableRecipes", at = @At("TAIL"), remap = false)
    private void anotherStages$filterRecipesInPlace(CallbackInfo ci) {
        if (!AsConfig.mixinSophisticated() || this.recipes == null || this.recipes.isEmpty()) {
            return;
        }
        Object previous = null;
        int sel = selectedRecipe != null ? selectedRecipe.get() : -1;
        if (sel >= 0 && sel < this.recipes.size()) {
            previous = this.recipes.get(sel);
        }
        @SuppressWarnings("unchecked")
        List<Object> filtered = (List<Object>) anotherStages$filterLocked(this.recipes);
        this.recipes = filtered;
        int newIndex = previous != null ? filtered.indexOf(previous) : -1;
        if (selectedRecipe != null) {
            selectedRecipe.set(newIndex);
        }
        if (newIndex < 0) {
            if (outputSlot != null) {
                outputSlot.set(ItemStack.EMPTY);
            }
        } else {
            updateRecipeResultSlot();
        }
    }

    @WrapMethod(method = "updateRecipeResultSlot", remap = false)
    private void anotherStages$gateResultSlot(Operation<Void> original) {
        if (!AsConfig.mixinSophisticated()) {
            original.call();
            return;
        }
        int sel = selectedRecipe != null ? selectedRecipe.get() : -1;
        if (sel >= 0 && this.recipes != null && sel < this.recipes.size()) {
            Object entry = this.recipes.get(sel);
            if (entry instanceof RecipeHolder<?> holder && anotherStages$isLocked(holder)) {
                if (outputSlot != null) {
                    outputSlot.set(ItemStack.EMPTY);
                }
                return;
            }
        }
        original.call();
    }

    @WrapMethod(method = "getRecipeList", remap = false)
    private List<?> anotherStages$filterRecipeList(Operation<List<?>> original) {
        List<?> recipes = original.call();
        if (!AsConfig.mixinSophisticated() || recipes == null || recipes.isEmpty()) {
            return recipes;
        }
        return anotherStages$filterLocked(recipes);
    }

    @WrapMethod(method = "selectRecipe", remap = false)
    private boolean anotherStages$gateSelectRecipe(int index, Operation<Boolean> original) {
        if (!AsConfig.mixinSophisticated()) {
            return original.call(index);
        }
        if (this.recipes == null || index < 0 || index >= this.recipes.size()) {
            return false;
        }
        Object holder = this.recipes.get(index);
        if (holder instanceof RecipeHolder<?> recipeHolder && anotherStages$isLocked(recipeHolder)) {
            return false;
        }
        return original.call(index);
    }

    @Unique
    private List<?> anotherStages$filterLocked(List<?> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        List<Object> filtered = new ArrayList<>(source.size());
        for (Object entry : source) {
            if (entry instanceof RecipeHolder<?> holder) {
                if (anotherStages$isLocked(holder)) {
                    continue;
                }
                filtered.add(holder);
            } else if (entry != null) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    @Unique
    private boolean anotherStages$isLocked(RecipeHolder<?> holder) {
        if (holder == null) {
            return false;
        }
        Player player = SophBlockConverterSubjects.get(this);
        if (player != null) {
            return RecipeStageGate.isBlockedForPlayer(holder.id(), player, level);
        }
        return RecipeStageGate.isBlockedForOwner(holder.id(), null, level);
    }
}
