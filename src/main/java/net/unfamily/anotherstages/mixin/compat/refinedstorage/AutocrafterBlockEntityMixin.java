package net.unfamily.anotherstages.mixin.compat.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.refinedmods.refinedstorage.api.autocrafting.Pattern;
import com.refinedmods.refinedstorage.api.autocrafting.PatternType;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.autocrafting.autocrafter.AutocrafterBlockEntity;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeIoProbe;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.UUID;

/**
 * RS Autocrafter: skip INTERNAL pattern steps when all crafting recipes that produce the
 * pattern's primary output item are stage-locked for the placer UUID.
 */
@Mixin(value = AutocrafterBlockEntity.class, remap = false)
public abstract class AutocrafterBlockEntityMixin {
    @Shadow(remap = false)
    private UUID placedByPlayerId;

    @WrapMethod(method = "canStep", remap = false)
    private boolean anotherStages$gateCanStep(Pattern pattern, Operation<Boolean> original) {
        if (!original.call(pattern)) {
            return false;
        }
        if (!AsConfig.mixinRefinedStorage() || pattern == null || pattern.layout() == null) {
            return true;
        }
        if (pattern.layout().type() != PatternType.INTERNAL) {
            return true;
        }
        AutocrafterBlockEntity self = (AutocrafterBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return true;
        }
        Item outputItem = primaryOutputItem(pattern.layout().outputs());
        if (outputItem == null) {
            return true;
        }
        String outputId = BuiltInRegistries.ITEM.getKey(outputItem).toString();
        boolean sawMatch = false;
        for (RecipeHolder<CraftingRecipe> holder :
                serverLevel.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!RecipeIoProbe.outputsMatch(holder.value(), List.of(outputId))) {
                continue;
            }
            sawMatch = true;
            if (!RecipeStageGate.isBlockedForOwner(holder.id(), placedByPlayerId, level)) {
                return true;
            }
        }
        return !sawMatch;
    }

    private static Item primaryOutputItem(List<ResourceAmount> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return null;
        }
        ResourceAmount amount = outputs.getFirst();
        if (amount == null || !(amount.resource() instanceof ItemResource itemResource)) {
            return null;
        }
        return itemResource.item();
    }
}
