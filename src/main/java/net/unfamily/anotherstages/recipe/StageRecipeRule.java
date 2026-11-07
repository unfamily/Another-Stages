package net.unfamily.anotherstages.recipe;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** One restriction rule from a stage_recipe JSON file. */
public final class StageRecipeRule {
    public String stagesLogic = "AND";
    public List<StageCondition> stages = new ArrayList<>();
    public List<String> recipes = new ArrayList<>();
    public List<String> recipeTypes = new ArrayList<>();
    public List<String> outputs = new ArrayList<>();
    public List<String> inputs = new ArrayList<>();
    public List<String> excludeRecipes = new ArrayList<>();
    /**
     * Recipe-viewer presentation when this rule is active:
     * {@code overlay} (default) or {@code hide}.
     */
    public String recipeViewer = "overlay";
    /** Optional custom tooltip lines (lang key or literal). */
    public List<String> tooltips = new ArrayList<>();
    /** When true, clear JEI recipe tooltips before appending custom / stage lock tips. */
    public boolean hideDefaultTooltips = false;
    /** KubeJS Component tooltips (in addition to string tooltips). */
    public List<Component> componentTooltips = new ArrayList<>();
}
