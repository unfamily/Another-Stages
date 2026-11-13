package net.unfamily.anotherstages.mixin.compat.sophisticated;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoCookingUpgradeWrapper;
import net.unfamily.anotherstages.compat.refinedstorage.RsCraftSubjects;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Push wearing / nearby entity as craft subject while auto-cooking upgrade ticks.
 */
@Mixin(value = AutoCookingUpgradeWrapper.class, remap = false)
public abstract class AutoCookingUpgradeWrapperMixin {
    @WrapMethod(method = "tick", remap = false)
    private void anotherStages$withEntitySubject(
            Entity entity, Level level, BlockPos pos, Operation<Void> original) {
        if (entity instanceof Player player) {
            RsCraftSubjects.pushPlayer(player);
        } else if (entity != null) {
            RsCraftSubjects.pushUuid(entity.getUUID(), level);
        } else {
            RsCraftSubjects.pushUuid(null, level);
        }
        try {
            original.call(entity, level, pos);
        } finally {
            RsCraftSubjects.pop();
        }
    }
}
