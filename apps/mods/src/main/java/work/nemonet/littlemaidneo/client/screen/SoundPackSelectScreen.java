package work.nemonet.littlemaidneo.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
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

public class SoundPackSelectScreen<T extends Entity & SoundPlayable> extends AbstractFilterableListScreen<SoundPackSelectScreen.SoundPackGUI> {
    public static final Identifier MODEL_SELECT_GUI_TEXTURE =
            Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "textures/gui/model_select.png");
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 196;
    // model_select.png のキャンバスサイズ（blit の UV 正規化に必要）
    private static final int TEXTURE_SIZE = 256;
    private final T entity;

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
            String combinedText = (config.packName() + " " + config.parentName() + " " + config.fileName()).toLowerCase();
            return combinedText.contains(filterText.toLowerCase());
        };

        this.listGUI = FilterableListGUI.<SoundPackGUI>builder()
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        assert this.minecraft != null;
        int relX = (this.width - GUI_WIDTH) / 2;
        int relY = (this.height - GUI_HEIGHT) / 2;
        context.blit(RenderPipelines.GUI_TEXTURED, MODEL_SELECT_GUI_TEXTURE, relX, relY, 0.0f, 0.0f, GUI_WIDTH, GUI_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        super.extractRenderState(context, mouseX, mouseY, delta);
        if (this.listGUI != null) {
            this.listGUI.extractRenderState(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (listGUI != null) {
            listGUI.getSelectedItem()
                    .ifPresent(gui -> NetworkHandler.sendSyncSoundPackC2S(this.entity, gui.getConfigHolder()));
        }
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
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            context.text(font, configHolder.packName(),
                    this.x, this.y + 1, 0xffffff, false);
            context.text(font, configHolder.parentName(),
                    this.x, this.y + 1 + (font.lineHeight + 1), 0xffffff, false);
            context.text(font, configHolder.fileName(),
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
