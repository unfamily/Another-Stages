package net.unfamily.anotherstages.kubejs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.unfamily.iskalib.stage.StageCatalog;
import net.unfamily.iskalib.stage.StageRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Stage query + mutate helpers for the {@code AnotherStages} binding.
 * Restriction registration uses {@link AnotherStagesEvents#RESTRICTIONS}, not these methods.
 * Mutators call Library {@link StageRegistry} (hooks / Stage Actions / {@code stageChanged} still fire).
 */
public final class StageBindings {
    private StageBindings() {}

    public static boolean playerHasStage(@Nullable Player player, @Nullable String stage) {
        if (player == null || stage == null || stage.isEmpty()) {
            return false;
        }
        return StageRegistry.playerHasStage(player, stage);
    }

    /** Team stages only for the player's shop/FTB team (Library TeamStageData). Not personal stages. */
    public static boolean teamHasStage(@Nullable Player player, @Nullable String stage) {
        if (player == null || stage == null || stage.isEmpty()) {
            return false;
        }
        return StageRegistry.playerTeamHasStage(player, stage);
    }

    public static boolean worldHasStage(@Nullable Level level, @Nullable String stage) {
        if (level == null || stage == null || stage.isEmpty()) {
            return false;
        }
        return StageRegistry.worldHasStage(level, stage);
    }

    /** Adds a personal stage. Server-only; returns {@code false} on client / invalid args. */
    public static boolean addPlayerStage(@Nullable Player player, @Nullable String stage) {
        return addPlayerStage(player, stage, false);
    }

    public static boolean addPlayerStage(@Nullable Player player, @Nullable String stage, boolean hideLog) {
        if (player == null || stage == null || stage.isEmpty()) {
            return false;
        }
        return StageRegistry.addPlayerStage(player, stage, hideLog);
    }

    /** Removes a personal stage. */
    public static boolean removePlayerStage(@Nullable Player player, @Nullable String stage) {
        return removePlayerStage(player, stage, false);
    }

    public static boolean removePlayerStage(@Nullable Player player, @Nullable String stage, boolean hideLog) {
        if (player == null || stage == null || stage.isEmpty()) {
            return false;
        }
        return StageRegistry.removePlayerStage(player, stage, hideLog);
    }

    /** Sets a personal stage to {@code value} ({@code true} = add, {@code false} = remove). */
    public static boolean setPlayerStage(@Nullable Player player, @Nullable String stage, boolean value) {
        return value ? addPlayerStage(player, stage) : removePlayerStage(player, stage);
    }

    public static boolean addWorldStage(@Nullable Level level, @Nullable String stage) {
        if (level == null || stage == null || stage.isEmpty()) {
            return false;
        }
        return StageRegistry.addWorldStage(level, stage);
    }

    public static boolean removeWorldStage(@Nullable Level level, @Nullable String stage) {
        if (level == null || stage == null || stage.isEmpty()) {
            return false;
        }
        return StageRegistry.removeWorldStage(level, stage);
    }

    public static boolean setWorldStage(@Nullable Level level, @Nullable String stage, boolean value) {
        return value ? addWorldStage(level, stage) : removeWorldStage(level, stage);
    }

    /**
     * Adds a stage to the player's current team (Library {@code team_player} path).
     * Prefer this over raw team-name keys when FTB Teams sync is on.
     */
    public static boolean addTeamStage(@Nullable Player player, @Nullable String stage) {
        if (!(player instanceof ServerPlayer serverPlayer) || stage == null || stage.isEmpty()) {
            return false;
        }
        if (!(serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || serverLevel.getServer() == null) {
            return false;
        }
        return StageRegistry.getInstance(serverLevel.getServer()).setPlayerTeamStage(serverPlayer, stage, true);
    }

    /** Removes a stage from the player's current team. */
    public static boolean removeTeamStage(@Nullable Player player, @Nullable String stage) {
        if (!(player instanceof ServerPlayer serverPlayer) || stage == null || stage.isEmpty()) {
            return false;
        }
        if (!(serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || serverLevel.getServer() == null) {
            return false;
        }
        return StageRegistry.getInstance(serverLevel.getServer()).setPlayerTeamStage(serverPlayer, stage, false);
    }

    public static boolean setTeamStage(@Nullable Player player, @Nullable String stage, boolean value) {
        return value ? addTeamStage(player, stage) : removeTeamStage(player, stage);
    }

    /** Adds a stage to a named team key (Library {@code team <name>}). Prefer {@link #addTeamStage(Player, String)} when possible. */
    public static boolean addNamedTeamStage(@Nullable Level level, @Nullable String teamName, @Nullable String stage) {
        if (level == null || teamName == null || teamName.isEmpty() || stage == null || stage.isEmpty()) {
            return false;
        }
        return StageRegistry.addTeamStage(level, teamName, stage);
    }

    public static boolean removeNamedTeamStage(@Nullable Level level, @Nullable String teamName, @Nullable String stage) {
        if (level == null || teamName == null || teamName.isEmpty() || stage == null || stage.isEmpty()) {
            return false;
        }
        return StageRegistry.removeTeamStage(level, teamName, stage);
    }

    /** True if the player has any personal stage whose id matches {@code regex}. */
    public static boolean playerHasStageMatching(@Nullable Player player, @Nullable String regex) {
        if (player == null || regex == null || regex.isEmpty() || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (!(serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || serverLevel.getServer() == null) {
            return false;
        }
        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (Exception ignored) {
            return false;
        }
        var stages = StageRegistry.getInstance(serverLevel.getServer()).getPlayerStages(serverPlayer);
        if (stages == null || stages.isEmpty()) {
            return false;
        }
        for (String id : stages) {
            if (id != null && pattern.matcher(id).find()) {
                return true;
            }
        }
        return false;
    }

    /** True if any world stage id matches {@code regex}. */
    public static boolean worldHasStageMatching(@Nullable Level level, @Nullable String regex) {
        if (level == null || regex == null || regex.isEmpty()
                || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || serverLevel.getServer() == null) {
            return false;
        }
        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (Exception ignored) {
            return false;
        }
        var stages = StageRegistry.getInstance(serverLevel.getServer()).getWorldStages();
        if (stages == null || stages.isEmpty()) {
            return false;
        }
        for (String id : stages) {
            if (id != null && pattern.matcher(id).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registers a stage id for typed Tab suggestions ({@code player}/{@code world}/{@code team}).
     * Does not grant the stage to anyone.
     */
    public static void registerStage(@Nullable String stageType, @Nullable String stage) {
        StageCatalog.registerKnownStage(stageType, stage);
    }

    /** Registers many stage ids for one typed branch. */
    public static void registerStages(@Nullable String stageType, @Nullable Collection<String> stages) {
        StageCatalog.registerKnownStages(stageType, stages);
    }

    /** True if any of the player's team stages match {@code regex}. */
    public static boolean teamHasStageMatching(@Nullable Player player, @Nullable String regex) {
        if (player == null || regex == null || regex.isEmpty() || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (!(serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || serverLevel.getServer() == null) {
            return false;
        }
        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (Exception ignored) {
            return false;
        }
        var stages = StageRegistry.getInstance(serverLevel.getServer()).getPlayerTeamStages(serverPlayer);
        if (stages == null || stages.isEmpty()) {
            return false;
        }
        for (String id : stages) {
            if (id != null && pattern.matcher(id).find()) {
                return true;
            }
        }
        return false;
    }
}
