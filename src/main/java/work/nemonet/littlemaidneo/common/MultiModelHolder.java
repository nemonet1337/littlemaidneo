package work.nemonet.littlemaidneo.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.compound.MultiModelCompound;
import work.nemonet.littlemaidneo.maidmodel.IModelCaps;
import work.nemonet.littlemaidneo.multimodel.IMultiModel;
import work.nemonet.littlemaidneo.resource.holder.TextureHolder;
import work.nemonet.littlemaidneo.resource.util.TextureColors;

import java.util.Optional;

public interface MultiModelHolder extends IHasMultiModel {

    MultiModelCompound getMultiModel();

    @Override
    default boolean isAllowChangeTexture(Entity changer, TextureHolder textureHolder, Layer layer, Part part) {
        return getMultiModel().isAllowChangeTexture(changer, textureHolder, layer, part);
    }

    @Override
    default void setTextureHolder(TextureHolder textureHolder, Layer layer, Part part) {
        getMultiModel().setTextureHolder(textureHolder, layer, part);
    }

    @Override
    default TextureHolder getTextureHolder(Layer layer, Part part) {
        return getMultiModel().getTextureHolder(layer, part);
    }

    @Override
    default void setColorMM(TextureColors color) {
        getMultiModel().setColorMM(color);
    }

    @Override
    default TextureColors getColorMM() {
        return getMultiModel().getColorMM();
    }

    @Override
    default void setContractMM(boolean isContract) {
        getMultiModel().setContractMM(isContract);
    }

    @Override
    default boolean isContractMM() {
        return getMultiModel().isContractMM();
    }

    @Override
    default Optional<IMultiModel> getModel(Layer layer, Part part) {
        return getMultiModel().getModel(layer, part);
    }

    @Override
    default Optional<Identifier> getTexture(Layer layer, Part part, boolean isLight) {
        return getMultiModel().getTexture(layer, part, isLight);
    }

    @Override
    default IModelCaps getCaps() {
        return getMultiModel().getCaps();
    }

    @Override
    default boolean isArmorVisible(Part part) {
        return getMultiModel().isArmorVisible(part);
    }

    @Override
    default boolean isArmorGlint(Part part) {
        return getMultiModel().isArmorGlint(part);
    }
}
