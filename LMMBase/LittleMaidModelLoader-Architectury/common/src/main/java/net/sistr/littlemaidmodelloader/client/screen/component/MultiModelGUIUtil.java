package net.sistr.littlemaidmodelloader.client.screen.component;

import com.google.common.collect.Lists;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.sistr.littlemaidmodelloader.client.screen.ModelSelectScreen;
import org.joml.Vector3f;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.maidmodel.EntityCaps;
import net.sistr.littlemaidmodelloader.maidmodel.IModelCaps;
import net.sistr.littlemaidmodelloader.multimodel.IMultiModel;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMPose;
import net.sistr.littlemaidmodelloader.resource.holder.TextureHolder;
import net.sistr.littlemaidmodelloader.resource.manager.LMModelManager;
import net.sistr.littlemaidmodelloader.resource.util.ArmorPart;
import net.sistr.littlemaidmodelloader.resource.util.ArmorSets;
import net.sistr.littlemaidmodelloader.resource.util.TextureColors;
import net.sistr.littlemaidmodelloader.resource.util.TexturePair;
import net.sistr.littlemaidmodelloader.setup.Registration;

import java.util.Optional;

public class MultiModelGUIUtil {

    public static Optional<IMultiModel> getModel(LMModelManager modelManager, TextureHolder texture) {
        if (modelManager.getModel(texture.getModelName(), IHasMultiModel.Layer.SKIN).isEmpty()) {
            return Optional.empty();
        }
        return modelManager.getModel(texture.getModelName(), IHasMultiModel.Layer.SKIN);
    }

    // Changed: ResourceLocation to ResourceLocation (Mojang mapping)
    public static Optional<TexturePair> getTexturePair(TextureHolder holder, TextureColors color, boolean isContract) {
        Optional<ResourceLocation> optional = holder.getTexture(color, isContract, false);
        return optional.map(resourceLocation ->
                new TexturePair(resourceLocation,
                        holder.getTexture(color, isContract, true).orElse(null)));
    }

    // Changed: GuiGraphics to GuiGraphics (Mojang mapping)
    public static void renderModel(GuiGraphics context, int posX, int posY, float mouseX, float mouseY, int scale,
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

    // Changed: ResourceLocation to ResourceLocation (Mojang mapping)
    public static ArmorPart getArmorDate(LMModelManager modelManager, TextureHolder texture, String armorName) {
        IMultiModel innerModel = modelManager.getModel(texture.getModelName(), IHasMultiModel.Layer.INNER)
                .orElseThrow(() -> new IllegalStateException("モデルが存在しません"));
        IMultiModel outerModel = modelManager.getModel(texture.getModelName(), IHasMultiModel.Layer.OUTER)
                .orElseThrow(() -> new IllegalStateException("モデルが存在しません"));
        ResourceLocation innerTex = texture.getArmorTexture(IHasMultiModel.Layer.INNER, armorName,
                0, false).orElse(null);
        ResourceLocation innerLightTex = texture.getArmorTexture(IHasMultiModel.Layer.INNER, armorName,
                0, true).orElse(null);
        ResourceLocation outerTex = texture.getArmorTexture(IHasMultiModel.Layer.OUTER, armorName,
                0, false).orElse(null);
        ResourceLocation outerLightTex = texture.getArmorTexture(IHasMultiModel.Layer.OUTER, armorName,
                0, true).orElse(null);
        return new ArmorPart(
                innerTex, innerLightTex,
                outerTex, outerLightTex,
                innerModel, outerModel
        );
    }

    // Changed: GuiGraphics to GuiGraphics (Mojang mapping)
    public static void renderArmor(GuiGraphics context, int posX, int posY, float mouseX, float mouseY, int scale,
                                   IMultiModel model, ArmorPart data, DummyModelEntity dummy) {
        dummy.setSkinModel(model);
        dummy.setSkinTexture(ModelSelectScreen.EMPTY_TEXTURE_PAIR);
        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            dummy.setArmorVisible(true, part);
            dummy.setArmorData(data, part);
        }
        renderEntity(context, posX, posY, mouseX, mouseY, scale, model, dummy);
    }

    // Changed: GuiGraphics to GuiGraphics (Mojang mapping)
    public static void renderArmorPart(GuiGraphics context, int posX, int posY, float mouseX, float mouseY, int scale,
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

    // Changed: GuiGraphics to GuiGraphics (Mojang mapping)
    public static void renderEntity(GuiGraphics context, int posX, int posY, float mouseX, float mouseY, int scale,
                                    IMultiModel model, DummyModelEntity dummy) {
        // 1.21.1: renderEntityInInventory signature changed
        float eyeHeight = model.getEyeHeight(dummy.getCaps(), MMPose.STANDING);
        InventoryScreen.renderEntityInInventory(context,
                posX, posY, scale,
                new Vector3f(posX - mouseX, posY - mouseY - eyeHeight * scale, 0),
                Axis.XP.rotationDegrees(0),
                Axis.YP.rotationDegrees(0),
                dummy
        );
    }

    // Changed: World to Level (Mojang mapping)
    public static class DummyModelEntity extends LivingEntity implements IHasMultiModel {
        private final EntityCaps caps = new EntityCaps(this);
        private IMultiModel skinModel;
        private TexturePair skinTexture;
        private final ArmorSets<ArmorPart> armorsData = new ArmorSets<>();
        private final ArmorSets<Boolean> armorsVisible = new ArmorSets<>();

        public DummyModelEntity(Level worldIn) {
            this(Registration.DUMMY_MODEL_ENTITY.get(), worldIn);
        }

        public DummyModelEntity(EntityType<DummyModelEntity> type, Level worldIn) {
            super(type, worldIn);
        }

        public void setSkinModel(IMultiModel model) {
            skinModel = model;
        }

        public void setSkinTexture(TexturePair skinTexture) {
            this.skinTexture = skinTexture;
        }

        public void setArmorData(ArmorPart data, Part part) {
            armorsData.setArmor(data, part);
        }

        public void setArmorVisible(boolean visible, Part part) {
            this.armorsVisible.setArmor(visible, part);
        }

        public void setAllArmorVisible(boolean visible) {
            for (Part part : Part.values()) {
                this.armorsVisible.setArmor(visible, part);
            }
        }

        // Changed: getArmorItems() to getArmorSlots() (Mojang mapping, 1.21.1)
        @Override
        public Iterable<ItemStack> getArmorSlots() {
            return Lists.newArrayList(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        }

        // Changed: getEquippedStack to getItemBySlot (Mojang mapping)
        @Override
        public ItemStack getItemBySlot(EquipmentSlot slot) {
            return ItemStack.EMPTY;
        }

        // Changed: equipStack to setItemSlot (Mojang mapping)
        @Override
        public void setItemSlot(EquipmentSlot slot, ItemStack stack) {

        }

        // Changed: Arm to HumanoidArm (Mojang mapping)
        @Override
        public HumanoidArm getMainArm() {
            return HumanoidArm.RIGHT;
        }

        @Deprecated
        @Override
        public void setTextureHolder(TextureHolder textureHolder, Layer layer, Part part) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public TextureHolder getTextureHolder(Layer layer, Part part) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public void setColorMM(TextureColors color) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public TextureColors getColorMM() {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public void setContractMM(boolean isContract) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public boolean isContractMM() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<IMultiModel> getModel(Layer layer, Part part) {
            if (layer == Layer.SKIN) {
                return Optional.ofNullable(skinModel);
            } else {
                return armorsData.getArmor(part)
                        .map(armorPart -> armorPart.getModel(layer));
            }
        }

        @Environment(EnvType.CLIENT)
        @Override
        public Optional<ResourceLocation> getTexture(Layer layer, Part part, boolean isLight) {
            if (layer == Layer.SKIN) {
                if (skinTexture == null) {
                    return Optional.empty();
                }
                return Optional.ofNullable(skinTexture.getTexture(isLight));
            } else {
                return armorsData.getArmor(part)
                        .map(armorPart -> armorPart.getTexture(layer, isLight));
            }
        }


        @Override
        public IModelCaps getCaps() {
            return caps;
        }

        @Environment(EnvType.CLIENT)
        @Override
        public boolean isArmorVisible(Part part) {
            return armorsVisible.getArmor(part).orElse(false);
        }

        @Override
        public boolean isArmorGlint(Part part) {
            return false;
        }

        @Deprecated
        @Override
        public boolean isAllowChangeTexture(Entity changer, TextureHolder textureHolder, Layer layer, Part part) {
            throw new UnsupportedOperationException();
        }

    }
}
