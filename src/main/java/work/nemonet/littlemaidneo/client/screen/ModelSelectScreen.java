package work.nemonet.littlemaidneo.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.client.screen.component.*;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.DummyModelEntity;
import work.nemonet.littlemaidneo.network.NetworkHandler;
import work.nemonet.littlemaidneo.resource.holder.TextureHolder;
import work.nemonet.littlemaidneo.resource.manager.LMModelManager;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;
import work.nemonet.littlemaidneo.resource.util.ArmorPart;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;
import work.nemonet.littlemaidneo.resource.util.TexturePair;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
public class ModelSelectScreen<T extends Entity & IHasMultiModel> extends Screen {
    public static final Identifier EMPTY_TEXTURE =
            Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "textures/empty.png");
    public static final TexturePair EMPTY_TEXTURE_PAIR = new TexturePair(EMPTY_TEXTURE, null);
    public static final ArmorPart EMPTY_ARMOR_DATA =
            new ArmorPart(null, null, null, null, null, null);
    public static final Identifier MODEL_SELECT_GUI_TEXTURE =
            Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "textures/gui/model_select.png");

    private static final ItemStack ARMOR = Items.DIAMOND_CHESTPLATE.getDefaultInstance();
    private static final ItemStack MODEL = Items.ARMOR_STAND.getDefaultInstance();
    private static final ItemStack WILD = Items.BONE.getDefaultInstance();
    private static final ItemStack CONTRACT = Items.CAKE.getDefaultInstance();
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 196;

    private final T entity;
    private final DummyModelEntity dummy;
    private final ArmorSets<ArmorModelGUI> armors = new ArmorSets<>();
    private final int scale = 15;
    private final int heightRatio = 3;
    private FilterableListGUI<MultiModelGUI> modelListGUI;
    private FilterableListGUI<ArmorModelGUI> armorListGUI;
    private boolean guiSwitch = true;
    private boolean isContract = true;

    public ModelSelectScreen(Component titleIn, Level world, T entity) {
        super(titleIn);
        this.entity = entity;
        this.dummy = new DummyModelEntity(world);
    }

    @Override
    protected void init() {
        assert this.minecraft != null;
        Collection<TextureHolder> textureHolders = LMTextureManager.INSTANCE.getAllTextures();
        Map<String, TextureHolder> map = new HashMap<>();
        textureHolders.forEach(th -> map.put(th.getTextureName().toLowerCase(), th));
        initModelGUI(textureHolders, map);
        initArmorGUI(textureHolders, map);
    }

    protected void initModelGUI(Collection<TextureHolder> textureHolders, Map<String, TextureHolder> textureHolderMap) {
        int allColor = 16;
        LMModelManager modelManager = LMModelManager.INSTANCE;
        int searchInputHeight = 20;
        int listWidth = scale * allColor;
        int listHeight = scale * heightRatio * 4;

        FilterPredicate<MultiModelGUI> multiModelFilter = (multiModelGUI, filterText) -> {
            String textureName = multiModelGUI.getTexture().getTextureName().toLowerCase();
            return textureName.contains(filterText.toLowerCase());
        };

        this.modelListGUI = FilterableListGUI.<MultiModelGUI>builder()
                .position((width - listWidth) / 2, (height - listHeight) / 2)
                .size(listWidth, listHeight)
                .elementSize(listWidth, scale * heightRatio)
                .items(textureHolders.stream()
                        .map(TextureHolder::getTextureName)
                        .map(String::toLowerCase)
                        .sorted(Comparator.naturalOrder())
                        .map(textureHolderMap::get)
                        .filter(th -> th.hasSkinTexture(this.isContract) &&
                                modelManager.getModel(th.getModelName(), IHasMultiModel.Layer.SKIN).isPresent())
                        .map(t -> new MultiModelGUI(t, this.isContract, scale, this.dummy))
                        .collect(Collectors.toList()))
                .filterBy(multiModelFilter)
                .withScrollBar()
                .searchInputHeight(searchInputHeight)
                .withPlaceholder("Search skin textures...")
                .build();

        restoreModelSelection();
    }

    protected void initArmorGUI(Collection<TextureHolder> textureHolders, Map<String, TextureHolder> map) {
        LMModelManager modelManager = LMModelManager.INSTANCE;
        int allColor = 16;
        int searchInputHeight = 20;
        int listWidth = scale * allColor;
        int listHeight = scale * heightRatio * 4;

        FilterPredicate<ArmorModelGUI> armorModelFilter = (armorModelGUI, filterText) -> {
            String textureName = armorModelGUI.getTexture().getTextureName().toLowerCase();
            return textureName.contains(filterText.toLowerCase());
        };

        this.armorListGUI = FilterableListGUI.<ArmorModelGUI>builder()
                .position((width - listWidth) / 2, (height - listHeight) / 2)
                .size(listWidth, listHeight)
                .elementSize(listWidth, scale * heightRatio)
                .items(textureHolders.stream()
                        .map(TextureHolder::getTextureName)
                        .map(String::toLowerCase)
                        .sorted(Comparator.naturalOrder())
                        .map(map::get)
                        .filter(th -> th.hasArmorTexture() &&
                                modelManager.getModel(th.getModelName(), IHasMultiModel.Layer.INNER).isPresent())
                        .map(t -> new ArmorModelGUI(t, scale, this.dummy, this.armors))
                        .collect(Collectors.toList()))
                .filterBy(armorModelFilter)
                .withScrollBar()
                .searchInputHeight(searchInputHeight)
                .withPlaceholder("Search armor textures...")
                .build();

        restoreArmorSelection();
    }

    public static void renderColor(GuiGraphicsExtractor context, int minX, int minY, int maxX, int maxY, int rgba) {
        context.fill(minX, minY, maxX, maxY, rgba);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
        assert this.minecraft != null;
        int relX = (this.width - GUI_WIDTH) / 2;
        int relY = (this.height - GUI_HEIGHT) / 2;
        context.blit(MODEL_SELECT_GUI_TEXTURE, relX, relY, GUI_WIDTH, GUI_HEIGHT, 0.0f, 0.0f, (float) GUI_WIDTH, (float) GUI_HEIGHT);

        context.item(guiSwitch ? ARMOR : MODEL, relX - 24, relY + GUI_HEIGHT - 16);
        context.item(isContract ? WILD : CONTRACT, relX - 24, relY + GUI_HEIGHT - 48);
        context.blit(MODEL_SELECT_GUI_TEXTURE, relX - 24, relY + GUI_HEIGHT - 16, 16, 16, 0.0f, 240.0f, 16.0f, 16.0f);
        context.blit(MODEL_SELECT_GUI_TEXTURE, relX - 24, relY + GUI_HEIGHT - 48, 16, 16, 0.0f, 240.0f, 16.0f, 16.0f);

        if (guiSwitch) {
            modelListGUI.extractRenderState(context, mouseX, mouseY, partialTicks);
            modelListGUI.getSelectedItem()
                    .filter(MultiModelGUI::isSelected)
                    .ifPresent(g -> g.getSelectColor().ifPresent(color -> {
                        TextureHolder texture = g.getTexture();
                        MultiModelGUIUtil.getModel(LMModelManager.INSTANCE, texture).ifPresent(model -> {
                            int sc = 15 * 3;
                            MultiModelGUIUtil.getTexturePair(texture, color, true).ifPresent(texturePair ->
                                    MultiModelGUIUtil.renderModel(context,
                                            (width + 15 * 16 + sc * 2) / 2,
                                            height - sc,
                                            mouseX, mouseY, sc,
                                            model, texturePair, this.dummy
                                    )
                            );
                        });
                    }));
        } else {
            armorListGUI.extractRenderState(context, mouseX, mouseY, partialTicks);
            this.armors.foreach((p, g) -> {
                TextureHolder texture = g.getTexture();
                MultiModelGUIUtil.getModel(LMModelManager.INSTANCE, texture).ifPresent(model -> {
                    int sc = 15 * 3;
                    ArmorPart armorData = MultiModelGUIUtil.getArmorDate(LMModelManager.INSTANCE, texture, "default");
                    MultiModelGUIUtil.renderArmorPart(context,
                            (width + 15 * 16 + sc * 2) / 2, height - sc,
                            mouseX, mouseY, sc, model, armorData, p, this.dummy);
                });
            });
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        double x = event.x(); double y = event.y();
        int minX = (this.width - GUI_WIDTH) / 2 - 24;
        int minY = (this.height - GUI_HEIGHT) / 2 + GUI_HEIGHT - 16;
        if (minX <= x && x < minX + 16 && minY <= y && y < minY + 16) {
            guiSwitch = !guiSwitch;
            playDownSound();
            return true;
        } else if (minX <= x && x < minX + 16 && minY - 32 <= y && y < minY - 16) {
            isContract = !isContract;
            Collection<TextureHolder> textureHolders = LMTextureManager.INSTANCE.getAllTextures();
            Map<String, TextureHolder> map = new HashMap<>();
            textureHolders.forEach(th -> map.put(th.getTextureName().toLowerCase(), th));
            initModelGUI(textureHolders, map);
            playDownSound();
            return true;
        }
        if (guiSwitch) return modelListGUI.mouseClicked(event, handled);
        else return armorListGUI.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (guiSwitch) return modelListGUI.mouseDragged(event, deltaX, deltaY);
        else return armorListGUI.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (guiSwitch) return modelListGUI.mouseReleased(event);
        else return armorListGUI.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double deltaX, double scrollAmount) {
        if (guiSwitch) return modelListGUI.mouseScrolled(x, y, deltaX, scrollAmount);
        else return armorListGUI.mouseScrolled(x, y, deltaX, scrollAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) return true;
        if (guiSwitch) return modelListGUI.keyPressed(event);
        else return armorListGUI.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (guiSwitch) {
            if (modelListGUI.charTyped(event)) return true;
        } else {
            if (armorListGUI.charTyped(event)) return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void removed() {
        super.removed();
        modelListGUI.getSelectedItem().ifPresent(g ->
                g.getSelectColor().ifPresent(color -> {
                    TextureHolder texture = g.getTexture();
                    entity.setColorMM(color);
                    entity.setContractMM(this.isContract);
                    entity.setTextureHolder(texture, IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD);
                    for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
                        entity.setTextureHolder(texture, IHasMultiModel.Layer.INNER, part);
                    }
                })
        );

        this.armors.foreach((p, g) ->
                entity.setTextureHolder(g.getTexture(), IHasMultiModel.Layer.INNER, p));

        NetworkHandler.sendSyncMultiModelC2S(entity, entity);
    }

    private void restoreModelSelection() {
        TextureHolder ownerSkinTex = entity.getTextureHolder(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD);
        var color = entity.getColorMM();
        if (ownerSkinTex != null) {
            modelListGUI.setSelectedItemBy(
                    multiModelGUI -> multiModelGUI.getTexture() == ownerSkinTex,
                    multiModelGUI -> multiModelGUI.setSelectColor(color)
            );
        }
    }

    private void restoreArmorSelection() {
        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            TextureHolder ownerArmorTex = entity.getTextureHolder(IHasMultiModel.Layer.INNER, part);
            if (ownerArmorTex != null) {
                armorListGUI.setSelectedItemBy(
                        armorModelGUI -> armorModelGUI.getTexture() == ownerArmorTex,
                        armorModelGUI -> armorModelGUI.setArmorPart(part, true)
                );
            }
        }
    }

    public static void playDownSound() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
