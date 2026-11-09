package net.unfamily.anotherstages.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;

/**
 * Registers {@code AnotherStagesEvents} (preferred restriction registration),
 * {@code AnotherStages} query helpers (+ legacy managers for retrocompat).
 */
public final class AnotherStagesKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(AnotherStagesEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("AnotherStages", AnotherStagesBinding.INSTANCE);
    }

    @Override
    public void beforeScriptsLoaded(ScriptManager manager) {
        // Server scripts own declarative restrictions; clear only here so startup commits are not wiped.
        if (manager.scriptType == ScriptType.SERVER) {
            PendingScriptRestrictions.clear();
        }
    }

    @Override
    public void afterScriptsLoaded(ScriptManager manager) {
        if (manager.scriptType != ScriptType.SERVER) {
            return;
        }
        // Listeners registered during script eval; post so event-based packs enqueue builders.
        if (AnotherStagesEvents.RESTRICTIONS.hasListeners()) {
            AnotherStagesEvents.RESTRICTIONS.post(ScriptType.SERVER, new StageRestrictionsKubeEvent());
        }
        PendingScriptRestrictions.commitAll();
    }
}
