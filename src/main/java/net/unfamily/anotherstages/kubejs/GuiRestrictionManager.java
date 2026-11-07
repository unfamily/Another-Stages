package net.unfamily.anotherstages.kubejs;

import net.minecraft.network.chat.Component;
import net.unfamily.anotherstages.gui.StageGuiHandler;
import net.unfamily.anotherstages.gui.StageGuiRule;
import net.unfamily.anotherstages.recipe.StageCondition;

import java.util.ArrayList;
import java.util.List;

/** {@code AnotherStages.guis.restrict()…} */
public final class GuiRestrictionManager {
    public GuiRestrictBuilder restrict() {
        return new GuiRestrictBuilder();
    }

    public static final class GuiRestrictBuilder {
        private String explicitId;
        private final StageGuiRule rule = new StageGuiRule();
        private boolean enqueued;

        GuiRestrictBuilder() {
            enqueue();
        }

        private void enqueue() {
            if (enqueued) {
                return;
            }
            enqueued = true;
            PendingScriptRestrictions.enqueue(this::commit);
        }

        public GuiRestrictBuilder id(String id) {
            this.explicitId = id;
            return this;
        }

        public GuiRestrictBuilder logicAnd() {
            rule.stagesLogic = "AND";
            return this;
        }

        public GuiRestrictBuilder logicOr() {
            rule.stagesLogic = "OR";
            return this;
        }

        public GuiRestrictBuilder stages(Object stages) {
            KubeStageMaps.fillRecipeStages(stages, rule.stages);
            return this;
        }

        public GuiRestrictBuilder menus(Object menus) {
            rule.menus.clear();
            rule.menus.addAll(KubeStageMaps.stringList(menus));
            return this;
        }

        public GuiRestrictBuilder menuTypes(Object types) {
            rule.menuTypes.clear();
            rule.menuTypes.addAll(KubeStageMaps.stringList(types));
            return this;
        }

        public GuiRestrictBuilder message(String message) {
            if (message != null) {
                rule.message = message;
            }
            return this;
        }

        public GuiRestrictBuilder message(Component component) {
            if (component != null) {
                rule.message = component.getString();
            }
            return this;
        }

        public GuiRestrictBuilder blockOpen(boolean block) {
            rule.blockOpen = block;
            return this;
        }

        public GuiRestrictBuilder blockedSlots(Object slots) {
            rule.blockedSlots.clear();
            rule.blockedSlots.addAll(KubeStageMaps.intList(slots));
            return this;
        }

        public GuiRestrictBuilder blockedItems(Object items) {
            rule.blockedItems.clear();
            rule.blockedItems.addAll(KubeStageMaps.stringList(items));
            return this;
        }

        public GuiRestrictBuilder maxSlots(int max) {
            rule.maxSlots = Math.max(0, max);
            return this;
        }

        private void commit() {
            String id = explicitId != null && !explicitId.isBlank()
                    ? explicitId
                    : ScriptIdAllocator.allocate("gui", fingerprint(), StageGuiHandler::hasRuleId);
            StageGuiHandler.putScriptRule(id, rule);
        }

        private String fingerprint() {
            List<String> parts = new ArrayList<>();
            parts.add(rule.stagesLogic);
            parts.add(String.valueOf(rule.blockOpen));
            parts.add(String.valueOf(rule.maxSlots));
            parts.add(rule.message);
            parts.add(String.join(",", rule.menus));
            parts.add(String.join(",", rule.menuTypes));
            parts.add(String.join(",", rule.blockedItems));
            for (Integer s : rule.blockedSlots) {
                parts.add("slot=" + s);
            }
            for (StageCondition c : rule.stages) {
                parts.add(c.stageType + ":" + c.stage + ":" + c.is);
            }
            return ScriptIdAllocator.fingerprint(parts.toArray(String[]::new));
        }
    }
}
