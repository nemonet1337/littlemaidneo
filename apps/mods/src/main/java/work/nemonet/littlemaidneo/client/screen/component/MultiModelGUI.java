package work.nemonet.littlemaidneo.client.screen.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import work.nemonet.littlemaidneo.client.screen.ModelSelectScreen;
import work.nemonet.littlemaidneo.resource.holder.TextureHolder;
import work.nemonet.littlemaidneo.resource.manager.LMModelManager;
import work.nemonet.littlemaidneo.resource.util.TextureColors;

import work.nemonet.littlemaidneo.entity.DummyModelEntity;

import java.util.Optional;

public class MultiModelGUI extends GUIElement implements ListGUIElement {
    private final MarginedClickable selectBox = new MarginedClickable(4);
    private final int scale;
    private final DummyModelEntity dummy;
    private final TextureHolder texture;
    private final boolean isContract;
    private TextureColors selectColor = null;
    private boolean selected;

    public MultiModelGUI(TextureHolder texture, boolean isContract, int scale, DummyModelEntity dummy) {
        super(scale * 16, scale * 3);
        this.isContract = isContract;
        this.scale = scale;
        this.dummy = dummy;
        this.texture = texture;
    }

    public TextureHolder getTexture() {
        return this.texture;
    }

    public Optional<TextureColors> getSelectColor() {
        return Optional.ofNullable(this.selectColor);
    }

    public void setSelectColor(TextureColors selectColor) {
        this.selectColor = selectColor;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        var fontRenderer = Minecraft.getInstance().font;
        ModelSelectScreen.renderColor(context,
                this.x, this.y,
                this.x + this.width, this.y + fontRenderer.lineHeight,
                0xFF404040
        );
        if (selected && selectColor != null) {
            context.fill(this.x + selectColor.getIndex() * scale, this.y,
                    this.x + selectColor.getIndex() * scale + scale, this.y + height,
                    (0x80 << 24) | selectColor.getColorCode());
        }

        MultiModelGUIUtil.getModel(LMModelManager.INSTANCE, texture).ifPresent(model ->
                renderAllColorModel(context, scale, mouseX, mouseY, model, texture, isContract));

        context.text(fontRenderer, texture.getTextureName(),
                this.x, this.y, 0xFFFFFFFF, false);
    }

    private void renderAllColorModel(GuiGraphicsExtractor context, int scale, float mouseX, float mouseY,
                                     work.nemonet.littlemaidneo.multimodel.IMultiModel model,
                                     TextureHolder holder, boolean isContract) {
        for (TextureColors color : TextureColors.values()) {
            MultiModelGUIUtil.getTexturePair(holder, color, isContract).ifPresent(texturePair ->
                    MultiModelGUIUtil.renderModel(context,
                            this.x + (color.getIndex() + 1) * scale - scale / 2,
                            this.y + height,
                            mouseX, mouseY, scale,
                            model, texturePair, dummy
                    )
            );
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            selectBox.click(event.x(), event.y());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (selectBox.release(event.x(), event.y())) {
                this.selectColor = TextureColors.getColor(Mth.floor((event.x() - this.x) / scale));
                return true;
            }
        }
        return false;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean isSelected() {
        return this.selected;
    }
}
