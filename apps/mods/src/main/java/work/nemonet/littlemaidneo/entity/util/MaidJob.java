package work.nemonet.littlemaidneo.entity.util;

import com.mojang.serialization.Codec;

/**
 * メイドさんの作業ジョブ（お仕事）。
 *
 * <p>旧来 {@code String} で扱っていたジョブ名を型安全にしたもの。typo はコンパイルエラーになる。
 * 永続化名は小文字で、Brain メモリ（{@code active_job_name}）と
 * datapack（{@code littlemaidneo:maid_job}）の双方で互換。
 * 旧データの誤字 {@code "pharmcist"} は {@link #byName(String)} 読み込み時に
 * {@link #PHARMACIST} へ正規化される（tick 経路には正規化処理を残さない）。
 */
public enum MaidJob {
    NONE("none"),
    COMBAT("combat"),
    COOKING("cooking"),
    RIPPER("ripper"),
    TORCHER("torcher"),
    HEALER("healer"),
    PHARMACIST("pharmacist");

    /** 旧バージョンの誤字。読み込み時に {@link #PHARMACIST} へ正規化される。 */
    private static final String LEGACY_PHARMACIST_TYPO = "pharmcist";

    private final String serialName;

    MaidJob(String serialName) {
        this.serialName = serialName;
    }

    /** datapack / Brain メモリで保存される小文字名。 */
    public String getSerialName() {
        return serialName;
    }

    /**
     * 寛容版の名前解決（Codec デコード用）。
     * 誤字・不正値は {@link #NONE} にフォールバックする。
     */
    public static MaidJob byName(String name) {
        String normalized = LEGACY_PHARMACIST_TYPO.equals(name) ? PHARMACIST.serialName : name;
        for (MaidJob job : values()) {
            if (job.serialName.equals(normalized)) {
                return job;
            }
        }
        return NONE;
    }

    /** ワールド保存・datapack 用 Codec。 */
    public static final Codec<MaidJob> CODEC =
            Codec.STRING.xmap(MaidJob::byName, MaidJob::getSerialName);
}
