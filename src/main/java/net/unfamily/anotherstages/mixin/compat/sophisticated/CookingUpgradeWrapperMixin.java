package net.unfamily.anotherstages.mixin.compat.sophisticated;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeWrapper;
import net.unfamily.anotherstages.compat.refinedstorage.RsCraftSubjects;
import net.unfamily.anotherstages.owner.MachineOwners;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

/**
 * Push wearing / nearby entity as craft subject while cooking upgrade ticks.
 * Storage with no entity uses {@link MachineOwners} on the block at {@code pos}.
 */
@Mixin(value = CookingUpgradeWrapper.class, remap = false)
public abstract class CookingUpgradeWrapperMixin {
    @WrapMethod(method = "tick", remap = false)
    private void anotherStages$withEntitySubject(
            Entity entity, Level level, BlockPos pos, Operation<Void> original) {
        if (entity instanceof Player player) {
            RsCraftSubjects.pushPlayer(player);
        } else if (entity != null) {
            RsCraftSubjects.pushUuid(entity.getUUID(), level);
        } else {
            UUID owner = null;
            if (level != null && pos != null) {
                BlockEntity be = level.getBlockEntity(pos);
                owner = MachineOwners.get(be);
            }
            RsCraftSubjects.pushUuid(owner, level);
        }
        try {
            original.call(entity, level, pos);
        } finally {
            RsCraftSubjects.pop();
        }
    }
}
