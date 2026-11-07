package net.unfamily.anotherstages.recipe;

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
}
