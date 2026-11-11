package net.unfamily.anotherstages.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/**
 * KubeJS event group {@code AnotherStagesEvents}.
 * Prefer {@code AnotherStagesEvents.restrictions(...)} to register stage gates.
 * {@code stageChanged} is the script equivalent of Library Stage Actions (react on add/remove).
 * Stage query helpers stay on the {@code AnotherStages} binding for {@code if}-style checks.
 */
public interface AnotherStagesEvents {
    EventGroup GROUP = EventGroup.of("AnotherStagesEvents");

    /**
     * Server scripts: register item / recipe / block / gui restrictions.
     * Fired once after {@code server_scripts} load (before script rules commit).
     */
    EventHandler RESTRICTIONS = GROUP.server("restrictions", () -> StageRestrictionsKubeEvent.class);

    /**
     * Server scripts: react when a Library stage is added or removed (player / world / team).
     * Runtime event (not fired at script load). Requires Another Stages + KubeJS.
     */
    EventHandler STAGE_CHANGED = GROUP.server("stageChanged", () -> StageChangedKubeEvent.class);
}
