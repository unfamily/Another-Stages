package net.unfamily.anotherstages.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-side mirror of the local player's stages (synced from the server).
 * Library stage checks are server-only; RV overlays need this cache.
 */
public final class ClientStageCache {
    private static final Set<String> PLAYER = new HashSet<>();
    private static final Set<String> WORLD = new HashSet<>();
    private static final Set<String> TEAM = new HashSet<>();
    private static int revision;

    private ClientStageCache() {}

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
    }

    public static void clear() {
        PLAYER.clear();
        WORLD.clear();
        TEAM.clear();
        revision++;
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
}
