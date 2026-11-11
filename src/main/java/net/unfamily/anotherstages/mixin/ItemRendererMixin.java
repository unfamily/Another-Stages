package net.unfamily.anotherstages.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.client.StageItemClientSetup;
import net.unfamily.anotherstages.item.StageItemHandler;
import net.unfamily.anotherstages.item.StageItemRvHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hardcore RV (1.21.1): replace the item's baked model with the flat lock/fish texture
 * everywhere (GUI, hand, item frames, ground entities).
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true, remap = false)
    private void anotherStages$hardcoreModel(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int seed,
            CallbackInfoReturnable<BakedModel> cir) {
        if (stack.isEmpty() || !StageItemHandler.hasLoadedRestrictions()) {
            return;
        }
        if (StageItemRvHelper.modeFor(stack.getItem()) != StageItemRvHelper.Mode.HARDCORE) {
            return;
        }
        BakedModel hardcore = Minecraft.getInstance()
                .getModelManager()
                .getModel(StageItemClientSetup.hardcoreStandaloneModel(StageItemRvHelper.hardcoreItemModelId()));
        if (hardcore != null) {
            cir.setReturnValue(hardcore);
        }
    }
}
