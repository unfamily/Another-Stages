package net.unfamily.anotherstages.client.gui.stagegen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.unfamily.anotherstages.client.gui.AsButton;
import net.unfamily.anotherstages.client.gui.AsToggle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * Scrollable ACM-style field list for Stage Generator (form / table / hub rows).
 * Explicitly supports click + drag on the scrollbar (container screens otherwise steal drag).
 */
public final class StageGenScrollList extends ContainerObjectSelectionList<StageGenScrollList.Entry> {
    public static final int ROW_H = 24;
    public static final int TALL_ROW_H = 36;

    private final Font font;
    private final int listWidth;
    private boolean draggingScrollbar;

    public StageGenScrollList(Minecraft minecraft, Font font, int width, int height, int y) {
        super(minecraft, width, height, y, ROW_H);
        this.font = font;
        this.listWidth = width;
        this.centerListVertically = false;
    }

    @Override
    public int getRowWidth() {
        return Math.max(80, listWidth - 16);
    }

    /** True while the user is dragging the scrollbar thumb/track. */
    public boolean isDraggingScrollbar() {
        return draggingScrollbar;
    }

    public boolean isOverScrollbar(double mouseX, double mouseY) {
        int barX = getScrollbarPosition();
        return mouseX >= barX
                && mouseX <= barX + SCROLLBAR_WIDTH
                && mouseY >= getY()
                && mouseY < getY() + getHeight();
    }

    /**
     * Begin scrollbar interaction: jump thumb toward click, then allow hold-drag.
     * @return true if the event was on the scrollbar
     */
    public boolean beginScrollbarDrag(double mouseX, double mouseY, int button) {
        if (!scrollbarVisible() || !isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = false;
            return false;
        }
        draggingScrollbar = true;
        jumpScrollToMouse(mouseY);
        return true;
    }

    public void endScrollbarDrag() {
        draggingScrollbar = false;
    }

    private int scrollerHeight() {
        int maxPos = getMaxPosition();
        if (maxPos <= 0) {
            return 32;
        }
        int h = (int) ((float) (getHeight() * getHeight()) / maxPos);
        return Mth.clamp(h, 32, getHeight() - 8);
    }

    private void jumpScrollToMouse(double mouseY) {
        int max = getMaxScroll();
        if (max <= 0) {
            return;
        }
        int thumb = scrollerHeight();
        int track = Math.max(1, getHeight() - thumb);
        double rel = (mouseY - getY() - thumb / 2.0) / track;
        setScrollAmount(Mth.clamp(rel, 0.0, 1.0) * max);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (beginScrollbarDrag(mouseX, mouseY, button)) {
            return true;
        }
        draggingScrollbar = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingScrollbar) {
            // Absolute follow (hold-drag): thumb tracks mouse Y even if cursor leaves the bar.
            jumpScrollToMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean was = draggingScrollbar;
        endScrollbarDrag();
        return super.mouseReleased(mouseX, mouseY, button) || was;
    }

    public void addLabel(Component text) {
        addEntry(new LabelEntry(text));
    }

    public void addLabel(String key) {
        addLabel(Component.translatable(key));
    }

    /** Long copy that wraps to the row width (close hint, prefer message, …). */
    public void addWrappedLabel(Component text) {
        int wrapWidth = Math.max(40, getRowWidth());
        for (FormattedCharSequence line : font.split(text, wrapWidth)) {
            addEntry(new WrappedLineEntry(line));
        }
    }

    public void addWrappedLabel(String key) {
        addWrappedLabel(Component.translatable(key));
    }

    public EditBox addTextField(String labelKey, String initial, Consumer<String> onChange) {
        TextEntry entry = new TextEntry(labelKey, initial, onChange);
        addEntry(entry);
        return entry.box;
    }

    public void addToggle(String labelKey, BooleanSupplier get, Consumer<Boolean> set) {
        addEntry(new ToggleEntry(labelKey, get, set));
    }

    public void addCycle(String labelKey, Supplier<String> get, IntConsumer cycleDelta) {
        addEntry(new CycleEntry(labelKey, get, cycleDelta));
    }

    public void addActionButton(Component label, Runnable action) {
        addEntry(new ButtonEntry(label, action));
    }

    public void addWidgets(AbstractWidget... widgets) {
        addEntry(new WidgetsEntry(List.of(widgets)));
    }

    public void addWidgetsTall(AbstractWidget... widgets) {
        addEntry(new WidgetsEntry(List.of(widgets)));
    }

    /** Browse row: icon + title + Ready/Incomplete badge; click opens edit. */
    public void addBrowseRow(
            ItemStack icon,
            @Nullable String glyph,
            Component title,
            Component badge,
            boolean ready,
            Runnable onClick
    ) {
        addEntry(new BrowseRowEntry(icon, glyph, title, badge, ready, onClick));
    }

    /** Matcher/exclude row: icon + id text + Remove. */
    public void addMatcherRow(ItemStack icon, @Nullable String glyph, String id, Runnable onRemove) {
        addEntry(new MatcherRowEntry(icon, glyph, id, onRemove));
    }

    public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {}

    private final class LabelEntry extends Entry {
        private final StringWidget label;

        LabelEntry(Component text) {
            this.label = new StringWidget(getRowWidth(), 12, text, font);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float a) {
            label.setPosition(left, top + 4);
            label.setWidth(getRowWidth());
            label.render(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(label);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(label);
        }
    }

    private final class WrappedLineEntry extends Entry {
        private final FormattedCharSequence line;

        WrappedLineEntry(FormattedCharSequence line) {
            this.line = line;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            graphics.drawString(font, line, left, top + 4, 0xFFE8E8F0, false);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    private final class TextEntry extends Entry {
        private final StringWidget label;
        private final EditBox box;

        TextEntry(String labelKey, String initial, Consumer<String> onChange) {
            this.label = new StringWidget(getRowWidth(), 10, Component.translatable(labelKey), font);
            this.box = new EditBox(font, 0, 0, getRowWidth(), 18, Component.translatable(labelKey));
            this.box.setMaxLength(512);
            this.box.setValue(initial == null ? "" : initial);
            this.box.setResponder(onChange);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float a) {
            int x = left;
            int y = top;
            label.setPosition(x, y);
            label.setWidth(getRowWidth());
            label.render(graphics, mouseX, mouseY, a);
            box.setWidth(getRowWidth());
            box.setPosition(x, y + 12);
            box.render(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(box);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(box);
        }
    }

    private final class ToggleEntry extends Entry {
        private final StringWidget label;
        private final AsToggle toggle;

        ToggleEntry(String labelKey, BooleanSupplier get, Consumer<Boolean> set) {
            this.label = new StringWidget(160, 12, Component.translatable(labelKey), font);
            this.toggle = new AsToggle(get, set);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float a) {
            int x = left;
            int y = top;
            label.setPosition(x, y + 4);
            label.render(graphics, mouseX, mouseY, a);
            toggle.setPosition(x + getRowWidth() - AsToggle.WIDTH, y + (ROW_H - AsToggle.HEIGHT) / 2);
            toggle.render(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(toggle);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(toggle);
        }
    }

    private final class CycleEntry extends Entry {
        private final StringWidget label;
        private final AsButton button;
        private final Supplier<String> get;

        CycleEntry(String labelKey, Supplier<String> get, IntConsumer cycleDelta) {
            this.label = new StringWidget(100, 12, Component.translatable(labelKey), font);
            this.get = get;
            this.button = AsButton.cycle(110, AsButton.HEIGHT, Component.literal(get.get()), cycleDelta);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float a) {
            int x = left;
            int y = top;
            label.setPosition(x, y + 4);
            label.render(graphics, mouseX, mouseY, a);
            button.setMessage(Component.literal(get.get()));
            button.setPosition(x + getRowWidth() - button.getWidth(), y + (ROW_H - AsButton.HEIGHT) / 2);
            button.render(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(button);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(button);
        }
    }

    private final class ButtonEntry extends Entry {
        private final AsButton button;

        ButtonEntry(Component label, Runnable action) {
            this.button = AsButton.text(Math.min(220, getRowWidth()), label, action);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float a) {
            button.setWidth(Math.min(220, getRowWidth()));
            button.setPosition(left, top + (ROW_H - AsButton.HEIGHT) / 2);
            button.render(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(button);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(button);
        }
    }

    private final class WidgetsEntry extends Entry {
        private final List<AbstractWidget> widgets;

        WidgetsEntry(List<AbstractWidget> widgets) {
            this.widgets = new ArrayList<>(widgets);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float a) {
            int x = left;
            int y = top + 2;
            int cursor = x;
            for (AbstractWidget w : widgets) {
                w.setPosition(cursor, y);
                w.render(graphics, mouseX, mouseY, a);
                cursor += w.getWidth() + 4;
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }
    }

    private final class BrowseRowEntry extends Entry {
        private final ItemStack icon;
        private final @Nullable String glyph;
        private final StringWidget badge;
        private final AsButton open;
        private final boolean ready;

        BrowseRowEntry(
                ItemStack icon,
                @Nullable String glyph,
                Component titleText,
                Component badgeText,
                boolean ready,
                Runnable onClick
        ) {
            this.icon = icon == null ? ItemStack.EMPTY : icon;
            this.glyph = glyph;
            this.ready = ready;
            this.badge = new StringWidget(64, 12, badgeText, font);
            this.open = AsButton.text(80, 14, titleText, onClick);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float a) {
            int x = left;
            int y = top;
            int w = getRowWidth();
            graphics.fill(x, y, x + w, y + ROW_H - 2, hovering ? 0x553A3A44 : 0x332C2C32);
            int iconX = x + 2;
            int iconY = y + 3;
            graphics.fill(iconX, iconY, iconX + 18, iconY + 18, 0xFF2C2C32);
            if (!icon.isEmpty()) {
                graphics.renderItem(icon, iconX + 1, iconY + 1);
                graphics.renderItemDecorations(font, icon, iconX + 1, iconY + 1);
            } else if (glyph != null) {
                graphics.drawString(font, glyph, iconX + 6, iconY + 5, 0xFFE8E8F0, false);
            }
            final int badgeW = 64;
            final int gap = 4;
            int titleW = Math.max(40, w - 24 - badgeW - gap);
            open.setWidth(titleW);
            open.setPosition(x + 24, y + 4);
            open.render(graphics, mouseX, mouseY, a);
            badge.setWidth(badgeW);
            badge.setMessage(Component.translatable(ready
                            ? "screen.another_stages.stage_generator.complete.ready"
                            : "screen.another_stages.stage_generator.complete.incomplete")
                    .withStyle(s -> s.withColor(ready ? 0x66CC66 : 0xCC8866)));
            badge.setPosition(x + w - badgeW, y + 6);
            badge.render(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(open);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(open);
        }
    }

    private final class MatcherRowEntry extends Entry {
        private final ItemStack icon;
        private final @Nullable String glyph;
        private final StringWidget label;
        private final AsButton remove;

        MatcherRowEntry(ItemStack icon, @Nullable String glyph, String id, Runnable onRemove) {
            this.icon = icon == null ? ItemStack.EMPTY : icon;
            this.glyph = glyph;
            String shown = id == null || id.isBlank() ? "…" : id;
            this.label = new StringWidget(140, 12, Component.literal(shown), font);
            this.remove = AsButton.text(
                    56,
                    14,
                    Component.translatable("screen.another_stages.stage_generator.table.remove"),
                    onRemove);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float a) {
            int x = left;
            int y = top;
            int iconX = x + 2;
            int iconY = y + 3;
            graphics.fill(iconX, iconY, iconX + 18, iconY + 18, 0xFF2C2C32);
            if (!icon.isEmpty()) {
                graphics.renderItem(icon, iconX + 1, iconY + 1);
                graphics.renderItemDecorations(font, icon, iconX + 1, iconY + 1);
            } else if (glyph != null) {
                graphics.drawString(font, glyph, iconX + 6, iconY + 5, 0xFFE8E8F0, false);
            }
            label.setPosition(x + 24, y + 6);
            label.setWidth(Math.max(40, getRowWidth() - 90));
            label.render(graphics, mouseX, mouseY, a);
            remove.setPosition(x + getRowWidth() - remove.getWidth(), y + 4);
            remove.render(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(remove);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(remove);
        }
    }
}
