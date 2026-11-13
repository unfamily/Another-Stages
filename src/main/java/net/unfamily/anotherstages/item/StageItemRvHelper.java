package net.unfamily.anotherstages.item;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.client.StageAprilFools;
import net.unfamily.anotherstages.item.StageItemHandler.StageItemRule;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Client helpers for recipe-viewer / inventory presentation of staged items.
 * Modes: {@code overlay} (default), {@code hardcore}, {@code hide}.
 */
public final class StageItemRvHelper {
    public enum Mode {
        NONE,
        OVERLAY,
        HARDCORE,
        HIDE
    }

    /** Overlay badge: 24×24 lock under {@code textures/stage_locking/}. */
    public static final ResourceLocation LOCK_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(AnotherStages.MOD_ID, "textures/stage_locking/stage_locked.png");
    public static final int LOCK_OVERLAY_SIZE = 24;
    public static final ResourceLocation HARDCORE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AnotherStages.MOD_ID, "textures/stage_locking/stage_locked_hardcore.png");
    /** Overlay badge: 32×32 so the fish art stays readable vs the 24×24 lock. */
    public static final ResourceLocation FISH_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(AnotherStages.MOD_ID, "textures/stage_locking/stage_fish.png");
    public static final int FISH_OVERLAY_SIZE = 32;
    public static final ResourceLocation FISH_HARDCORE =
            ResourceLocation.fromNamespaceAndPath(AnotherStages.MOD_ID, "textures/stage_locking/stage_fish_hardcore.png");

    /** Client item-model ids ({@code assets/.../items/*.json}) used to replace the real item when hardcore. */
    public static final ResourceLocation HARDCORE_ITEM_MODEL =
            ResourceLocation.fromNamespaceAndPath(AnotherStages.MOD_ID, "stage_locked_hardcore");
    public static final ResourceLocation FISH_HARDCORE_ITEM_MODEL =
            ResourceLocation.fromNamespaceAndPath(AnotherStages.MOD_ID, "stage_fish_hardcore");

    private static final int UNFAMILIAR_RED = 0xFF5555;
    private static final String LOCK_CHAR = "\uD83D\uDD12"; // 🔒
    private static final String FISH_CHAR = "\uD83D\uDC1F"; // 🐟

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
        if (active.isEmpty()) {
            return Mode.NONE;
        }
        // Highest priority wins (list already sorted desc) for exclusive presentation.
        return switch (StageItemHandler.rvMode(active.getFirst())) {
            case "hide" -> Mode.HIDE;
            case "hardcore" -> Mode.HARDCORE;
            default -> Mode.OVERLAY;
        };
    }

    /** Texture for inventory/JEI decorator based on mode + April Fools. */
    public static ResourceLocation textureFor(Mode mode) {
        boolean april = StageAprilFools.isAprilFools();
        return switch (mode) {
            case HARDCORE -> april ? FISH_HARDCORE : HARDCORE_TEXTURE;
            case OVERLAY -> april ? FISH_OVERLAY : LOCK_OVERLAY;
            default -> LOCK_OVERLAY;
        };
    }

    /** Native pixel size of the active overlay badge texture (24 lock / 32 fish). */
    public static int overlayTextureSize() {
        return StageAprilFools.isAprilFools() ? FISH_OVERLAY_SIZE : LOCK_OVERLAY_SIZE;
    }

    /** Flat item model that replaces the real item everywhere while hardcore is active. */
    public static ResourceLocation hardcoreItemModelId() {
        return StageAprilFools.isAprilFools() ? FISH_HARDCORE_ITEM_MODEL : HARDCORE_ITEM_MODEL;
    }

    /**
     * Whether the display name should be replaced with Unfamiliar Item/Fish.
     * Default true when any active visible rule keeps {@code hide_item_name}.
     */
    public static boolean shouldHideItemName(Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || item == null) {
            return false;
        }
        Mode mode = modeFor(mc.player, mc.level, item);
        if (mode == Mode.NONE || mode == Mode.HIDE) {
            return false;
        }
        for (StageItemRule rule : StageItemHandler.findActiveRules(mc.player, mc.level, item)) {
            String rv = StageItemHandler.rvMode(rule);
            if ("hide".equals(rv)) {
                continue;
            }
            if (rule.hideItemName) {
                return true;
            }
        }
        return false;
    }

    /** True when name stays real but should get lock (or fish) chars. */
    public static boolean shouldDecorateRealName(Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || item == null) {
            return false;
        }
        Mode mode = modeFor(mc.player, mc.level, item);
        if (mode == Mode.NONE || mode == Mode.HIDE) {
            return false;
        }
        if (shouldHideItemName(item)) {
            return false;
        }
        return true;
    }

    /** Hidden display name for the strongest active hide_item_name rule (custom or Unfamiliar). */
    public static Component hiddenDisplayName(Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && item != null) {
            for (StageItemRule rule : StageItemHandler.findActiveRules(mc.player, mc.level, item)) {
                String rv = StageItemHandler.rvMode(rule);
                if ("hide".equals(rv) || !rule.hideItemName) {
                    continue;
                }
                if (rule.hiddenItemNameComponent != null) {
                    return rule.hiddenItemNameComponent.copy();
                }
                if (rule.hiddenItemName != null && !rule.hiddenItemName.isBlank()) {
                    return resolveNameString(rule.hiddenItemName.trim());
                }
            }
        }
        return defaultUnfamiliarName();
    }

    /** @deprecated use {@link #hiddenDisplayName(Item)} */
    @Deprecated
    public static Component unfamiliarName() {
        return defaultUnfamiliarName();
    }

    public static Component defaultUnfamiliarName() {
        if (StageAprilFools.isAprilFools()) {
            MutableComponent fish = Component.translatable("item.another_stages.unfamiliar_fish")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(UNFAMILIAR_RED)));
            return Component.literal(FISH_CHAR + " ").append(fish).append(Component.literal(" " + FISH_CHAR));
        }
        return Component.translatable("item.another_stages.unfamiliar_item")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(UNFAMILIAR_RED)));
    }

    private static Component resolveNameString(String raw) {
        if (raw.indexOf(' ') < 0 && raw.indexOf('.') >= 0) {
            return Component.translatable(raw);
        }
        return Component.literal(raw);
    }

    public static Component decorateRealName(Component original) {
        String mark = StageAprilFools.isAprilFools() ? FISH_CHAR : LOCK_CHAR;
        return Component.literal(mark + " ").append(original.copy()).append(Component.literal(" " + mark));
    }

    /** All stage ids from active rules (for tooltips). */
    public static List<String> stageHints(Item item) {
        Minecraft mc = Minecraft.getInstance();
        Set<String> stages = new LinkedHashSet<>();
        if (mc.player == null || mc.level == null || item == null) {
            return List.of();
        }
        for (StageItemRule rule : StageItemHandler.findActiveRules(mc.player, mc.level, item)) {
            if (rule.stages == null) {
                continue;
            }
            for (StageItemHandler.StageCondition c : rule.stages) {
                if (c.stage != null && !c.stage.isBlank()) {
                    stages.add(c.stage.trim());
                }
            }
        }
        return new ArrayList<>(stages);
    }

    /** @deprecated use {@link #stageHints(Item)} */
    @Deprecated
    public static String firstStageHint(Item item) {
        List<String> hints = stageHints(item);
        return hints.isEmpty() ? "" : hints.getFirst();
    }

    public static boolean hasCustomTooltips(StageItemRule rule) {
        return rule != null && (
                (rule.tooltips != null && !rule.tooltips.isEmpty())
                        || (rule.componentTooltips != null && !rule.componentTooltips.isEmpty()));
    }
}
