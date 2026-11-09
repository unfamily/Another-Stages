package net.unfamily.anotherstages.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-side mirror of the local player's stages (synced from the server).
 * Library stage checks are server-only; RV overlays and stage_block mimic need this cache.
 */
public final class ClientStageCache {
    private static final Set<String> PLAYER = new HashSet<>();
    private static final Set<String> WORLD = new HashSet<>();
    private static final Set<String> TEAM = new HashSet<>();
    private static int revision;
    /** Optional client hook (e.g. rebuild mimic meshes); must not pull client-only classes into common load. */
    private static Runnable onChanged = () -> {};

    private ClientStageCache() {}

    public static void setOnChanged(Runnable hook) {
        onChanged = hook != null ? hook : () -> {};
    }

    public static void apply(List<String> player, List<String> world, List<String> team) {
        PLAYER.clear();
        WORLD.clear();
        TEAM.clear();
        if (player != null) {
            PLAYER.addAll(player);
        }
        if (world != null) {
            WORLD.addAll(world);
        }
        if (team != null) {
            TEAM.addAll(team);
        }
        revision++;
        onChanged.run();
    }

    public static void clear() {
        PLAYER.clear();
        WORLD.clear();
        TEAM.clear();
        revision++;
        onChanged.run();
    }

    public static boolean hasPlayerStage(String stage) {
        return stage != null && PLAYER.contains(stage);
    }

    public static boolean hasWorldStage(String stage) {
        return stage != null && WORLD.contains(stage);
    }

    /** Team stages for the local player's team (empty if not on a team). */
    public static boolean hasTeamStage(String stage) {
        return stage != null && TEAM.contains(stage);
    }

    public static int revision() {
        return revision;
    }

    public static String fingerprint() {
        return revision + "|" + sorted(PLAYER) + "|" + sorted(WORLD) + "|" + sorted(TEAM);
    }

    private static String sorted(Set<String> set) {
        if (set.isEmpty()) {
            return "";
        }
        return String.join(",", set.stream().sorted().toList());
    }

    public static Set<String> playerStagesView() {
        return Collections.unmodifiableSet(PLAYER);
    }

    public static Set<String> worldStagesView() {
        return Collections.unmodifiableSet(WORLD);
    }

    public static Set<String> teamStagesView() {
        return Collections.unmodifiableSet(TEAM);
    }

    /** All known stage ids from the last sync (player ∪ world ∪ team). */
    public static Set<String> allKnownStages() {
        Set<String> all = new HashSet<>(PLAYER.size() + WORLD.size() + TEAM.size());
        all.addAll(PLAYER);
        all.addAll(WORLD);
        all.addAll(TEAM);
        return Collections.unmodifiableSet(all);
    }
}
