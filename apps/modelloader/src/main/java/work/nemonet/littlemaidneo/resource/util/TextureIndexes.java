package work.nemonet.littlemaidneo.resource.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * テクスチャパックのインデックス帯定義。
 *
 * <p><b>不変条件（保護コア B）</b>: 宣言順はテキスチャー結合互換のために <b>変更不可</b>。
 * indexMin/indexMax を変更しない範囲でのみ並べ替え可。外部テクスチャパックが
 * この順序（および各 index 帯の境界値）に依存する。
 *
 * <p>宣言順は非対称性が目立つため、ここでは <b>index 帯の自然順（昇順）</b> に整理する:
 * <pre>
 * COLOR(0x00-0x0F)
 * DEFAULT_CONTRACT_LIGHT(0x13) DEFAULT_WILD_LIGHT(0x14)
 * GUI(0x20)
 * COLOR_WILD(0x30-0x3F)
 * ARMOR_1_DAMAGED(0x40-0x49) ARMOR_2_DAMAGED(0x50-0x59)
 * COLOR_CONTRACT_LIGHT(0x60-0x6F) COLOR_WILD_LIGHT(0x70-0x7F)
 * ARMOR_1_DAMAGED_LIGHT(0x80-0x89) ARMOR_2_DAMAGED_LIGHT(0x90-0x99)
 * </pre>
 * ※ NONE(-1) は「不明」用の番兵なので先頭に置く。
 */
public enum TextureIndexes {
    NONE(-1, -1),
    COLOR(0, 0xF),
    DEFAULT_CONTRACT_LIGHT(0x13, 0x13),
    DEFAULT_WILD_LIGHT(0x14, 0x14),
    GUI(0x20, 0x20),
    COLOR_WILD(0x30, 0x3F),
    ARMOR_1_DAMAGED(0x40, 0x49),
    ARMOR_2_DAMAGED(0x50, 0x59),
    COLOR_CONTRACT_LIGHT(0x60, 0x6F),
    COLOR_WILD_LIGHT(0x70, 0x7F),
    ARMOR_1_DAMAGED_LIGHT(0x80, 0x89),
    ARMOR_2_DAMAGED_LIGHT(0x90, 0x99);

    private static final Logger LOGGER = LogManager.getLogger();
    private final int indexMin;
    private final int indexMax;

    TextureIndexes(int indexMin, int indexMax) {
        this.indexMin = indexMin;
        this.indexMax = indexMax;
    }

    public boolean isArmor() {
        return switch (this) {
            case ARMOR_1_DAMAGED, ARMOR_2_DAMAGED, ARMOR_1_DAMAGED_LIGHT, ARMOR_2_DAMAGED_LIGHT -> true;
            default -> false;
        };
    }

    public int getIndexMin() {
        return indexMin;
    }

    public int getIndexMax() {
        return indexMax;
    }

    public static TextureIndexes getTextureIndexes(int index) {
        for (TextureIndexes textureIndex : TextureIndexes.values()) {
            if (textureIndex.getIndexMin() <= index && index <= textureIndex.getIndexMax()) {
                return textureIndex;
            }
        }
        LOGGER.warn("インデックスが存在しません。 : {}", index);
        return NONE;
    }
}
