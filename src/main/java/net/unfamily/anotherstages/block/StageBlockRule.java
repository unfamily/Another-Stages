package net.unfamily.anotherstages.block;

import net.minecraft.network.chat.Component;
import net.unfamily.anotherstages.recipe.StageCondition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class StageBlockRule {
    /** Lang key used when {@code .breakMessage(true)} opts into default feedback. */
    public static final String DEFAULT_BREAK_MESSAGE = "message.another_stages.block_locked";
    /** Lang key used when {@code .interactMessage(true)} opts into default feedback. */
    public static final String DEFAULT_INTERACT_MESSAGE = "message.another_stages.block_locked";

    public String stagesLogic = "AND";
    public List<StageCondition> stages = new ArrayList<>();
    public List<String> blocks = new ArrayList<>();
    public List<String> exclude = new ArrayList<>();
    /** Replacement block id used for loot (always) and client camouflage when {@link #camouflage} is true. */
    public String mimic = "minecraft:stone";
    /**
     * When true (default): hide real mesh and draw {@link #mimic}.
     * When false: keep real appearance; loot still uses {@link #mimic}.
     */
    public boolean camouflage = true;
    public boolean unbreakable = false;
    /**
     * DANGEROUS / best-effort: when true and the rule is active, skip BlockEntity ticks,
     * scheduled block/fluid ticks, and random ticks at this position.
     * Default false. Prefer {@code stage_type: world}; player/team with no subject are fail-close.
     */
    public boolean stopTick = false;
    /**
     * Break action-bar text: lang key (dotted, no spaces) or literal.
     * Default blank = silent; use {@link #DEFAULT_BREAK_MESSAGE} via {@code .breakMessage(true)}.
     */
    public String breakMessage = "";
    /** Kube {@code Text}/{@code Component} break message; wins over {@link #breakMessage} when set. */
    @Nullable
    public Component breakMessageComponent;
    /**
     * Interact action-bar text: lang key or literal.
     * Default blank = silent; use {@link #DEFAULT_INTERACT_MESSAGE} via {@code .interactMessage(true)}.
     */
    public String interactMessage = "";
    /** Kube {@code Text}/{@code Component} interact message; wins over {@link #interactMessage} when set. */
    @Nullable
    public Component interactMessageComponent;
    public List<String> tooltips = new ArrayList<>();
    public boolean hideDefaultTooltips = false;
    /** KubeJS Component tooltips (in addition to string tooltips). */
    public List<Component> componentTooltips = new ArrayList<>();
}
