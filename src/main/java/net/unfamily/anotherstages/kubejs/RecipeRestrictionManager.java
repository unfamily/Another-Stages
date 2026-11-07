package net.unfamily.anotherstages.kubejs;

import net.unfamily.anotherstages.recipe.StageCondition;
import net.unfamily.anotherstages.recipe.StageRecipeHandler;
import net.unfamily.anotherstages.recipe.StageRecipeRule;

import java.util.ArrayList;
import java.util.List;

/** {@code AnotherStages.recipes.restrict()…} */
public final class RecipeRestrictionManager {
    public RecipeRestrictBuilder restrict() {
        return new RecipeRestrictBuilder();
    }

    public static final class RecipeRestrictBuilder {
        private String explicitId;
        private final StageRecipeRule rule = new StageRecipeRule();
        private boolean enqueued;

        RecipeRestrictBuilder() {
            enqueue();
        }

        private void enqueue() {
            if (enqueued) {
                return;
            }
            enqueued = true;
            PendingScriptRestrictions.enqueue(this::commit);
        }

        public RecipeRestrictBuilder id(String id) {
            this.explicitId = id;
            return this;
        }

        public RecipeRestrictBuilder logicAnd() {
            rule.stagesLogic = "AND";
            return this;
        }

        public RecipeRestrictBuilder logicOr() {
            rule.stagesLogic = "OR";
            return this;
        }

        public RecipeRestrictBuilder stages(Object stages) {
            KubeStageMaps.fillRecipeStages(stages, rule.stages);
            return this;
        }

        public RecipeRestrictBuilder recipes(Object recipes) {
            rule.recipes.clear();
            rule.recipes.addAll(KubeStageMaps.stringList(recipes));
            return this;
        }

        public RecipeRestrictBuilder recipeTypes(Object types) {
            rule.recipeTypes.clear();
            rule.recipeTypes.addAll(KubeStageMaps.stringList(types));
            return this;
        }

        public RecipeRestrictBuilder outputs(Object outputs) {
            rule.outputs.clear();
            rule.outputs.addAll(KubeStageMaps.stringList(outputs));
            return this;
        }

        public RecipeRestrictBuilder inputs(Object inputs) {
            rule.inputs.clear();
            rule.inputs.addAll(KubeStageMaps.stringList(inputs));
            return this;
        }

        public RecipeRestrictBuilder excludeRecipes(Object exclude) {
            rule.excludeRecipes.clear();
            rule.excludeRecipes.addAll(KubeStageMaps.stringList(exclude));
            return this;
        }

        private void commit() {
            String id = explicitId != null && !explicitId.isBlank()
                    ? explicitId
                    : ScriptIdAllocator.allocate("recipe", fingerprint(), StageRecipeHandler::hasRuleId);
            StageRecipeHandler.putScriptRule(id, rule);
        }

        private String fingerprint() {
            List<String> parts = new ArrayList<>();
            parts.add(rule.stagesLogic);
            parts.add(String.join(",", rule.recipes));
            parts.add(String.join(",", rule.recipeTypes));
            parts.add(String.join(",", rule.outputs));
            parts.add(String.join(",", rule.inputs));
            parts.add(String.join(",", rule.excludeRecipes));
            for (StageCondition c : rule.stages) {
                parts.add(c.stageType + ":" + c.stage + ":" + c.is);
            }
            return ScriptIdAllocator.fingerprint(parts.toArray(String[]::new));
        }
    }
}
