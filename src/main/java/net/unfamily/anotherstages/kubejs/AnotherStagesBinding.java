package net.unfamily.anotherstages.kubejs;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Single KubeJS binding: fluent managers + exceptional stage queries.
 * Bound as {@code AnotherStages} only (no {@code Stages} alias).
 */
public final class AnotherStagesBinding {
    public static final AnotherStagesBinding INSTANCE = new AnotherStagesBinding();

    public final ItemRestrictionManager items = new ItemRestrictionManager();
    public final RecipeRestrictionManager recipes = new RecipeRestrictionManager();
    public final BlockRestrictionManager blocks = new BlockRestrictionManager();
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
}
