package net.unfamily.anotherstages.compat.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.world.item.ItemStack;
import net.unfamily.anotherstages.item.AsItems;

/**
 * Always removes the generic hardcore placeholder from REI's ingredient list;
 * registers Stage Generator drag visitor.
 */
public final class AnotherStagesReiClientPlugin implements REIClientPlugin {
    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.removeEntryIf(stack -> {
            if (stack.getType() != VanillaEntryTypes.ITEM) {
                return false;
            }
            EntryStack<ItemStack> item = stack.cast();
            return AsItems.isHardcorePlaceholder(item.getValue().getItem());
        });
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerDraggableStackVisitor(new StageGenReiDragVisitor());
    }
}
