package work.nemonet.littlemaidneo.client.screen.component;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.client.screen.ModelSelectScreen;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.multimodel.IMultiModel;
import work.nemonet.littlemaidneo.multimodel.layer.MMPose;
import work.nemonet.littlemaidneo.resource.holder.TextureHolder;
import work.nemonet.littlemaidneo.resource.manager.LMModelManager;
import work.nemonet.littlemaidneo.resource.util.ArmorPart;
import work.nemonet.littlemaidneo.resource.util.TextureColors;
import work.nemonet.littlemaidneo.resource.util.TexturePair;
import work.nemonet.littlemaidneo.entity.DummyModelEntity;

import java.util.Optional;

public class MultiModelGUIUtil {

    public static Optional<IMultiModel> getModel(LMModelManager modelManager, TextureHolder texture) {
        return modelManager.getModel(texture.getModelName(), IHasMultiModel.Layer.SKIN);
    }

    public static Optional<TexturePair> getTexturePair(TextureHolder holder, TextureColors color, boolean isContract) {
        Optional<Identifier> optional = holder.getTexture(color, isContract, false);
        return optional.map(Identifier ->
                new TexturePair(Identifier,
                        holder.getTexture(color, isContract, true).orElse(null)));
    }

    public static void renderModel(GuiGraphicsExtractor context, int posX, int posY, float mouseX, float mouseY, int scale,
                                   IMultiModel model, TexturePair texturePair, DummyModelEntity dummy) {
        dummy.setSkinModel(model);
        dummy.setSkinTexture(texturePair);
        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            dummy.setArmorVisible(false, part);
            dummy.setArmorData(ModelSelectScreen.EMPTY_ARMOR_DATA, part);
        }
        dummy.setAllArmorVisible(false);
        renderEntity(context, posX, posY, mouseX, mouseY, scale, model, dummy);
    }

    public static ArmorPart getArmorDate(LMModelManager modelManager, TextureHolder texture, String armorName) {
        IMultiModel innerModel = modelManager.getModel(texture.getModelName(), IHasMultiModel.Layer.INNER)
                .orElseThrow(() -> new IllegalStateException("モデルが存在しません"));
        IMultiModel outerModel = modelManager.getModel(texture.getModelName(), IHasMultiModel.Layer.OUTER)
                .orElseThrow(() -> new IllegalStateException("モデルが存在しません"));
        Identifier innerTex = texture.getArmorTexture(IHasMultiModel.Layer.INNER, armorName, 0, false).orElse(null);
        Identifier innerLightTex = texture.getArmorTexture(IHasMultiModel.Layer.INNER, armorName, 0, true).orElse(null);
        Identifier outerTex = texture.getArmorTexture(IHasMultiModel.Layer.OUTER, armorName, 0, false).orElse(null);
        Identifier outerLightTex = texture.getArmorTexture(IHasMultiModel.Layer.OUTER, armorName, 0, true).orElse(null);
        return new ArmorPart(innerTex, innerLightTex, outerTex, outerLightTex, innerModel, outerModel);
    }

    public static void renderArmor(GuiGraphicsExtractor context, int posX, int posY, float mouseX, float mouseY, int scale,
                                   IMultiModel model, ArmorPart data, DummyModelEntity dummy) {
        dummy.setSkinModel(model);
        dummy.setSkinTexture(ModelSelectScreen.EMPTY_TEXTURE_PAIR);
        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            dummy.setArmorVisible(true, part);
            dummy.setArmorData(data, part);
        }
        renderEntity(context, posX, posY, mouseX, mouseY, scale, model, dummy);
    }

    public static void renderArmorPart(GuiGraphicsExtractor context, int posX, int posY, float mouseX, float mouseY, int scale,
                                       IMultiModel model, ArmorPart data, IHasMultiModel.Part armorPart, DummyModelEntity dummy) {
        dummy.setSkinModel(model);
        dummy.setSkinTexture(ModelSelectScreen.EMPTY_TEXTURE_PAIR);
        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            dummy.setArmorVisible(false, part);
            dummy.setArmorData(ModelSelectScreen.EMPTY_ARMOR_DATA, part);
        }
        dummy.setArmorVisible(true, armorPart);
        dummy.setArmorData(data, armorPart);
        renderEntity(context, posX, posY, mouseX, mouseY, scale, model, dummy);
    }

    public static void renderEntity(GuiGraphicsExtractor context, int posX, int posY, float mouseX, float mouseY, int scale,
                                    IMultiModel model, DummyModelEntity dummy) {
        float eyeHeight = model.getEyeHeight(dummy.getCaps(), MMPose.STANDING);
        InventoryScreen.extractEntityInInventoryFollowsMouse(context,
                posX - scale, posY - (int) (eyeHeight * scale * 2),
                posX + scale, posY,
                scale, 0f,
                mouseX, mouseY,
                dummy
        );
    }
}
