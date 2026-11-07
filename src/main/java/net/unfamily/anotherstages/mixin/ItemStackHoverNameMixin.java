package net.unfamily.anotherstages.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.unfamily.anotherstages.item.StageItemHandler;
import net.unfamily.anotherstages.item.StageItemRvHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-only Unfamiliar Item/Fish rename for hotbar / JEI when {@code hide_item_name} is active.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackHoverNameMixin {
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true, remap = false)
    private void anotherStages$unfamiliarName(CallbackInfoReturnable<Component> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.level.isClientSide()) {
            return;
        }
        if (!StageItemHandler.hasLoadedRestrictions()) {
            return;
        }
        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty() || !StageItemRvHelper.shouldHideItemName(self.getItem())) {
            return;
        }
        cir.setReturnValue(StageItemRvHelper.hiddenDisplayName(self.getItem()));
    }
}
