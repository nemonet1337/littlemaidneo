package work.nemonet.littlemaidneo.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.client.screen.component.FilterPredicate;
import work.nemonet.littlemaidneo.client.screen.component.FilterableListGUI;
import work.nemonet.littlemaidneo.client.screen.component.GUIElement;
import work.nemonet.littlemaidneo.client.screen.component.ListGUIElement;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayable;
import work.nemonet.littlemaidneo.network.NetworkHandler;
import work.nemonet.littlemaidneo.resource.holder.ConfigHolder;
import work.nemonet.littlemaidneo.resource.manager.LMConfigManager;

import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class SoundPackSelectScreen<T extends Entity & SoundPlayable> extends Screen {
    public static final ResourceLocation MODEL_SELECT_GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(LittleMaidNeo.MODID, "textures/gui/model_select.png");
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 196;
    private final T entity;
    private FilterableListGUI<SoundPackGUI> soundPackListGUI;

    public SoundPackSelectScreen(Component titleIn, T owner) {
        super(titleIn);
        this.entity = owner;
    }

    @Override
    protected void init() {
        int scale = 15;
        int allColor = 16;
        int heightRatio = 3;
        int heightStack = 4;
        int searchInputHeight = 20;
        int totalWidth = scale * allColor;
        int totalHeight = scale * heightRatio * heightStack;
        int elementHeight = (this.font.lineHeight + 1) * 3;

        FilterPredicate<SoundPackGUI> soundPackFilter = (soundPackGUI, filterText) -> {
            ConfigHolder config = soundPackGUI.getConfigHolder();
            String combinedText = (config.getPackName() + " " + config.getParentName() + " " + config.getFileName()).toLowerCase();
            return combinedText.contains(filterText.toLowerCase());
        };

        this.soundPackListGUI = FilterableListGUI.<SoundPackGUI>builder()
                .position((width - totalWidth) / 2, (height - totalHeight) / 2)
                .size(totalWidth, totalHeight)
                .elementSize(totalWidth, elementHeight)
                .items(LMConfigManager.INSTANCE.getAllConfig().stream()
                        .map(c -> new SoundPackGUI(totalWidth, elementHeight, this.font, c))
                        .collect(Collectors.toList()))
                .filterBy(soundPackFilter)
                .withScrollBar()
                .searchInputHeight(searchInputHeight)
                .withPlaceholder("Search sound packs...")
                .build();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        assert this.minecraft != null;
        int relX = (this.width - GUI_WIDTH) / 2;
        int relY = (this.height - GUI_HEIGHT) / 2;
        context.blit(MODEL_SELECT_GUI_TEXTURE, relX, relY, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        super.render(context, mouseX, mouseY, delta);
        this.soundPackListGUI.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.soundPackListGUI.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.soundPackListGUI.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.soundPackListGUI.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.soundPackListGUI.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) return true;
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        return this.soundPackListGUI.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.soundPackListGUI.charTyped(chr, modifiers)) return true;
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        soundPackListGUI.getSelectedItem()
                .ifPresent(gui -> NetworkHandler.sendSyncSoundPackC2S(this.entity, gui.getConfigHolder()));
    }

    public static class SoundPackGUI extends GUIElement implements ListGUIElement {
        private final Font font;
        private final ConfigHolder configHolder;
        private boolean selected;

        protected SoundPackGUI(int width, int height, Font font, ConfigHolder configHolder) {
            super(width, height);
            this.font = font;
            this.configHolder = configHolder;
        }

        @Override
        public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
            context.drawString(font, configHolder.getPackName(),
                    this.x, this.y + 1, 0xffffff, false);
            context.drawString(font, configHolder.getParentName(),
                    this.x, this.y + 1 + (font.lineHeight + 1), 0xffffff, false);
            context.drawString(font, configHolder.getFileName(),
                    this.x, this.y + 1 + (font.lineHeight + 1) * 2, 0xffffff, false);
            context.fill(this.x, this.y + this.height - 1,
                    this.x + this.width, this.y + this.height, 0xffffffff);
            if (this.selected) {
                context.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x80ffffff);
            }
        }

        @Override
        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public boolean isSelected() {
            return this.selected;
        }

        public ConfigHolder getConfigHolder() {
            return configHolder;
        }
    }
}
