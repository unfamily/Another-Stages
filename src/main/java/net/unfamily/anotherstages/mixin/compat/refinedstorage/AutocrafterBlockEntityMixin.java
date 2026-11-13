package net.unfamily.anotherstages.mixin.compat.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.refinedmods.refinedstorage.api.autocrafting.Pattern;
import com.refinedmods.refinedstorage.common.autocrafting.autocrafter.AutocrafterBlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.refinedstorage.RsCraftGate;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;

/**
 * RS Autocrafter safety net: skip INTERNAL pattern steps when the resolved recipe is stage-locked
 * for the placer UUID ({@code placedByPlayerId}).
 */
@Mixin(value = AutocrafterBlockEntity.class, remap = false)
public abstract class AutocrafterBlockEntityMixin {
    @WrapMethod(method = "canStep", remap = false)
    private boolean anotherStages$gateCanStep(Pattern pattern, Operation<Boolean> original) {
        if (!original.call(pattern)) {
            return false;
        }
        if (!AsConfig.mixinRefinedStorage() || pattern == null) {
            return true;
        }
        AutocrafterBlockEntity self = (AutocrafterBlockEntity) (Object) this;
        Level level = self.getLevel();
        Identifier blocked = RsCraftGate.findBlockedRecipe(
                pattern, ((RsPlacedByAccessor) this).anotherStages$getPlacedByPlayerId(), level);
        return blocked == null;
    }
}
