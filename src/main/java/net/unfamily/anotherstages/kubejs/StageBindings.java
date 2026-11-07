package net.unfamily.anotherstages.kubejs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.unfamily.iskalib.stage.StageRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * KubeJS bindings for interrogating stages. No events — call from existing script hooks.
 * Prefer {@code teamHasStage(player, stage)} for pack checks that should include the player's team.
 */
public final class StageBindings {
    private StageBindings() {}

    public static boolean playerHasStage(@Nullable Player player, @Nullable String stage) {
        if (player == null || stage == null || stage.isEmpty()) {
            return false;
        }
        return StageRegistry.playerHasStage(player, stage);
    }

    /** Player's personal stages plus their team stages (Library FTB sync). */
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
