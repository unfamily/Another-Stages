package net.unfamily.anotherstages.kubejs;

import net.minecraft.network.chat.Component;
import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.block.StageBlockRule;
import net.unfamily.anotherstages.recipe.StageCondition;

import java.util.ArrayList;
import java.util.List;

/** {@code AnotherStages.blocks.restrict()…} */
public final class BlockRestrictionManager {
    public BlockRestrictBuilder restrict() {
        return new BlockRestrictBuilder();
    }

    public static final class BlockRestrictBuilder {
        private String explicitId;
        private final StageBlockRule rule = new StageBlockRule();
        private boolean enqueued;

        BlockRestrictBuilder() {
            enqueue();
        }

        private void enqueue() {
            if (enqueued) {
                return;
            }
            enqueued = true;
            PendingScriptRestrictions.enqueue(this::commit);
        }

        public BlockRestrictBuilder id(String id) {
            this.explicitId = id;
            return this;
        }

        public BlockRestrictBuilder logicAnd() {
            rule.stagesLogic = "AND";
            return this;
        }

        public BlockRestrictBuilder logicOr() {
            rule.stagesLogic = "OR";
            return this;
        }

        public BlockRestrictBuilder stages(Object stages) {
            KubeStageMaps.fillRecipeStages(stages, rule.stages);
            return this;
        }

        public BlockRestrictBuilder blocks(Object blocks) {
            rule.blocks.clear();
            rule.blocks.addAll(KubeStageMaps.stringList(blocks));
            return this;
        }

        public BlockRestrictBuilder exclude(Object exclude) {
            rule.exclude.clear();
            rule.exclude.addAll(KubeStageMaps.stringList(exclude));
            return this;
        }

        public BlockRestrictBuilder mimic(String mimic) {
            if (mimic != null && !mimic.isBlank()) {
                rule.mimic = mimic;
            }
            return this;
        }

        public BlockRestrictBuilder unbreakable(boolean unbreakable) {
            rule.unbreakable = unbreakable;
            return this;
        }

        public BlockRestrictBuilder breakMessage(String message) {
            rule.breakMessage = message == null ? "" : message;
            return this;
        }

        public BlockRestrictBuilder interactMessage(String message) {
            rule.interactMessage = message == null ? "" : message;
            return this;
        }

        public BlockRestrictBuilder tooltip(Component component) {
            if (component != null) {
                rule.componentTooltips.add(component);
            }
            return this;
        }

        public BlockRestrictBuilder tooltips(Object tips) {
            rule.tooltips.clear();
            rule.tooltips.addAll(KubeStageMaps.stringList(tips));
            return this;
        }

        public BlockRestrictBuilder hideDefaultTooltips(boolean hide) {
            rule.hideDefaultTooltips = hide;
            return this;
        }

        private void commit() {
            String id = explicitId != null && !explicitId.isBlank()
                    ? explicitId
                    : ScriptIdAllocator.allocate("block", fingerprint(), StageBlockHandler::hasRuleId);
            StageBlockHandler.putScriptRule(id, rule);
        }

        private String fingerprint() {
            List<String> parts = new ArrayList<>();
            parts.add(rule.stagesLogic);
            parts.add(rule.mimic);
            parts.add(String.valueOf(rule.unbreakable));
            parts.add(String.join(",", rule.blocks));
            parts.add(String.join(",", rule.exclude));
            for (StageCondition c : rule.stages) {
                parts.add(c.stageType + ":" + c.stage + ":" + c.is);
            }
            return ScriptIdAllocator.fingerprint(parts.toArray(String[]::new));
        }
    }
}
