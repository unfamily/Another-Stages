package net.unfamily.anotherstages.kubejs;

import net.minecraft.network.chat.Component;
import net.unfamily.anotherstages.item.StageItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** {@code AnotherStages.items.restrict()…} */
public final class ItemRestrictionManager {
    public ItemRestrictBuilder restrict() {
        return new ItemRestrictBuilder();
    }

    public static final class ItemRestrictBuilder {
        private String explicitId;
        private final StageItemHandler.StageItemRestriction restriction = new StageItemHandler.StageItemRestriction();
        private final StageItemHandler.StageItemRule rule = new StageItemHandler.StageItemRule();
        private boolean enqueued;

        ItemRestrictBuilder() {
            enqueue();
        }

        private void enqueue() {
            if (enqueued) {
                return;
            }
            enqueued = true;
            PendingScriptRestrictions.enqueue(this::commit);
        }

        public ItemRestrictBuilder id(String id) {
            this.explicitId = id;
            return this;
        }

        public ItemRestrictBuilder logicAnd() {
            rule.stagesLogic = "AND";
            return this;
        }

        public ItemRestrictBuilder logicOr() {
            rule.stagesLogic = "OR";
            return this;
        }

        public ItemRestrictBuilder logicDefOr() {
            rule.stagesLogic = "DEF_OR";
            return this;
        }

        public ItemRestrictBuilder logicDefAnd() {
            rule.stagesLogic = "DEF_AND";
            return this;
        }

        public ItemRestrictBuilder stages(Object stages) {
            KubeStageMaps.fillItemStages(stages, rule.stages);
            return this;
        }

        public ItemRestrictBuilder items(Object items) {
            rule.items.clear();
            rule.items.addAll(KubeStageMaps.stringList(items));
            return this;
        }

        public ItemRestrictBuilder exclude(Object exclude) {
            rule.exclude.clear();
            rule.exclude.addAll(KubeStageMaps.stringList(exclude));
            return this;
        }

        public ItemRestrictBuilder containersWhitelist(boolean whitelist) {
            rule.containersWhitelist = whitelist;
            return this;
        }

        public ItemRestrictBuilder containers(Object containers) {
            rule.containersList.clear();
            rule.containersList.addAll(KubeStageMaps.stringList(containers));
            return this;
        }

        public ItemRestrictBuilder rightClick() {
            addOther("right_click");
            return this;
        }

        public ItemRestrictBuilder leftClick() {
            addOther("left_click");
            return this;
        }

        public ItemRestrictBuilder mainHand() {
            addOther("main_hand");
            return this;
        }

        public ItemRestrictBuilder offHand() {
            addOther("off_hand");
            return this;
        }

        public ItemRestrictBuilder otherCaseAll() {
            rule.otherCase.clear();
            rule.otherCase.add("all");
            StageItemHandler.expandOtherCaseAll(rule.otherCase);
            return this;
        }

        private void addOther(String key) {
            if (!rule.otherCase.contains(key)) {
                rule.otherCase.add(key);
            }
        }

        public ItemRestrictBuilder drop() {
            rule.consequence = "drop";
            return this;
        }

        public ItemRestrictBuilder delete() {
            rule.consequence = "delete";
            return this;
        }

        public ItemRestrictBuilder block() {
            rule.consequence = "block";
            return this;
        }

        public ItemRestrictBuilder blockDrop() {
            rule.consequence = "block_drop";
            return this;
        }

        public ItemRestrictBuilder blockDelete() {
            rule.consequence = "block_delete";
            return this;
        }

        public ItemRestrictBuilder viewerOverlay() {
            rule.recipeViewer = "overlay";
            return this;
        }

        public ItemRestrictBuilder viewerHide() {
            rule.recipeViewer = "hide";
            return this;
        }

        public ItemRestrictBuilder viewerPartial() {
            rule.recipeViewer = "partial";
            return this;
        }

        public ItemRestrictBuilder viewerNone() {
            rule.recipeViewer = "none";
            return this;
        }

        public ItemRestrictBuilder tooltip(Component component) {
            if (component != null) {
                rule.componentTooltips.add(component);
            }
            return this;
        }

        public ItemRestrictBuilder tooltips(Object tips) {
            rule.tooltips.clear();
            rule.tooltips.addAll(KubeStageMaps.stringList(tips));
            return this;
        }

        public ItemRestrictBuilder hideDefaultTooltips(boolean hide) {
            rule.hideDefaultTooltips = hide;
            return this;
        }

        public ItemRestrictBuilder when(Object conditionIndices, Consumer<ItemBranchBuilder> branch) {
            ItemBranchBuilder b = new ItemBranchBuilder();
            if (branch != null) {
                branch.accept(b);
            }
            StageItemHandler.StageItemIfRule ifRule = b.toIfRule();
            ifRule.conditions.clear();
            ifRule.conditions.addAll(KubeStageMaps.intList(conditionIndices));
            StageItemHandler.expandOtherCaseAll(ifRule.otherCase);
            rule.ifConditions.add(ifRule);
            return this;
        }

        private void commit() {
            StageItemHandler.expandOtherCaseAll(rule.otherCase);
            restriction.restrictions.clear();
            restriction.restrictions.add(rule);
            String id = explicitId != null && !explicitId.isBlank()
                    ? explicitId
                    : ScriptIdAllocator.allocate("item", fingerprint(), StageItemHandler::hasRestrictionId);
            StageItemHandler.putScriptRestriction(id, restriction);
        }

        private String fingerprint() {
            List<String> parts = new ArrayList<>();
            parts.add(rule.stagesLogic);
            parts.add(rule.consequence);
            parts.add(rule.recipeViewer);
            parts.add(String.valueOf(rule.containersWhitelist));
            parts.add(String.join(",", rule.items));
            parts.add(String.join(",", rule.exclude));
            parts.add(String.join(",", rule.otherCase));
            parts.add(String.join(",", rule.containersList));
            for (StageItemHandler.StageCondition c : rule.stages) {
                parts.add(c.stageType + ":" + c.stage + ":" + c.is);
            }
            parts.add("if=" + rule.ifConditions.size());
            return ScriptIdAllocator.fingerprint(parts.toArray(String[]::new));
        }
    }

    public static final class ItemBranchBuilder {
        private final StageItemHandler.StageItemIfRule ifRule = new StageItemHandler.StageItemIfRule();

        public ItemBranchBuilder containersWhitelist(boolean whitelist) {
            ifRule.containersWhitelist = whitelist;
            return this;
        }

        public ItemBranchBuilder containers(Object containers) {
            ifRule.containersList.clear();
            ifRule.containersList.addAll(KubeStageMaps.stringList(containers));
            return this;
        }

        public ItemBranchBuilder items(Object items) {
            ifRule.items.clear();
            ifRule.items.addAll(KubeStageMaps.stringList(items));
            return this;
        }

        public ItemBranchBuilder exclude(Object exclude) {
            ifRule.exclude.clear();
            ifRule.exclude.addAll(KubeStageMaps.stringList(exclude));
            return this;
        }

        public ItemBranchBuilder rightClick() {
            addOther("right_click");
            return this;
        }

        public ItemBranchBuilder leftClick() {
            addOther("left_click");
            return this;
        }

        public ItemBranchBuilder mainHand() {
            addOther("main_hand");
            return this;
        }

        public ItemBranchBuilder offHand() {
            addOther("off_hand");
            return this;
        }

        public ItemBranchBuilder otherCaseAll() {
            ifRule.otherCase.clear();
            ifRule.otherCase.add("all");
            return this;
        }

        private void addOther(String key) {
            if (!ifRule.otherCase.contains(key)) {
                ifRule.otherCase.add(key);
            }
        }

        public ItemBranchBuilder drop() {
            ifRule.consequence = "drop";
            return this;
        }

        public ItemBranchBuilder delete() {
            ifRule.consequence = "delete";
            return this;
        }

        public ItemBranchBuilder block() {
            ifRule.consequence = "block";
            return this;
        }

        public ItemBranchBuilder blockDrop() {
            ifRule.consequence = "block_drop";
            return this;
        }

        public ItemBranchBuilder blockDelete() {
            ifRule.consequence = "block_delete";
            return this;
        }

        StageItemHandler.StageItemIfRule toIfRule() {
            return ifRule;
        }
    }
}
