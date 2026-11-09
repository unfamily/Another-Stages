package net.unfamily.anotherstages.kubejs;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * KubeJS binding {@code AnotherStages}: stage <strong>query</strong> helpers for {@code if}-style checks.
 * Restriction registration prefers {@code AnotherStagesEvents.restrictions}; the {@code items}/{@code recipes}/
 * {@code blocks}/{@code guis} managers remain for retrocompat with packs that called them top-level.
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
}
