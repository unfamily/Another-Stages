package net.unfamily.anotherstages.mixin.compat.refinedstorage;

import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(value = AbstractBaseNetworkNodeContainerBlockEntity.class, remap = false)
public interface RsPlacedByAccessor {
    @Accessor(value = "placedByPlayerId", remap = false)
    UUID anotherStages$getPlacedByPlayerId();
}
