package net.unfamily.anotherstages.mixin;

import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuResultAccessor {
    @Accessor(value = "resultSlots", remap = false)
    ResultContainer anotherStages$getResultSlots();
}
