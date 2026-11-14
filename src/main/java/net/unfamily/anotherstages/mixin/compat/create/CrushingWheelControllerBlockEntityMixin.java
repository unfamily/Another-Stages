package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;
import java.util.UUID;

/**
 * Create Crushing Wheels: while stage-locked, skip crush but <strong>do not</strong> clear inventory
 * (Create's empty-{@code findRecipe} path would destroy the stack). Create then latches
 * {@code appliedRecipe} and ejects the <em>unchanged</em> input — pass-through, no jam.
 * <p>
 * After stage grant, the next insert crushes normally. Controller is auto-placed; claim owner
 * from ingested {@link ItemEntity} thrower or nearest player so player-stage unlocks resolve.
 */
@Mixin(value = CrushingWheelControllerBlockEntity.class, remap = false)
public abstract class CrushingWheelControllerBlockEntityMixin {
    @Shadow(remap = false)
    public abstract Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> findRecipe();

    @WrapMethod(method = "applyRecipe", remap = false)
    private void anotherStages$gateApplyRecipe(Operation<Void> original) {
        if (!AsConfig.mixinCreate()) {
            original.call();
            return;
        }
        CrushingWheelControllerBlockEntity self = (CrushingWheelControllerBlockEntity) (Object) this;
        Level level = ((BlockEntity) (Object) self).getLevel();
        Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> recipe = findRecipe();
        if (recipe.isPresent()
                && RecipeStageGate.isBlockedForOwner(
                        recipe.get().id(), anotherStages$subject(self, level), level)) {
            // Leave input in slot 0. Tick sets appliedRecipe=true and ejects uncrushed — pass-through.
            return;
        }
        original.call();
    }

    @WrapMethod(
            method = "intakeItem(Lnet/minecraft/world/entity/item/ItemEntity;)V",
            remap = false)
    private void anotherStages$claimOnIntake(ItemEntity itemEntity, Operation<Void> original) {
        if (AsConfig.mixinCreate() && itemEntity != null) {
            CrushingWheelControllerBlockEntity self = (CrushingWheelControllerBlockEntity) (Object) this;
            Entity thrower = itemEntity.getOwner();
            if (thrower != null) {
                MachineOwners.claimIfAbsent(self, thrower.getUUID());
            }
        }
        original.call(itemEntity);
    }

    /**
     * Controller is auto-placed — prefer stored owner, else claim nearest player once so
     * {@code is: false} player-stage rules can unlock on the next pass.
     */
    @Unique
    @Nullable
    private static UUID anotherStages$subject(CrushingWheelControllerBlockEntity self, @Nullable Level level) {
        UUID owned = MachineOwners.get(self);
        if (owned != null) {
            return owned;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        BlockPos pos = ((BlockEntity) self).getBlockPos();
        if (!(serverLevel.getNearestPlayer(
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12.0, false)
                instanceof ServerPlayer nearest)) {
            return null;
        }
        MachineOwners.claimIfAbsent(self, nearest.getUUID());
        return MachineOwners.get(self);
    }
}
