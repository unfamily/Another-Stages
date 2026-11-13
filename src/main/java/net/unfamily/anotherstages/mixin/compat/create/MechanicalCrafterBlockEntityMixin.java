package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.owner.MachineOwners;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Push {@link MachineOwners} UUID around the only {@code tryToApplyRecipe} call site
 * so unlock restores craft for the placing player.
 */
@Mixin(value = MechanicalCrafterBlockEntity.class, remap = false)
public abstract class MechanicalCrafterBlockEntityMixin {
    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler;tryToApplyRecipe(Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)Lnet/minecraft/world/item/ItemStack;",
                    remap = false),
            remap = false)
    private ItemStack anotherStages$withOwnerSubject(
            Level world, GroupedItems items, Operation<ItemStack> original) {
        MechanicalCrafterBlockEntity self = (MechanicalCrafterBlockEntity) (Object) this;
        CreateCrafterSubject.push(MachineOwners.get(self));
        try {
            return original.call(world, items);
        } finally {
            CreateCrafterSubject.clear();
        }
    }
}
