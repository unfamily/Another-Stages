package net.unfamily.anotherstages.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.ItemStack;
import net.unfamily.anotherstages.client.gui.stagegen.StageGeneratorScreen;
import net.unfamily.anotherstages.item.AsItems;

/**
 * Always removes the generic hardcore placeholder from EMI's ingredient list;
 * registers Stage Generator drag-drop.
 */
@EmiEntrypoint
public final class AnotherStagesEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.removeEmiStacks(EmiStack.of(new ItemStack(AsItems.STAGE_LOCKED_HARDCORE.get())));
        registry.addDragDropHandler(StageGeneratorScreen.class, new StageGenEmiDragDropHandler());
    }
}
