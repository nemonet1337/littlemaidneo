package work.nemonet.littlemaidneo.client.screen.component;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import work.nemonet.littlemaidneo.client.screen.ModelSelectScreen;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.multimodel.IMultiModel;
import work.nemonet.littlemaidneo.resource.holder.TextureHolder;
import work.nemonet.littlemaidneo.resource.manager.LMModelManager;
import work.nemonet.littlemaidneo.resource.util.ArmorPart;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;
import work.nemonet.littlemaidneo.entity.DummyModelEntity;

public class ArmorModelGUI extends GUIElement implements ListGUIElement {
    private static final ArmorSets<ItemStack> ARMOR_ICONS = new ArmorSets<>();
    private final MarginedClickable selectBox = new MarginedClickable(4);
    private final int scale;
    private final DummyModelEntity dummy;
    private final TextureHolder texture;
    private final ImmutableList<String> armorNames;
    private final ArmorSets<ArmorModelGUI> armors;
    private boolean selected;

    static {
        ARMOR_ICONS.setArmor(Items.DIAMOND_HELMET.getDefaultInstance(), IHasMultiModel.Part.HEAD);
        ARMOR_ICONS.setArmor(Items.DIAMOND_CHESTPLATE.getDefaultInstance(), IHasMultiModel.Part.BODY);
        ARMOR_ICONS.setArmor(Items.DIAMOND_LEGGINGS.getDefaultInstance(), IHasMultiModel.Part.LEGS);
        ARMOR_ICONS.setArmor(Items.DIAMOND_BOOTS.getDefaultInstance(), IHasMultiModel.Part.FEET);
    }

    public ArmorModelGUI(TextureHolder texture, int scale, DummyModelEntity dummy,
                         ArmorSets<ArmorModelGUI> armors) {
        super(scale * 16, scale * 3);
        this.scale = scale;
        this.dummy = dummy;
        this.texture = texture;
        this.armorNames = ImmutableList.copyOf(texture.getArmorNames());
        this.armors = armors;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        MultiModelGUIUtil.getModel(LMModelManager.INSTANCE, texture).ifPresent(model ->
                renderAllArmorModel(context, scale, mouseX, mouseY, model, texture, dummy));
    }

    public void renderAllArmorModel(GuiGraphicsExtractor context, int scale, float mouseX, float mouseY,
                                    IMultiModel model, TextureHolder texture, DummyModelEntity dummy) {
        Font fontRenderer = Minecraft.getInstance().font;
        ModelSelectScreen.renderColor(context,
                this.x, this.y,
                this.x + this.width, this.y + fontRenderer.lineHeight,
                0xFF404040
        );

        int index = 0;
        LMModelManager modelManager = LMModelManager.INSTANCE;
        for (String armorName : armorNames) {
            index++;
            ArmorPart armorData = MultiModelGUIUtil.getArmorDate(modelManager, texture, armorName);
            MultiModelGUIUtil.renderArmor(context,
                    this.x + index * scale - scale / 2, this.y + height,
                    mouseX, mouseY, scale, model, armorData, dummy);
        }

        context.text(fontRenderer, texture.getTextureName(),
                this.x, this.y, 0xFFFFFFFF, false);

        ARMOR_ICONS.foreach((part, stack) ->
                context.item(stack,
                        this.x + this.width - 16 * (part.getIndex() + 1),
                        this.y + fontRenderer.lineHeight));
        armors.foreach((p, g) -> {
            if (g == this) {
                ModelSelectScreen.renderColor(context,
                        this.x + this.width - 16 * (p.getIndex() + 1),
                        this.y + fontRenderer.lineHeight,
                        this.x + this.width - 16 * (p.getIndex() + 1) + 16,
                        this.y + fontRenderer.lineHeight + 16,
                        0x80FFFFFF
                );
            }
        });
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
                Font fontRenderer = Minecraft.getInstance().font;
                double relativeX = event.x() - this.x;
                double relativeY = event.y() - this.y;
                if (this.width - 16 * 4 <= relativeX && relativeX < this.width
                        && fontRenderer.lineHeight <= relativeY && relativeY < fontRenderer.lineHeight + 16) {
                    int idx = 3 - Mth.floor((relativeX - (this.width - 16 * 4)) / 16);
                    IHasMultiModel.Part part = IHasMultiModel.Part.getPart(idx);
                    if (armors.getArmor(part).filter(g -> g == this).isPresent()) {
                        armors.setArmor(null, part);
                    } else {
                        armors.setArmor(this, part);
                    }
                } else {
                    boolean selectAll = false;
                    for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
                        if (armors.getArmor(part).filter(g -> g == this).isEmpty()) {
                            selectAll = true;
                            break;
                        }
                    }
                    for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
                        armors.setArmor(selectAll ? this : null, part);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public TextureHolder getTexture() {
        return this.texture;
    }

    public void setArmorPart(IHasMultiModel.Part part, boolean selected) {
        if (selected) {
            armors.setArmor(this, part);
        } else {
            if (armors.getArmor(part).filter(g -> g == this).isPresent()) {
                armors.setArmor(null, part);
            }
        }
    }

    public void setAllArmorParts(boolean selected) {
        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            setArmorPart(part, selected);
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
}
