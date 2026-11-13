package net.unfamily.anotherstages.mixin.compat.mekanism;

import mekanism.common.lib.multiblock.MultiblockData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.compat.mekanism.MekMultiblockSubjects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Capture the forming tile so stage gates can use {@code getOwnerUUID()}, not {@code inventoryID}.
 */
@Mixin(value = MultiblockData.class, remap = false)
public abstract class MultiblockDataMixin {
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void anotherStages$captureFormingTile(BlockEntity tile, CallbackInfo ci) {
        MekMultiblockSubjects.bind(this, tile);
    }
}
