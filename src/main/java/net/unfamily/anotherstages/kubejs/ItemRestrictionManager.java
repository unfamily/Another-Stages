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

        /**
         * Conflict resolution when several rules match the same item.
         * Higher value wins for exclusive presentation; default {@code 0}; ties keep registration order.
         */
        public ItemRestrictBuilder priority(int priority) {
            rule.priority = priority;
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

        public ItemRestrictBuilder caseRightClick() {
            addOther("right_click");
            return this;
        }

        public ItemRestrictBuilder caseLeftClick() {
            addOther("left_click");
            return this;
        }

        public ItemRestrictBuilder caseMainHand() {
            addOther("main_hand");
            return this;
        }

        public ItemRestrictBuilder caseOffHand() {
            addOther("off_hand");
            return this;
        }

        /** Adds {@code inventory} case with default {@code inventory_ticks} (30). */
        public ItemRestrictBuilder caseInventory() {
            return caseInventory(StageItemHandler.DEFAULT_INVENTORY_TICKS);
        }

        /**
         * Adds {@code inventory} case. {@code ticks < 1} (or omitted via no-arg) uses default 30.
         */
        public ItemRestrictBuilder caseInventory(int ticks) {
            addOther(StageItemHandler.CASE_INVENTORY);
            rule.inventoryTicks = StageItemHandler.normalizeInventoryTicks(ticks);
            return this;
        }

        /**
         * List cases (array or string). Merges into the existing case list without clearing.
         * Same keys as JSON {@code cases} / {@code other_case}.
         */
        public ItemRestrictBuilder cases(Object cases) {
            for (String key : KubeStageMaps.stringList(cases)) {
                addOther(key);
            }
            StageItemHandler.expandOtherCaseAll(rule.otherCase);
            return this;
        }

        /**
         * Four click/hand cases <strong>plus</strong> {@code inventory} with default ticks (30).
         */
        public ItemRestrictBuilder caseAll() {
            return caseAll(StageItemHandler.DEFAULT_INVENTORY_TICKS);
        }

        /**
         * Like {@link #caseAll()} with custom {@code inventory_ticks} ({@code ticks < 1} → 30).
         */
        public ItemRestrictBuilder caseAll(int ticks) {
            rule.otherCase.clear();
            for (String s : StageItemHandler.ALL_OTHER_CASES) {
                rule.otherCase.add(s);
            }
            rule.otherCase.add(StageItemHandler.CASE_INVENTORY);
            rule.inventoryTicks = StageItemHandler.normalizeInventoryTicks(ticks);
            return this;
        }

        /** When false, matching ItemEntities cannot be picked up while the rule is active. Default true. */
        public ItemRestrictBuilder canPickup(boolean canPickup) {
            rule.canPickup = canPickup;
            return this;
        }

        /** Opt into default pickup-denied action-bar lang, or silence with {@code false}. */
        public ItemRestrictBuilder pickupMessage(boolean useDefault) {
            rule.pickupMessageComponent = null;
            rule.pickupMessage = useDefault ? StageItemHandler.DEFAULT_PICKUP_MESSAGE : "";
            return this;
        }

        public ItemRestrictBuilder pickupMessage(String message) {
            rule.pickupMessageComponent = null;
            rule.pickupMessage = message == null ? "" : message;
            return this;
        }

        public ItemRestrictBuilder pickupMessage(Component component) {
            if (component != null) {
                rule.pickupMessageComponent = component;
            }
            return this;
        }

        private void addOther(String key) {
            if (!rule.otherCase.contains(key)) {
                rule.otherCase.add(key);
            }
        }

        public ItemRestrictBuilder consequenceDrop() {
            rule.consequence = "drop";
            return this;
        }

        public ItemRestrictBuilder consequenceDelete() {
            rule.consequence = "delete";
            return this;
        }

        public ItemRestrictBuilder consequenceBlock() {
            rule.consequence = "block";
            return this;
        }

        public ItemRestrictBuilder consequenceBlockDrop() {
            rule.consequence = "block_drop";
            return this;
        }

        public ItemRestrictBuilder consequenceBlockDelete() {
            rule.consequence = "block_delete";
            return this;
        }

        /**
         * Set consequence from array or string (same keys as JSON {@code consequence}).
         * Runtime keeps one consequence; if multiple values, the last non-blank wins.
         */
        public ItemRestrictBuilder consequences(Object consequences) {
            List<String> list = KubeStageMaps.stringList(consequences);
            for (String key : list) {
                if (key != null && !key.isBlank()) {
                    rule.consequence = key.trim().toLowerCase(java.util.Locale.ROOT);
                }
            }
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

        public ItemRestrictBuilder viewerHardcore() {
            rule.recipeViewer = "hardcore";
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

        /** When true (default), show Unfamiliar Item/Fish (or {@link #hiddenItemName}) instead of the real name. */
        public ItemRestrictBuilder hideItemName(boolean hide) {
            rule.hideItemName = hide;
            return this;
        }

        public ItemRestrictBuilder showRealName() {
            rule.hideItemName = false;
            return this;
        }

        /** Custom hidden display name (Kube Text/Component). Implies {@code hide_item_name}. */
        public ItemRestrictBuilder hiddenItemName(Component component) {
            if (component != null) {
                rule.hiddenItemNameComponent = component;
                rule.hideItemName = true;
            }
            return this;
        }

        /** Custom hidden display name as lang key or literal string (datapack-compatible). */
        public ItemRestrictBuilder hiddenItemName(String name) {
            if (name != null && !name.isBlank()) {
                rule.hiddenItemName = name.trim();
                rule.hideItemName = true;
            }
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
            if (b.inventoryTicksOverride >= 1) {
                rule.inventoryTicks = b.inventoryTicksOverride;
            }
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
            parts.add("priority=" + rule.priority);
            parts.add(rule.consequence);
            parts.add(rule.recipeViewer);
            parts.add(String.valueOf(rule.hideItemName));
            parts.add(String.valueOf(rule.containersWhitelist));
            parts.add(String.join(",", rule.items));
            parts.add(String.join(",", rule.exclude));
            parts.add(String.join(",", rule.otherCase));
            parts.add("canPickup=" + rule.canPickup);
            parts.add("invTicks=" + rule.inventoryTicks);
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
        /** {@code >= 1} when set by {@link #caseInventory(int)} / {@link #caseAll(int)}; applied to parent rule. */
        int inventoryTicksOverride = -1;

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

        public ItemBranchBuilder caseRightClick() {
            addOther("right_click");
            return this;
        }

        public ItemBranchBuilder caseLeftClick() {
            addOther("left_click");
            return this;
        }

        public ItemBranchBuilder caseMainHand() {
            addOther("main_hand");
            return this;
        }

        public ItemBranchBuilder caseOffHand() {
            addOther("off_hand");
            return this;
        }

        public ItemBranchBuilder caseInventory() {
            return caseInventory(StageItemHandler.DEFAULT_INVENTORY_TICKS);
        }

        public ItemBranchBuilder caseInventory(int ticks) {
            addOther(StageItemHandler.CASE_INVENTORY);
            inventoryTicksOverride = StageItemHandler.normalizeInventoryTicks(ticks);
            return this;
        }

        public ItemBranchBuilder cases(Object cases) {
            for (String key : KubeStageMaps.stringList(cases)) {
                addOther(key);
            }
            return this;
        }

        public ItemBranchBuilder caseAll() {
            return caseAll(StageItemHandler.DEFAULT_INVENTORY_TICKS);
        }

        public ItemBranchBuilder caseAll(int ticks) {
            ifRule.otherCase.clear();
            for (String s : StageItemHandler.ALL_OTHER_CASES) {
                ifRule.otherCase.add(s);
            }
            ifRule.otherCase.add(StageItemHandler.CASE_INVENTORY);
            inventoryTicksOverride = StageItemHandler.normalizeInventoryTicks(ticks);
            return this;
        }

        private void addOther(String key) {
            if (!ifRule.otherCase.contains(key)) {
                ifRule.otherCase.add(key);
            }
        }

        public ItemBranchBuilder consequenceDrop() {
            ifRule.consequence = "drop";
            return this;
        }

        public ItemBranchBuilder consequenceDelete() {
            ifRule.consequence = "delete";
            return this;
        }

        public ItemBranchBuilder consequenceBlock() {
            ifRule.consequence = "block";
            return this;
        }

        public ItemBranchBuilder consequenceBlockDrop() {
            ifRule.consequence = "block_drop";
            return this;
        }

        public ItemBranchBuilder consequenceBlockDelete() {
            ifRule.consequence = "block_delete";
            return this;
        }

        public ItemBranchBuilder consequences(Object consequences) {
            List<String> list = KubeStageMaps.stringList(consequences);
            for (String key : list) {
                if (key != null && !key.isBlank()) {
                    ifRule.consequence = key.trim().toLowerCase(java.util.Locale.ROOT);
                }
            }
            return this;
        }

        StageItemHandler.StageItemIfRule toIfRule() {
            return ifRule;
        }
    }
}
