package net.unfamily.anotherstages.block;

import net.unfamily.anotherstages.recipe.StageCondition;

import java.util.ArrayList;
import java.util.List;

public final class StageBlockRule {
    public String stagesLogic = "AND";
    public List<StageCondition> stages = new ArrayList<>();
    public List<String> blocks = new ArrayList<>();
    public List<String> exclude = new ArrayList<>();
    /** Replacement block id used for client render + loot when locked. */
    public String mimic = "minecraft:stone";
    public boolean unbreakable = false;
    public String breakMessage = "";
    public String interactMessage = "";
    public List<String> tooltips = new ArrayList<>();
    public boolean hideDefaultTooltips = false;
    /** KubeJS Component tooltips (in addition to string tooltips). */
    public List<net.minecraft.network.chat.Component> componentTooltips = new ArrayList<>();
}
