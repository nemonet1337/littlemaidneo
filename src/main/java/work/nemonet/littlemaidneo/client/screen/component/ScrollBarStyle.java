package work.nemonet.littlemaidneo.client.screen.component;

import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.client.screen.ModelSelectScreen;

import java.util.function.Function;

public enum ScrollBarStyle {
    DEFAULT(
            (height) -> new ScrollableListGUI.ScrollBarConfig(
                    10, 0, 8, height,
                    new TextureAddress(0, 200, 8, 8, 256, 256),
                    new TextureAddress(0, 208, 8, 8, 256, 256),
                    new TextureAddress(0, 216, 8, 8, 256, 256),
                    new TextureAddress(0, 224, 10, 6, 256, 256),
                    ModelSelectScreen.MODEL_SELECT_GUI_TEXTURE
            )
    );

    private final Function<Integer, ScrollableListGUI.ScrollBarConfig> config;

    ScrollBarStyle(Function<Integer, ScrollableListGUI.ScrollBarConfig> config) {
        this.config = config;
    }

    public ScrollableListGUI.ScrollBarConfig getConfig(int height) {
        return config.apply(height);
    }

    public static ScrollableListGUI.ScrollBarConfig custom(
            int offsetX, int offsetY, int width, int height,
            TextureAddress sliderT, TextureAddress sliderM,
            TextureAddress sliderB, TextureAddress pointer,
            Identifier texture) {
        return new ScrollableListGUI.ScrollBarConfig(
                offsetX, offsetY, width, height,
                sliderT, sliderM, sliderB, pointer, texture
        );
    }
}
