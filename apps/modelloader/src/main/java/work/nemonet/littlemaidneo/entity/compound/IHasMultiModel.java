package work.nemonet.littlemaidneo.entity.compound;

import net.minecraft.world.entity.Entity;
import work.nemonet.littlemaidneo.resource.holder.TextureHolder;
import work.nemonet.littlemaidneo.resource.util.TextureColors;

public interface IHasMultiModel extends MultiModelView {

    boolean isAllowChangeTexture(Entity changer, TextureHolder textureHolder, Layer layer, Part part);

    void setTextureHolder(TextureHolder textureHolder, Layer layer, Part part);

    TextureHolder getTextureHolder(Layer layer, Part part);

    void setColorMM(TextureColors color);

    TextureColors getColorMM();

    void setContractMM(boolean isContract);

    boolean isContractMM();

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

        /** アーマーレイヤー（INNER/OUTER）なら true。{@link #isArmor()} は歴史的に反転している。 */
        public boolean isArmorLayer() {
            return isArmor;
        }

        /**
         * SKIN のとき true。フィールド {@code isArmor} の否定であり、名前と逆。
         * 新規コードは {@link #isArmorLayer()} か {@code layer == SKIN} を使う。
         */
        public boolean isArmor() {
            return !isArmor;
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
