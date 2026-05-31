package work.nemonet.littlemaidneo.entity.compound;

import net.minecraft.world.entity.Entity;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import work.nemonet.littlemaidneo.maidmodel.IModelCaps;
import work.nemonet.littlemaidneo.multimodel.IMultiModel;
import work.nemonet.littlemaidneo.resource.holder.TextureHolder;
import work.nemonet.littlemaidneo.resource.util.TextureColors;

import java.util.Optional;

public interface IHasMultiModel {

    boolean isAllowChangeTexture(Entity changer, TextureHolder textureHolder, Layer layer, Part part);

    void setTextureHolder(TextureHolder textureHolder, Layer layer, Part part);

    TextureHolder getTextureHolder(Layer layer, Part part);

    void setColorMM(TextureColors color);

    TextureColors getColorMM();

    void setContractMM(boolean isContract);

    boolean isContractMM();

    Optional<IMultiModel> getModel(Layer layer, Part part);

    @OnlyIn(Dist.CLIENT)
    Optional<Identifier> getTexture(Layer layer, Part part, boolean isLight);

    IModelCaps getCaps();

    @OnlyIn(Dist.CLIENT)
    boolean isArmorVisible(Part part);

    @OnlyIn(Dist.CLIENT)
    boolean isArmorGlint(Part part);

    enum Layer {
        SKIN(0, 0, false),
        INNER(1, 0, true),
        OUTER(2, 1, true);

        private final int index;
        private final int partIndex;
        private final boolean isArmor;

        Layer(int index, int partIndex, boolean isArmor) {
            this.index = index;
            this.partIndex = partIndex;
            this.isArmor = isArmor;
        }

        public int getIndex() {
            return index;
        }

        public int getPartIndex() {
            return partIndex;
        }

        public boolean isArmor() {
            return isArmor;
        }
    }

    enum Part {
        HEAD(3, "head"),
        BODY(2, "body"),
        LEGS(1, "legs"),
        FEET(0, "feet");

        private final int index;
        private final String partName;

        Part(int index, String partName) {
            this.index = index;
            this.partName = partName;
        }

        public static Part getPart(int index) {
            for (Part part : Part.values()) {
                if (part.getIndex() == index) {
                    return part;
                }
            }
            throw new IllegalArgumentException("そんなパーツは存在しない");
        }

        public int getIndex() {
            return index;
        }

        public String getPartName() {
            return partName;
        }
    }
}
