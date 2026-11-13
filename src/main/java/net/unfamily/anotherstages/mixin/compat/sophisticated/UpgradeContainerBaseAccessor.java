package net.unfamily.anotherstages.mixin.compat.sophisticated;

import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = UpgradeContainerBase.class, remap = false)
public interface UpgradeContainerBaseAccessor {
    @Accessor(value = "player", remap = false)
    Player anotherStages$getPlayer();
}
