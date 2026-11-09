package net.unfamily.anotherstages.client.gui.stagegen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * EditBox with Tab / Shift+Tab prefix completion from a suggestion pool.
 */
public final class SuggestingEditBox extends EditBox {
    private final Supplier<List<String>> poolSupplier;
    private String typedPrefix = "";
    private final List<String> visibleSuggestions = new ArrayList<>();
    private static final int MAX_VISIBLE = 6;

    public SuggestingEditBox(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            Supplier<List<String>> poolSupplier
    ) {
        super(font, x, y, width, height, message);
        this.poolSupplier = poolSupplier;
        super.setResponder(this::onTyped);
    }

    @Override
    public void setResponder(Consumer<String> responder) {
        super.setResponder(value -> {
            onTyped(value);
            if (responder != null) {
                responder.accept(value);
            }
        });
    }

    private void onTyped(String value) {
        typedPrefix = value == null ? "" : value;
        refreshSuggestions();
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
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_TAB && isFocused()) {
            boolean reverse = event.hasShiftDown();
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
        boolean handled = super.keyPressed(event);
        refreshSuggestions();
        return handled;
    }

    /** Draw suggestion strip under the box (call from screen extract). */
    public void extractSuggestions(GuiGraphicsExtractor graphics, Font font) {
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
            graphics.text(font, s, x + 2, y + 2 + i * rowH, match ? 0xFF66CC66 : 0xFFE8E8F0, false);
        }
    }

    public static boolean hasShift(@Nullable KeyEvent event) {
        return event != null && event.hasShiftDown();
    }
}
