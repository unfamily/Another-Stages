package net.unfamily.anotherstages.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.unfamily.anotherstages.AnotherStages;

import java.util.List;

/**
 * Applies optional custom tooltips from stage-item rules.
 * Matching is by item id/tag/mod only (stage gates are enforced server-side).
 */
@EventBusSubscriber(modid = AnotherStages.MOD_ID, value = Dist.CLIENT)
public final class StageItemClientEvents {
    private StageItemClientEvents() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !StageItemHandler.hasLoadedRestrictions()) {
            return;
        }

        List<StageItemHandler.StageItemRule> rules = StageItemHandler.findRulesForItem(stack.getItem());
        if (rules.isEmpty()) {
            return;
        }

        boolean hideDefault = false;
        for (StageItemHandler.StageItemRule rule : rules) {
            if (rule.hideDefaultTooltips) {
                hideDefault = true;
                break;
            }
        }
        if (hideDefault) {
            event.getToolTip().clear();
            event.getToolTip().add(stack.getHoverName());
        }

        for (StageItemHandler.StageItemRule rule : rules) {
            if (rule.tooltips == null || rule.tooltips.isEmpty()) {
                continue;
            }
            for (String tip : rule.tooltips) {
                if (tip == null || tip.isBlank()) {
                    continue;
                }
                event.getToolTip().add(resolveTooltip(tip.trim()));
            }
        }
    }

    private static Component resolveTooltip(String raw) {
        // Prefer lang keys (dot-separated, no spaces); otherwise treat as literal text.
        if (raw.indexOf(' ') < 0 && raw.indexOf('.') >= 0) {
            return Component.translatable(raw);
        }
        return Component.literal(raw);
    }
}
