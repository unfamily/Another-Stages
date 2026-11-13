package net.unfamily.anotherstages.compat.sophisticated;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Interacting player for Sophisticated {@code BlockConverterRecipeContainer} instances.
 * The container does not store the upgrade container as a field — capture at {@code <init>}.
 */
public final class SophBlockConverterSubjects {
    private static final Map<Object, Player> PLAYERS = Collections.synchronizedMap(new WeakHashMap<>());

    private SophBlockConverterSubjects() {}

    public static void set(Object recipeContainer, @Nullable Player player) {
        if (recipeContainer == null) {
            return;
        }
        if (player == null) {
            PLAYERS.remove(recipeContainer);
        } else {
            PLAYERS.put(recipeContainer, player);
        }
    }

    @Nullable
    public static Player get(Object recipeContainer) {
        return recipeContainer == null ? null : PLAYERS.get(recipeContainer);
    }
}
