package net.unfamily.anotherstages.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/** Registers query-only stage bindings for pack scripts. */
public final class AnotherStagesKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("AnotherStages", StageBindings.class);
        bindings.add("Stages", StageBindings.class);
    }
}
