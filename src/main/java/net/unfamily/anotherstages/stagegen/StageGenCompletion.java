package net.unfamily.anotherstages.stagegen;

import net.minecraft.network.chat.Component;
import net.unfamily.anotherstages.client.gui.stagegen.RestrictionDraft;

/**
 * Autonomous readiness checks for Stage Generator drafts (Save gating + browse badges).
 */
public final class StageGenCompletion {
    private StageGenCompletion() {}

    public record Status(boolean ready, Component reason) {
        public static Status ok() {
            return new Status(true, Component.translatable("screen.another_stages.stage_generator.complete.ready"));
        }

        public static Status fail(String key) {
            return new Status(false, Component.translatable(key));
        }
    }

    public static Status evaluate(RestrictionDraft d) {
        if (d == null) {
            return Status.fail("screen.another_stages.stage_generator.complete.missing_draft");
        }
        if (!hasNamedStage(d)) {
            return Status.fail("screen.another_stages.stage_generator.complete.need_stage");
        }
        return switch (d.category) {
            case ITEM -> evaluateItem(d);
            case RECIPE -> evaluateRecipe(d);
            case BLOCK -> evaluateBlock(d);
            case GUI -> evaluateGui(d);
        };
    }

    public static boolean isReady(RestrictionDraft d) {
        return evaluate(d).ready();
    }

    private static boolean hasNamedStage(RestrictionDraft d) {
        for (RestrictionDraft.StageRow row : d.stages) {
            if (row != null && row.id != null && !row.id.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static Status evaluateItem(RestrictionDraft d) {
        if (d.matchers.isEmpty()) {
            return Status.fail("screen.another_stages.stage_generator.complete.need_matchers");
        }
        if (d.caseList().isEmpty()) {
            return Status.fail("screen.another_stages.stage_generator.complete.need_cases");
        }
        return Status.ok();
    }

    private static Status evaluateRecipe(RestrictionDraft d) {
        boolean any = !d.matchers.isEmpty()
                || !d.recipes.isEmpty()
                || !d.recipeTypes.isEmpty()
                || !d.inputs.isEmpty();
        if (!any) {
            return Status.fail("screen.another_stages.stage_generator.complete.need_recipe_target");
        }
        return Status.ok();
    }

    private static Status evaluateBlock(RestrictionDraft d) {
        if (d.matchers.isEmpty()) {
            return Status.fail("screen.another_stages.stage_generator.complete.need_blocks");
        }
        if (d.mimic == null || d.mimic.isBlank()) {
            return Status.fail("screen.another_stages.stage_generator.complete.need_mimic");
        }
        return Status.ok();
    }

    private static Status evaluateGui(RestrictionDraft d) {
        if (d.menuTypes.isEmpty() && d.menus.isEmpty()) {
            return Status.fail("screen.another_stages.stage_generator.complete.need_menus");
        }
        return Status.ok();
    }
}
