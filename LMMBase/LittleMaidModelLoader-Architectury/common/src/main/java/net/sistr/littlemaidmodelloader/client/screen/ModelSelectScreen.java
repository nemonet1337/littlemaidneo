package net.sistr.littlemaidmodelloader.client.screen;

// Changed: Yarn mappings to Mojang mappings
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.sistr.littlemaidmodelloader.LMMLMod;
import net.sistr.littlemaidmodelloader.client.screen.component.*;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.network.SyncMultiModelPacket;
import net.sistr.littlemaidmodelloader.resource.holder.TextureHolder;
import net.sistr.littlemaidmodelloader.resource.manager.LMModelManager;
import net.sistr.littlemaidmodelloader.resource.manager.LMTextureManager;
import net.sistr.littlemaidmodelloader.resource.util.ArmorPart;
import net.sistr.littlemaidmodelloader.resource.util.ArmorSets;
import net.sistr.littlemaidmodelloader.resource.util.TexturePair;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

//GUIが小さくならない画面の最低サイズ640x480を想定して組む
//ただし、描画は320x240でやって倍にされるっぽい
@Environment(EnvType.CLIENT)
public class ModelSelectScreen<T extends Entity & IHasMultiModel> extends Screen {
    // Changed: ResourceLocation to ResourceLocation, new constructor method (Mojang mapping)
    public static final ResourceLocation EMPTY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(LMMLMod.MODID, "textures/empty.png");
    public static final TexturePair EMPTY_TEXTURE_PAIR = new TexturePair(EMPTY_TEXTURE, null);
    public static final ArmorPart EMPTY_ARMOR_DATA =
            new ArmorPart(null, null, null, null,
                    null, null);
    public static final ResourceLocation MODEL_SELECT_GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(LMMLMod.MODID, "textures/gui/model_select.png");
    // Changed: getDefaultStack() to getDefaultInstance() (Mojang mapping)
    private static final ItemStack ARMOR = Items.DIAMOND_CHESTPLATE.getDefaultInstance();
    private static final ItemStack MODEL = Items.ARMOR_STAND.getDefaultInstance();
    private static final ItemStack WILD = Items.BONE.getDefaultInstance();
    private static final ItemStack CONTRACT = Items.CAKE.getDefaultInstance();
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 196;
    private final T entity;
    private final MultiModelGUIUtil.DummyModelEntity dummy;
    private final ArmorSets<ArmorModelGUI> armors = new ArmorSets<>();
    private final int scale = 15;
    private final int heightRatio = 3;
    private FilterableListGUI<MultiModelGUI> modelListGUI;
    private FilterableListGUI<ArmorModelGUI> armorListGUI;
    private boolean guiSwitch = true;
    private boolean isContract = true;

    // Changed: Text to Component, World to Level (Mojang mapping)
    public ModelSelectScreen(Component titleIn, Level world, T entity) {
        super(titleIn);
        this.entity = entity;
        this.dummy = new MultiModelGUIUtil.DummyModelEntity(world);
    }

    @Override
    protected void init() {
        // Changed: this.minecraft to this.minecraft (Mojang mapping)
        assert this.minecraft != null;
        Collection<TextureHolder> textureHolders =
                LMTextureManager.INSTANCE.getAllTextures();
        Map<String, TextureHolder> map = new HashMap<>();
        textureHolders.forEach(textureHolder -> map.put(textureHolder.getTextureName().toLowerCase(), textureHolder));
        initModelGUI(textureHolders, map);
        initArmorGUI(textureHolders, map);
    }

    protected void initModelGUI(Collection<TextureHolder> textureHolders, Map<String, TextureHolder> textureHolderMap) {
        int allColor = 16;
        LMModelManager modelManager = LMModelManager.INSTANCE;

        // レイアウト計算（4列時と同じ位置・サイズを維持）
        int searchInputHeight = 20;
        int listWidth = scale * allColor;
        int listHeight = scale * heightRatio * 4; // 4列時のサイズを維持

        // MultiModelGUI用のFilterPredicate（テクスチャ名で検索）
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
                        .filter(textureHolder ->
                                textureHolder.hasSkinTexture(this.isContract) &&
                                        modelManager.getModel(textureHolder.getModelName(), IHasMultiModel.Layer.SKIN)
                                                .isPresent())
                        .map(t -> new MultiModelGUI(t, this.isContract, scale, this.dummy))
                        .collect(Collectors.toList()))
                .filterBy(multiModelFilter)
                .withScrollBar()
                .searchInputHeight(searchInputHeight)
                .withPlaceholder("Search skin textures...")
                .build();

        // 初期選択状態の復元
        restoreModelSelection();
    }

    protected void initArmorGUI(Collection<TextureHolder> textureHolders, Map<String, TextureHolder> map) {
        LMModelManager modelManager = LMModelManager.INSTANCE;
        int allColor = 16;

        // レイアウト計算（4列時と同じ位置・サイズを維持）
        int searchInputHeight = 20;
        int listWidth = scale * allColor;
        int listHeight = scale * heightRatio * 4; // 4列時のサイズを維持

        // ArmorModelGUI用のFilterPredicate（テクスチャ名で検索）
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
                        .filter(textureHolder ->
                                textureHolder.hasArmorTexture() &&
                                        modelManager.getModel(textureHolder.getModelName(), IHasMultiModel.Layer.INNER)
                                                .isPresent())
                        .map(t -> new ArmorModelGUI(t, scale, this.dummy, this.armors))
                        .collect(Collectors.toList()))
                .filterBy(armorModelFilter)
                .withScrollBar()
                .searchInputHeight(searchInputHeight)
                .withPlaceholder("Search armor textures...")
                .build();

        // 初期選択状態の復元
        restoreArmorSelection();
    }

    // Changed: GuiGraphics to GuiGraphics (Mojang mapping)
    public static void renderColor(GuiGraphics context, int minX, int minY, int maxX, int maxY, int rgba) {
        context.fill(minX, minY, maxX, maxY, rgba);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        assert this.minecraft != null;
        int relX = (this.width - GUI_WIDTH) / 2;
        int relY = (this.height - GUI_HEIGHT) / 2;
        context.blit(MODEL_SELECT_GUI_TEXTURE, relX, relY, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        context.renderItem(guiSwitch ? ARMOR : MODEL, relX - 24, relY + GUI_HEIGHT - 16);
        context.renderItem(isContract ? WILD : CONTRACT, relX - 24, relY + GUI_HEIGHT - 48);
        context.blit(MODEL_SELECT_GUI_TEXTURE, relX - 24, relY + GUI_HEIGHT - 16, 0, 240, 16, 16);
        context.blit(MODEL_SELECT_GUI_TEXTURE, relX - 24, relY + GUI_HEIGHT - 48, 0, 240, 16, 16);

        if (guiSwitch) {
            modelListGUI.render(context, mouseX, mouseY, partialTicks);
            modelListGUI.getSelectedItem()
                    .filter(MultiModelGUI::isSelected)
                    .ifPresent(g -> g.getSelectColor().ifPresent(color -> {
                        TextureHolder texture = g.getTexture();
                        MultiModelGUIUtil.getModel(LMModelManager.INSTANCE, texture).ifPresent(model -> {
                            int scale = 15 * 3;
                            MultiModelGUIUtil.getTexturePair(texture, color, true).ifPresent(texturePair ->
                                    MultiModelGUIUtil.renderModel(context,
                                            (width + 15 * 16 + scale * 2) / 2,
                                            height - scale,
                                            mouseX, mouseY, scale,
                                            model, texturePair, this.dummy
                                    )
                            );
                        });
                    }));
        } else {
            armorListGUI.render(context, mouseX, mouseY, partialTicks);
            this.armors.foreach((p, g) -> {
                TextureHolder texture = g.getTexture();
                MultiModelGUIUtil.getModel(LMModelManager.INSTANCE, texture).ifPresent(model -> {
                    int scale = 15 * 3;
                    LMModelManager modelManager = LMModelManager.INSTANCE;
                    ArmorPart armorData = MultiModelGUIUtil.getArmorDate(modelManager, texture, "default");
                    MultiModelGUIUtil.renderArmorPart(context, (width + 15 * 16 + scale * 2) / 2, height - scale,
                            mouseX, mouseY, scale, model, armorData, p, this.dummy);
                });
            });
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        int minX = (this.width - GUI_WIDTH) / 2 - 24;
        int minY = (this.height - GUI_HEIGHT) / 2 + GUI_HEIGHT - 16;
        if (minX <= x && x < minX + 16 && minY <= y && y < minY + 16) {
            guiSwitch = !guiSwitch;
            playDownSound();
            return true;
        } else if (minX <= x && x < minX + 16 && minY - 32 <= y && y < minY - 16) {
            isContract = !isContract;
            Collection<TextureHolder> textureHolders =
                    LMTextureManager.INSTANCE.getAllTextures();
            Map<String, TextureHolder> map = new HashMap<>();
            textureHolders.forEach(textureHolder -> map.put(textureHolder.getTextureName().toLowerCase(), textureHolder));
            initModelGUI(textureHolders, map);
            playDownSound();
            return true;
        }
        if (guiSwitch) {
            return modelListGUI.mouseClicked(x, y, button);
        } else {
            return armorListGUI.mouseClicked(x, y, button);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (guiSwitch) {
            return modelListGUI.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        } else {
            return armorListGUI.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (guiSwitch) {
            return modelListGUI.mouseReleased(mouseX, mouseY, button);
        } else {
            return armorListGUI.mouseReleased(mouseX, mouseY, button);
        }
    }

    // Changed: mouseScrolled signature in 1.21.1 (added double deltaX)
    @Override
    public boolean mouseScrolled(double x, double y, double deltaX, double scrollAmount) {
        if (guiSwitch) {
            return modelListGUI.mouseScrolled(x, y, deltaX, scrollAmount);
        } else {
            return armorListGUI.mouseScrolled(x, y, deltaX, scrollAmount);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // こちらが先でないとESCで画面を閉じれない
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (guiSwitch) {
            return modelListGUI.keyPressed(keyCode, scanCode, modifiers);
        } else {
            return armorListGUI.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (guiSwitch) {
            if (modelListGUI.charTyped(chr, modifiers)) {
                return true;
            }
        } else {
            if (armorListGUI.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        modelListGUI.getSelectedItem().ifPresent(g ->
                g.getSelectColor().ifPresent(color -> {
                    TextureHolder texture = g.getTexture();
                    //カラーと契約を更新
                    entity.setColorMM(color);
                    entity.setContractMM(this.isContract);
                    //スキンを更新
                    entity.setTextureHolder(texture, IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD);
                    //防具をスキンと同様に更新
                    for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
                        entity.setTextureHolder(texture, IHasMultiModel.Layer.INNER, part);
                    }
                })
        );

        this.armors.foreach((p, g) ->
                entity.setTextureHolder(g.getTexture(), IHasMultiModel.Layer.INNER, p));

        ArmorSets<String> armorNames = new ArmorSets<>();
        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            armorNames.setArmor(entity.getTextureHolder(IHasMultiModel.Layer.INNER, part).getTextureName(), part);
        }
        SyncMultiModelPacket.sendC2SPacket(entity, entity);
    }

    /**
     * モデルリストの初期選択状態を復元
     */
    private void restoreModelSelection() {
        TextureHolder ownerSkinTex = entity.getTextureHolder(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD);
        var color = entity.getColorMM();
        if (ownerSkinTex != null) {
            modelListGUI.setSelectedItemBy(multiModelGUI ->
                            multiModelGUI.getTexture() == ownerSkinTex,
                    multiModelGUI -> multiModelGUI.setSelectColor(color)
            );
        }
    }

    /**
     * アーマーリストの初期選択状態を復元
     */
    private void restoreArmorSelection() {
        // 各部位のアーマーテクスチャを取得して復元
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

    // Changed: Minecraft to Minecraft, PositionedSoundInstance to SimpleSoundInstance (Mojang mapping)
    public static void playDownSound() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }


}
