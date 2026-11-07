package net.unfamily.anotherstages.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.item.StageItemHandler.StageItemRule;

import java.util.List;

/**
 * Client helpers for recipe-viewer / inventory presentation of staged items.
 */
public final class StageItemRvHelper {
    public enum Mode {
        NONE,
        OVERLAY,
        PARTIAL,
        HIDE
    }

    private StageItemRvHelper() {}

    public static Mode modeFor(Item item) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null || item == null || !StageItemHandler.hasLoadedRestrictions()) {
            return Mode.NONE;
        }
        return modeFor(player, level, item);
    }

    public static Mode modeFor(Player player, Level level, Item item) {
        List<StageItemRule> active = StageItemHandler.findActiveRules(player, level, item);
        Mode strongest = Mode.NONE;
        for (StageItemRule rule : active) {
            Mode mode = switch (StageItemHandler.rvMode(rule)) {
                case "hide" -> Mode.HIDE;
                case "partial" -> Mode.PARTIAL;
                case "none" -> Mode.NONE;
                default -> Mode.OVERLAY;
            };
            if (rank(mode) > rank(strongest)) {
                strongest = mode;
            }
        }
        return strongest;
    }

    private static int rank(Mode mode) {
        return switch (mode) {
            case HIDE -> 3;
            case PARTIAL -> 2;
            case OVERLAY -> 1;
            case NONE -> 0;
        };
    }

    /** First stage id from an active rule (for tooltips). */
    public static String firstStageHint(Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || item == null) {
            return "";
        }
        for (StageItemRule rule : StageItemHandler.findActiveRules(mc.player, mc.level, item)) {
            if (rule.stages != null) {
                for (StageItemHandler.StageCondition c : rule.stages) {
                    if (c.stage != null && !c.stage.isBlank()) {
                        return c.stage;
                    }
                }
            }
        }
        return "";
    }
}
