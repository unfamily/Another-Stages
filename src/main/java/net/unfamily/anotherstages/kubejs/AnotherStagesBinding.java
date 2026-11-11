package net.unfamily.anotherstages.kubejs;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * KubeJS binding {@code AnotherStages}: stage <strong>query</strong> and <strong>mutate</strong> helpers.
 * Restriction registration prefers {@code AnotherStagesEvents.restrictions}; the {@code items}/{@code recipes}/
 * {@code blocks}/{@code guis} managers remain for retrocompat with packs that called them top-level.
 * Usable from any server Kube event (not only {@code stageChanged}).
 */
public final class AnotherStagesBinding {
    public static final AnotherStagesBinding INSTANCE = new AnotherStagesBinding();

    /** Prefer {@code AnotherStagesEvents.restrictions(e => e.items…)}. Kept for retrocompat. */
    @Deprecated
    public final ItemRestrictionManager items = new ItemRestrictionManager();
    /** Prefer {@code AnotherStagesEvents.restrictions(e => e.recipes…)}. Kept for retrocompat. */
    @Deprecated
    public final RecipeRestrictionManager recipes = new RecipeRestrictionManager();
    /** Prefer {@code AnotherStagesEvents.restrictions(e => e.blocks…)}. Kept for retrocompat. */
    @Deprecated
    public final BlockRestrictionManager blocks = new BlockRestrictionManager();
    /** Prefer {@code AnotherStagesEvents.restrictions(e => e.guis…)}. Kept for retrocompat. */
    @Deprecated
    public final GuiRestrictionManager guis = new GuiRestrictionManager();

    private AnotherStagesBinding() {}

    public boolean playerHasStage(@Nullable Player player, @Nullable String stage) {
        return StageBindings.playerHasStage(player, stage);
    }

    public boolean teamHasStage(@Nullable Player player, @Nullable String stage) {
        return StageBindings.teamHasStage(player, stage);
    }

    public boolean worldHasStage(@Nullable Level level, @Nullable String stage) {
        return StageBindings.worldHasStage(level, stage);
    }

    public boolean playerHasStageMatching(@Nullable Player player, @Nullable String regex) {
        return StageBindings.playerHasStageMatching(player, regex);
    }

    public boolean teamHasStageMatching(@Nullable Player player, @Nullable String regex) {
        return StageBindings.teamHasStageMatching(player, regex);
    }

    public boolean worldHasStageMatching(@Nullable Level level, @Nullable String regex) {
        return StageBindings.worldHasStageMatching(level, regex);
    }

    public boolean addPlayerStage(@Nullable Player player, @Nullable String stage) {
        return StageBindings.addPlayerStage(player, stage);
    }

    public boolean addPlayerStage(@Nullable Player player, @Nullable String stage, boolean hideLog) {
        return StageBindings.addPlayerStage(player, stage, hideLog);
    }

    public boolean removePlayerStage(@Nullable Player player, @Nullable String stage) {
        return StageBindings.removePlayerStage(player, stage);
    }

    public boolean removePlayerStage(@Nullable Player player, @Nullable String stage, boolean hideLog) {
        return StageBindings.removePlayerStage(player, stage, hideLog);
    }

    public boolean setPlayerStage(@Nullable Player player, @Nullable String stage, boolean value) {
        return StageBindings.setPlayerStage(player, stage, value);
    }

    public boolean addWorldStage(@Nullable Level level, @Nullable String stage) {
        return StageBindings.addWorldStage(level, stage);
    }

    public boolean removeWorldStage(@Nullable Level level, @Nullable String stage) {
        return StageBindings.removeWorldStage(level, stage);
    }

    public boolean setWorldStage(@Nullable Level level, @Nullable String stage, boolean value) {
        return StageBindings.setWorldStage(level, stage, value);
    }

    /** Player's current team (Library {@code team_player}). */
    public boolean addTeamStage(@Nullable Player player, @Nullable String stage) {
        return StageBindings.addTeamStage(player, stage);
    }

    public boolean removeTeamStage(@Nullable Player player, @Nullable String stage) {
        return StageBindings.removeTeamStage(player, stage);
    }

    public boolean setTeamStage(@Nullable Player player, @Nullable String stage, boolean value) {
        return StageBindings.setTeamStage(player, stage, value);
    }

    /** Named team key (Library {@code team <name>}). Prefer {@link #addTeamStage(Player, String)} when possible. */
    public boolean addNamedTeamStage(@Nullable Level level, @Nullable String teamName, @Nullable String stage) {
        return StageBindings.addNamedTeamStage(level, teamName, stage);
    }

    public boolean removeNamedTeamStage(@Nullable Level level, @Nullable String teamName, @Nullable String stage) {
        return StageBindings.removeNamedTeamStage(level, teamName, stage);
    }

    /**
     * Registers a stage id for command Tab autocomplete ({@code player}/{@code world}/{@code team}).
     * Does not grant the stage. Prefer JSON {@code iska_lib:known_stages} for datapack packs.
     */
    public void registerStage(@Nullable String stageType, @Nullable String stage) {
        StageBindings.registerStage(stageType, stage);
    }

    public void registerStages(@Nullable String stageType, @Nullable Collection<String> stages) {
        StageBindings.registerStages(stageType, stages);
    }
}
