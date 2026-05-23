package work.nemonet.littlemaidneo.client.screen.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TextInputGUI extends GUIElement {
    private final Font font;
    private final int maxLength;
    private final Predicate<String> textPredicate;
    private final List<Consumer<String>> textChangeListeners = new ArrayList<>();

    private String text = "";
    private String placeholderText = "";
    private int cursorBlinkTicks = 0;
    private boolean showCursor = true;
    private boolean editable = true;
    private int textColor = 0xE0E0E0;
    private int disabledTextColor = 0x707070;
    private int placeholderColor = 0x808080;
    private int backgroundColor = 0xFF000000;
    private int borderColor = 0xFFA0A0A0;
    private int focusedBorderColor = 0xFFFFFFFF;

    public TextInputGUI(int x, int y, int width, int height) {
        this(x, y, width, height, 32);
    }

    public TextInputGUI(int x, int y, int width, int height, int maxLength) {
        this(x, y, width, height, maxLength, s -> true);
    }

    public TextInputGUI(int x, int y, int width, int height, int maxLength, Predicate<String> textPredicate) {
        super(width, height);
        this.x = x;
        this.y = y;
        this.font = Minecraft.getInstance().font;
        this.maxLength = maxLength;
        this.textPredicate = textPredicate;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        cursorBlinkTicks++;
        if (cursorBlinkTicks >= 30) {
            showCursor = !showCursor;
            cursorBlinkTicks = 0;
        }

        int borderColorToUse = isFocused() ? focusedBorderColor : borderColor;
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, borderColorToUse);
        context.fill(x, y, x + width, y + height, backgroundColor);

        int textY = y + (height - font.lineHeight) / 2;
        String displayText = text;
        int textColorToUse = editable ? textColor : disabledTextColor;

        if (displayText.isEmpty() && !placeholderText.isEmpty() && !isFocused()) {
            context.drawString(font, placeholderText, x + 4, textY, placeholderColor, false);
        } else if (!displayText.isEmpty()) {
            int textWidth = width - 8;
            String visibleText = getVisibleText(displayText, textWidth);
            context.drawString(font, visibleText, x + 4, textY, textColorToUse, false);
            if (isFocused() && showCursor && editable) {
                int cursorX = x + 4 + font.width(visibleText);
                context.fill(cursorX, textY - 1, cursorX + 1, textY + font.lineHeight - 1, 0xFFD0D0D0);
            }
        } else if (isFocused() && showCursor && editable) {
            context.fill(x + 4, textY - 1, x + 5, textY + font.lineHeight - 1, 0xFFD0D0D0);
        }
    }

    private String getVisibleText(String fullText, int maxWidth) {
        String visibleText = fullText;
        while (font.width(visibleText) > maxWidth && !visibleText.isEmpty()) {
            visibleText = visibleText.substring(1);
        }
        return visibleText;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            setFocused(isMouseOver(mouseX, mouseY));
            return isFocused();
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused() || !editable) return false;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!text.isEmpty()) {
                setText(text.substring(0, text.length() - 1));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!isFocused() || !editable) return false;
        if (chr >= 32 && chr != 127 && text.length() < maxLength) {
            String newText = text + chr;
            if (textPredicate.test(newText)) {
                setText(newText);
            }
            return true;
        }
        return false;
    }

    public void setText(String text) {
        if (textPredicate.test(text) && text.length() <= maxLength) {
            String oldText = this.text;
            this.text = text;
            resetCursorBlink();
            if (!oldText.equals(text)) {
                notifyTextChanged();
            }
        }
    }

    public String getText() {
        return text;
    }

    public void setPlaceholder(String placeholderText) {
        this.placeholderText = placeholderText;
    }

    private void resetCursorBlink() {
        cursorBlinkTicks = 0;
        showCursor = true;
    }

    public void addTextChangeListener(Consumer<String> listener) {
        textChangeListeners.add(listener);
    }

    public void removeTextChangeListener(Consumer<String> listener) {
        textChangeListeners.remove(listener);
    }

    private void notifyTextChanged() {
        for (Consumer<String> listener : textChangeListeners) {
            listener.accept(text);
        }
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isEditable() {
        return editable;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
