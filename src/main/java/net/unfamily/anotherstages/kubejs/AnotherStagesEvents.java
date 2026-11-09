package net.unfamily.anotherstages.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/**
 * KubeJS event group {@code AnotherStagesEvents}.
 * Prefer {@code AnotherStagesEvents.restrictions(...)} to register stage gates.
 * Stage query helpers stay on the {@code AnotherStages} binding for {@code if}-style checks.
 */
public interface AnotherStagesEvents {
    EventGroup GROUP = EventGroup.of("AnotherStagesEvents");

    /**
     * Server scripts: register item / recipe / block / gui restrictions.
     * Fired once after {@code server_scripts} load (before script rules commit).
     */
    EventHandler RESTRICTIONS = GROUP.server("restrictions", () -> StageRestrictionsKubeEvent.class);
}
