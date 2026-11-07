package net.unfamily.anotherstages.mixin;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.item.StageItemHandler;
import net.unfamily.anotherstages.item.StageItemRvHelper;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Hardcore RV: replace the item's baked model with the flat lock/fish texture everywhere
 * (GUI, hand, item frames, pedestals, ground entities).
 */
@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
    @ModifyVariable(
            method = "appendItemLayers",
            at = @At("STORE"),
            ordinal = 0,
            remap = false)
    private Identifier anotherStages$hardcoreModel(
            Identifier modelId,
            ItemStackRenderState output,
            ItemStack item,
            ItemDisplayContext displayContext,
            @Nullable Level level,
            @Nullable ItemOwner owner,
            int seed) {
        if (modelId == null || item.isEmpty() || !StageItemHandler.hasLoadedRestrictions()) {
            return modelId;
        }
        if (StageItemRvHelper.modeFor(item.getItem()) != StageItemRvHelper.Mode.HARDCORE) {
            return modelId;
        }
        return StageItemRvHelper.hardcoreItemModelId();
    }
}
