package net.unfamily.anotherstages.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Shared UI helpers for Another Stages screens (ACM-style click sounds).
 */
public final class AsUi {
    private AsUi() {}

    public static void playClick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
        }
    }

    public static void playClick(float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
        }
    }
}
