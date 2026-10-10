package work.nemonet.littlemaidneo.entity.util;

import com.mojang.serialization.Codec;

/**
 * メイドさんの戦闘スタイル（combat ジョブ時の戦闘モード）。
 *
 * <p>旧来 {@code String}（{@code "sword"} / {@code "bow"}）で扱っていた値を型安全にしたもの。
 * 永続化名は小文字で Brain メモリ（{@code active_battle_mode}）と互換。
 */
public enum BattleMode {
    NONE("none"),
    SWORD("sword"),
    BOW("bow");

    private final String serialName;

    BattleMode(String serialName) {
        this.serialName = serialName;
    }

    /** Brain メモリで保存される小文字名。 */
    public String getSerialName() {
        return serialName;
    }

    /**
     * 寛容版の名前解決（Codec デコード用）。
     * 不正値は {@link #NONE} にフォールバックする。
     */
    public static BattleMode byName(String name) {
        for (BattleMode mode : values()) {
            if (mode.serialName.equals(name)) {
                return mode;
            }
        }
        return NONE;
    }

    /** ワールド保存用 Codec。 */
    public static final Codec<BattleMode> CODEC =
            Codec.STRING.xmap(BattleMode::byName, BattleMode::getSerialName);
}
