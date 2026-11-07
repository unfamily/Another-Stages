package net.unfamily.anotherstages.recipe;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.unfamily.anotherstages.config.AsConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional action-bar tip when a stage-blocked recipe is denied.
 * Off by default; when enabled, skipped while the owner has a container menu open
 * (crafting / machine GUIs already show empty results or JEI locks).
 */
public final class RecipeStageFeedback {
    private static final long COOLDOWN_MS = 3000L;
    private static final Map<UUID, Long> LAST_NOTIFY_MS = new ConcurrentHashMap<>();

    private RecipeStageFeedback() {}

    /**
     * Notify the online owner that a recipe was blocked, if config allows and no GUI is open.
     */
    public static void maybeNotify(@Nullable ServerPlayer onlinePlayer) {
        if (!AsConfig.recipeBlockedActionbar() || onlinePlayer == null) {
            return;
        }
        if (onlinePlayer.containerMenu != null && !(onlinePlayer.containerMenu instanceof InventoryMenu)) {
            return;
        }
        long now = System.currentTimeMillis();
        UUID id = onlinePlayer.getUUID();
        Long last = LAST_NOTIFY_MS.get(id);
        if (last != null && now - last < COOLDOWN_MS) {
            return;
        }
        LAST_NOTIFY_MS.put(id, now);
        onlinePlayer.sendSystemMessage(Component.translatable("message.another_stages.recipe_blocked"), true);
    }
}
