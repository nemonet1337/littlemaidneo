package work.nemonet.littlemaidneo.client.screen.component;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FilterableListGUI<T extends GUIElement> extends GUIElement {
    private final TextInputGUI searchInput;
    private final ScrollableListGUI<T> listGUI;
    private final FilterPredicate<T> filterPredicate;

    private final List<T> allItems;
    private final List<T> filteredItems;

    private final int searchInputHeight;
    private final int elementW;

    public FilterableListGUI(int x, int y, int width, int height,
                             int elementW, int elementH,
                             Collection<T> items,
                             FilterPredicate<T> filterPredicate,
                             ScrollableListGUI.ScrollBarConfig scrollBarConfig,
                             int searchInputHeight) {
        super(width, height);
        this.x = x;
        this.y = y;
        this.filterPredicate = filterPredicate;
        this.allItems = new ArrayList<>(items);
        this.filteredItems = new ArrayList<>(items);
        this.searchInputHeight = searchInputHeight;
        this.elementW = elementW;

        int widthStack = Math.max(1, width / elementW);
        int listHeight = height - searchInputHeight;
        int heightStack = Math.max(1, listHeight / elementH);

        int searchY = y + listHeight;

        this.searchInput = new TextInputGUI(x, searchY, width, searchInputHeight);
        this.searchInput.addTextChangeListener(this::onFilterTextChanged);

        if (scrollBarConfig != null) {
            this.listGUI = new ScrollableListGUI<>(
                    x, y, widthStack, heightStack, elementW, elementH,
                    filteredItems, scrollBarConfig
            );
        } else {
            this.listGUI = new ScrollableListGUI<>(
                    x, y, widthStack, heightStack, elementW, elementH,
                    filteredItems, false
            );
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        searchInput.extractRenderState(context, mouseX, mouseY, delta);
        listGUI.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void setPos(int x, int y) {
        super.setPos(x, y);
        int listHeight = height - searchInputHeight;
        searchInput.setPos(x, y + listHeight);
        listGUI.setPos(x, y);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (searchInput.mouseClicked(event, handled)) return true;
        return listGUI.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean a = searchInput.mouseReleased(event);
        boolean b = listGUI.mouseReleased(event);
        return a || b;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (searchInput.mouseDragged(event, deltaX, deltaY)) return true;
        return listGUI.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return listGUI.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (searchInput.isFocused()) return searchInput.keyPressed(event);
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchInput.isFocused()) return searchInput.charTyped(event);
        return super.charTyped(event);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) searchInput.setFocused(false);
    }

    private void onFilterTextChanged(String filterText) {
        updateFilteredItems(filterText);
        updateListGUI();
    }

    private void updateFilteredItems(String filterText) {
        filteredItems.clear();
        if (filterText == null || filterText.trim().isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            filteredItems.addAll(
                    allItems.stream()
                            .filter(item -> filterPredicate.test(item, filterText))
                            .toList()
            );
        }
    }

    private void updateListGUI() {
        listGUI.setElements(filteredItems);
        listGUI.setScroll(0);
    }

    public Optional<T> getSelectedItem() {
        return listGUI.getSelectElement();
    }

    public void setItems(Collection<T> newItems) {
        allItems.clear();
        allItems.addAll(newItems);
        onFilterTextChanged(searchInput.getText());
    }

    public void addItem(T item) {
        allItems.add(item);
        onFilterTextChanged(searchInput.getText());
    }

    public void removeItem(T item) {
        allItems.remove(item);
        onFilterTextChanged(searchInput.getText());
    }

    public void clearItems() {
        allItems.clear();
        onFilterTextChanged(searchInput.getText());
    }

    public String getFilterText() {
        return searchInput.getText();
    }

    public void setFilterText(String text) {
        searchInput.setText(text);
    }

    public void setPlaceholder(String placeholderText) {
        searchInput.setPlaceholder(placeholderText);
    }

    public int getFilteredItemCount() {
        return filteredItems.size();
    }

    public int getTotalItemCount() {
        return allItems.size();
    }

    public boolean hasScrollBar() {
        return listGUI.hasScrollBar();
    }

    public ScrollableListGUI<T> getListGUI() {
        return listGUI;
    }

    public TextInputGUI getSearchInput() {
        return searchInput;
    }

    public boolean setSelectedItem(T item) {
        int index = filteredItems.indexOf(item);
        if (index >= 0) return setSelectedIndex(index);
        return false;
    }

    public boolean setSelectedItemBy(Predicate<T> predicate) {
        for (int i = 0; i < filteredItems.size(); i++) {
            if (predicate.test(filteredItems.get(i))) return setSelectedIndex(i);
        }
        return false;
    }

    public void setSelectedItemBy(Predicate<T> predicate, Consumer<T> consumer) {
        for (int i = 0; i < filteredItems.size(); i++) {
            if (predicate.test(filteredItems.get(i))) {
                consumer.accept(filteredItems.get(i));
                setSelectedIndex(i);
                return;
            }
        }
    }

    private boolean setSelectedIndex(int index) {
        if (index < 0 || index >= filteredItems.size()) return false;

        listGUI.getAllElements().forEach(element -> {
            if (element instanceof ListGUIElement) {
                ((ListGUIElement) element).setSelected(false);
            }
        });

        T selectedItem = filteredItems.get(index);
        if (selectedItem instanceof ListGUIElement) {
            ((ListGUIElement) selectedItem).setSelected(true);
        }

        int widthStack = Math.max(1, width / elementW);
        int scrollRow = index / widthStack;
        if (scrollRow < listGUI.size()) {
            listGUI.setScroll(scrollRow);
        } else if (scrollRow >= listGUI.size()) {
            listGUI.setScroll(listGUI.size());
        }

        return true;
    }

    public static <T extends GUIElement> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T extends GUIElement> {
        private int x = 0, y = 0;
        private int width = 200, height = 150;
        private int elementW = 200, elementH = 30;
        private Collection<T> items = new ArrayList<>();
        private FilterPredicate<T> filterPredicate = FilterPredicate.containsIgnoreCase();
        private ScrollableListGUI.ScrollBarConfig scrollBarConfig = null;
        private int searchInputHeight = 20;
        private String placeholderText = "";

        public Builder<T> position(int x, int y) {
            this.x = x; this.y = y; return this;
        }

        public Builder<T> size(int width, int height) {
            this.width = width; this.height = height; return this;
        }

        public Builder<T> elementSize(int elementW, int elementH) {
            this.elementW = elementW; this.elementH = elementH; return this;
        }

        public Builder<T> items(Collection<T> items) {
            this.items = items; return this;
        }

        public Builder<T> filterBy(FilterPredicate<T> filterPredicate) {
            this.filterPredicate = filterPredicate; return this;
        }

        public Builder<T> withScrollBar() {
            this.scrollBarConfig = ScrollBarStyle.DEFAULT.getConfig(this.height); return this;
        }

        public Builder<T> withScrollBar(ScrollableListGUI.ScrollBarConfig config) {
            this.scrollBarConfig = config; return this;
        }

        public Builder<T> withoutScrollBar() {
            this.scrollBarConfig = null; return this;
        }

        public Builder<T> withPlaceholder(String placeholderText) {
            this.placeholderText = placeholderText; return this;
        }

        public Builder<T> searchInputHeight(int height) {
            this.searchInputHeight = height; return this;
        }

        public FilterableListGUI<T> build() {
            FilterableListGUI<T> gui = new FilterableListGUI<>(
                    x, y, width, height, elementW, elementH,
                    items, filterPredicate, scrollBarConfig, searchInputHeight
            );
            if (!placeholderText.isEmpty()) {
                gui.setPlaceholder(placeholderText);
            }
            return gui;
        }
    }
}
