package net.unfamily.anotherstages.client.gui.stagegen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Edit box with a small suggestion strip under the field (Tab cycles completions).
 */
public final class SuggestingEditBox extends EditBox {
    private static final int MAX_VISIBLE = 6;

    private final Supplier<List<String>> poolSupplier;
    private final List<String> visibleSuggestions = new ArrayList<>();
    private String typedPrefix = "";

    public SuggestingEditBox(Font font, int width, int height, Component message, Supplier<List<String>> poolSupplier) {
        super(font, 0, 0, width, height, message);
        this.poolSupplier = poolSupplier;
        setResponder(s -> {
            typedPrefix = s == null ? "" : s;
            refreshSuggestions();
        });
    }

    public void refreshSuggestions() {
        visibleSuggestions.clear();
        List<String> matches = StageGenSuggestions.matching(poolSupplier.get(), typedPrefix);
        for (int i = 0; i < matches.size() && i < MAX_VISIBLE; i++) {
            visibleSuggestions.add(matches.get(i));
        }
    }

    public List<String> visibleSuggestions() {
        return visibleSuggestions;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_TAB && isFocused()) {
            boolean reverse = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
            String next = StageGenSuggestions.nextCompletion(
                    poolSupplier.get(),
                    typedPrefix,
                    getValue(),
                    reverse);
            if (next != null) {
                setValue(next);
                moveCursorToEnd(false);
                refreshSuggestions();
                return true;
            }
            return true;
        }
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        refreshSuggestions();
        return handled;
    }

    /** Draw suggestion strip under the box (call from screen render). */
    public void renderSuggestions(GuiGraphics graphics, Font font) {
        if (!isFocused() || visibleSuggestions.isEmpty()) {
            return;
        }
        int x = getX();
        int y = getY() + getHeight() + 2;
        int w = getWidth();
        int rowH = 10;
        int h = visibleSuggestions.size() * rowH + 2;
        graphics.fill(x, y, x + w, y + h, 0xEE1A1A1E);
        graphics.fill(x, y, x + w, y + 1, 0xFF5A5A68);
        for (int i = 0; i < visibleSuggestions.size(); i++) {
            String s = visibleSuggestions.get(i);
            boolean match = s.equalsIgnoreCase(getValue());
            graphics.drawString(font, s, x + 2, y + 2 + i * rowH, match ? 0xFF66CC66 : 0xFFE8E8F0, false);
        }
    }

    public static boolean hasShiftDown() {
        long window = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
