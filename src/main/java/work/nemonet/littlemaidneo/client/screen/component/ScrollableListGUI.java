package work.nemonet.littlemaidneo.client.screen.component;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import work.nemonet.littlemaidneo.client.screen.ModelSelectScreen;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

public class ScrollableListGUI<T extends GUIElement> extends MutableListGUI<T> {
    @Nullable
    private final MutableScrollBar scrollBar;

    public ScrollableListGUI(int x, int y, int widthStack, int heightStack,
                             int elementW, int elementH, Collection<T> elements,
                             boolean enableScrollBar) {
        super(x, y, widthStack, heightStack, elementW, elementH, elements);
        this.scrollBar = enableScrollBar ? createDefaultScrollBar() : null;
    }

    public ScrollableListGUI(int x, int y, int widthStack, int heightStack,
                             int elementW, int elementH, Collection<T> elements,
                             ScrollBarConfig scrollBarConfig) {
        super(x, y, widthStack, heightStack, elementW, elementH, elements);
        this.scrollBar = createScrollBar(scrollBarConfig);
    }

    private MutableScrollBar createDefaultScrollBar() {
        return new MutableScrollBar(
                this.x + this.width + 10, this.y,
                8, this.height, calculateScrollBarSize(),
                new TextureAddress(0, 200, 8, 8, 256, 256),
                new TextureAddress(0, 208, 8, 8, 256, 256),
                new TextureAddress(0, 216, 8, 8, 256, 256),
                new TextureAddress(0, 224, 10, 6, 256, 256),
                ModelSelectScreen.MODEL_SELECT_GUI_TEXTURE
        );
    }

    private MutableScrollBar createScrollBar(ScrollBarConfig config) {
        return new MutableScrollBar(
                this.x + this.width + config.offsetX(), this.y + config.offsetY(),
                config.width(), config.height(), calculateScrollBarSize(),
                config.sliderT(), config.sliderM(), config.sliderB(), config.pointer(),
                config.texture()
        );
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (scrollBar != null) {
            scrollBar.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollBar != null && scrollBar.mouseClicked(mouseX, mouseY, button)) {
            syncScrollFromScrollBar();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean result = false;
        if (scrollBar != null && scrollBar.mouseReleased(mouseX, mouseY, button)) {
            syncScrollFromScrollBar();
            result = true;
        }
        return result | super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (scrollBar != null && scrollBar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            syncScrollFromScrollBar();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (scrollBar != null && scrollBar.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
            syncScrollFromScrollBar();
            return true;
        }
        boolean result = super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        if (result) syncScrollToScrollBar();
        return result;
    }

    @Override
    public void setScroll(int scroll) {
        super.setScroll(scroll);
        syncScrollToScrollBar();
    }

    @Override
    public void setElements(Collection<T> newElements) {
        super.setElements(newElements);
        updateScrollBarSize();
    }

    @Override
    public void addElement(T element) {
        super.addElement(element);
        updateScrollBarSize();
    }

    @Override
    public void removeElement(T element) {
        super.removeElement(element);
        updateScrollBarSize();
    }

    @Override
    public void removeElementAt(int index) {
        super.removeElementAt(index);
        updateScrollBarSize();
    }

    @Override
    public void clearElements() {
        super.clearElements();
        updateScrollBarSize();
    }

    private void syncScrollFromScrollBar() {
        if (scrollBar != null) {
            int newScroll = scrollBar.getPoint();
            if (newScroll != this.scroll) {
                int totalRows = (size() + widthStack - 1) / widthStack;
                int maxScroll = Math.max(0, totalRows - heightStack);
                this.scroll = Mth.clamp(newScroll, 0, maxScroll);
            }
        }
    }

    private void syncScrollToScrollBar() {
        if (scrollBar != null && scrollBar.getPoint() != this.scroll) {
            scrollBar.setPoint(this.scroll);
        }
    }

    private void updateScrollBarSize() {
        if (scrollBar != null) {
            scrollBar.setElemSize(calculateScrollBarSize());
            syncScrollToScrollBar();
        }
    }

    public boolean hasScrollBar() {
        return scrollBar != null;
    }

    public Optional<MutableScrollBar> getScrollBar() {
        return Optional.ofNullable(scrollBar);
    }

    private int calculateScrollBarSize() {
        if (size() == 0) return 1;
        int totalRows = (size() + widthStack - 1) / widthStack;
        int maxScrollableRows = Math.max(0, totalRows - heightStack);
        return Math.max(1, maxScrollableRows + 1);
    }

    public record ScrollBarConfig(
            int offsetX, int offsetY, int width, int height,
            TextureAddress sliderT, TextureAddress sliderM,
            TextureAddress sliderB, TextureAddress pointer,
            ResourceLocation texture
    ) {
        public static ScrollBarConfig defaultConfig() {
            return new ScrollBarConfig(
                    4, 0, 8, 200,
                    new TextureAddress(0, 200, 8, 8, 256, 256),
                    new TextureAddress(0, 208, 8, 8, 256, 256),
                    new TextureAddress(0, 216, 8, 8, 256, 256),
                    new TextureAddress(0, 224, 10, 6, 256, 256),
                    ModelSelectScreen.MODEL_SELECT_GUI_TEXTURE
            );
        }
    }
}
