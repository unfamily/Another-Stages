package net.unfamily.anotherstages.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;

/**
 * Payload for {@code AnotherStagesEvents.restrictions}.
 * Fluent managers match the legacy {@code AnotherStages.items/recipes/blocks/guis} binding surface.
 */
public final class StageRestrictionsKubeEvent implements KubeEvent {
    public final ItemRestrictionManager items = new ItemRestrictionManager();
    public final RecipeRestrictionManager recipes = new RecipeRestrictionManager();
    public final BlockRestrictionManager blocks = new BlockRestrictionManager();
    public final GuiRestrictionManager guis = new GuiRestrictionManager();
}
