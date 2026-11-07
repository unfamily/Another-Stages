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
 * Applies optional custom tooltips, lock hints, and Unfamiliar Item renaming from stage-item rules.
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

        Component displayName = displayNameFor(stack, event.getToolTip().isEmpty()
                ? null
                : event.getToolTip().getFirst());

        if (hideDefault) {
            event.getToolTip().clear();
            event.getToolTip().add(displayName);
        } else if (!event.getToolTip().isEmpty()) {
            event.getToolTip().set(0, displayName);
        }

        for (StageItemHandler.StageItemRule rule : rules) {
            if (rule.tooltips != null) {
                for (String tip : rule.tooltips) {
                    if (tip == null || tip.isBlank()) {
                        continue;
                    }
                    event.getToolTip().add(resolveTooltip(tip.trim()));
                }
            }
            if (rule.componentTooltips != null) {
                for (Component tip : rule.componentTooltips) {
                    if (tip != null) {
                        event.getToolTip().add(tip);
                    }
                }
            }
        }

        if (event.getEntity() == null || event.getEntity().level() == null) {
            return;
        }
        var active = StageItemHandler.findActiveRules(
                event.getEntity(), event.getEntity().level(), stack.getItem());
        if (active.isEmpty()) {
            return;
        }

        StageItemRvHelper.Mode mode = StageItemRvHelper.modeFor(
                event.getEntity(), event.getEntity().level(), stack.getItem());
        if (mode == StageItemRvHelper.Mode.NONE || mode == StageItemRvHelper.Mode.HIDE) {
            return;
        }

        boolean anyCustom = false;
        for (StageItemHandler.StageItemRule rule : active) {
            if (StageItemRvHelper.hasCustomTooltips(rule)) {
                anyCustom = true;
                break;
            }
        }
        if (anyCustom) {
            return;
        }

        List<String> stages = StageItemRvHelper.stageHints(stack.getItem());
        if (stages.isEmpty()) {
            event.getToolTip().add(Component.translatable("tooltip.another_stages.item_locked"));
        } else {
            event.getToolTip().add(Component.translatable(
                    "tooltip.another_stages.item_locked_stage", String.join(", ", stages)));
        }
    }

    /**
     * Public for {@link net.unfamily.anotherstages.mixin.ItemStackHoverNameMixin}.
     */
    public static Component displayNameFor(ItemStack stack, Component fallbackName) {
        if (StageItemRvHelper.shouldHideItemName(stack.getItem())) {
            return StageItemRvHelper.unfamiliarName();
        }
        Component base = fallbackName != null ? fallbackName : stack.getItem().getName(stack);
        if (StageItemRvHelper.shouldDecorateRealName(stack.getItem())) {
            return StageItemRvHelper.decorateRealName(base);
        }
        return base;
    }

    private static Component resolveTooltip(String raw) {
        if (raw.indexOf(' ') < 0 && raw.indexOf('.') >= 0) {
            return Component.translatable(raw);
        }
        return Component.literal(raw);
    }
}
