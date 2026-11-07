package net.unfamily.anotherstages.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuMixin {
    @Shadow(remap = false)
    @Final
    private Level level;
    @Shadow(remap = false)
    @Final
    Slot resultSlot;
    @Shadow(remap = false)
    @Final
    ResultContainer resultContainer;

    @Unique
    private Player anotherStages$player;

    @Inject(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL"),
            remap = false)
    private void anotherStages$capturePlayer(int containerId, Inventory inv, ContainerLevelAccess access, CallbackInfo ci) {
        this.anotherStages$player = inv.player;
    }

    @Inject(method = "setupResultSlot", at = @At("TAIL"), remap = false)
    private void anotherStages$gateResult(int index, CallbackInfo ci) {
        if (!AsConfig.mixinStonecutter()) {
            return;
        }
        RecipeHolder<?> used = this.resultContainer.getRecipeUsed();
        if (used == null || anotherStages$player == null) {
            return;
        }
        if (RecipeStageGate.isBlockedForPlayer(used.id(), anotherStages$player, this.level)) {
            this.resultSlot.set(ItemStack.EMPTY);
            this.resultContainer.setRecipeUsed(null);
        }
    }
}
