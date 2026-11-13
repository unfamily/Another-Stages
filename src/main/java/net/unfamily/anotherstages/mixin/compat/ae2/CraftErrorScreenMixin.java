package net.unfamily.anotherstages.mixin.compat.ae2;

import appeng.api.networking.crafting.CraftingSubmitErrorCode;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import appeng.client.gui.me.crafting.CraftErrorScreen;
import net.minecraft.network.chat.Component;
import net.unfamily.anotherstages.client.Ae2CraftErrorClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replace AE2 incomplete-plan dialog text when AS marked the submit as stage-blocked.
 * Uses late apply so S2C can arrive after {@code <init>}.
 */
@Mixin(value = CraftErrorScreen.class, remap = false)
public abstract class CraftErrorScreenMixin {
    @Unique
    private CraftingSubmitErrorCode anotherStages$errorCode;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void anotherStages$captureAndApply(
            CraftConfirmScreen parent, CraftingSubmitErrorCode errorCode, Object details, CallbackInfo ci) {
        anotherStages$errorCode = errorCode;
        anotherStages$tryApplyStageText();
    }

    @Inject(method = "updateBeforeRender", at = @At("HEAD"), remap = false)
    private void anotherStages$lateApply(CallbackInfo ci) {
        anotherStages$tryApplyStageText();
    }

    @Unique
    private void anotherStages$tryApplyStageText() {
        if (anotherStages$errorCode != CraftingSubmitErrorCode.INCOMPLETE_PLAN) {
            return;
        }
        if (!Ae2CraftErrorClient.isPending()) {
            return;
        }
        String recipeId = Ae2CraftErrorClient.peekRecipeId();
        Component text = recipeId == null || recipeId.isEmpty()
                ? Component.translatable("another_stages.ae2.craft_blocked")
                : Component.translatable("another_stages.ae2.craft_blocked_recipe", recipeId);
        ((AeBaseScreenTextInvoker) this).anotherStages$invokeSetTextContent("errorText", text);
        Ae2CraftErrorClient.markApplied();
    }
}
