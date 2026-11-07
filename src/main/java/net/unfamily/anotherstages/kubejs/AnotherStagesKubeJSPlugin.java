package net.unfamily.anotherstages.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;

/** Registers {@code AnotherStages} fluent managers + query helpers. */
public final class AnotherStagesKubeJSPlugin implements KubeJSPlugin {
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
        if (manager.scriptType == ScriptType.SERVER) {
            PendingScriptRestrictions.commitAll();
        }
    }
}
