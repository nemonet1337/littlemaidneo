package work.nemonet.littlemaidneo.entity.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * メイドさんの移動モード（ロコモーション／スタンス）を表す enum。
 *
 * <p>旧 {@code MovingMode} を改名し、{@link #CODEC}（ワールド保存）と
 * {@link #STREAM_CODEC}（ネットワーク同期）による現代的なシリアライズ機構を備える。
 * これにより、永続化は手書きの byte 変換ではなく Codec、クライアント↔サーバ同期は
 * StreamCodec へ統一され、Mode/Goal AI と一貫した状態シリアライズ基盤になる。
 *
 * <p>値そのもの（{@link #FREEDOM}/{@link #ESCORT}/{@link #TRACER}）は表示名・lang キー
 * （{@code state.littlemaidneo.<Name>}）・外部モデルパックが参照する描画 caps
 * （{@code caps_isFreedom}/{@code caps_isTracer}）・本パラメータ（{@code moving=...}）の契約と
 * 結びついているため、既存3値の名前・IDは不変とする。
 * {@link #STROLL} は後から追加したお散歩モード（ご主人周辺の徘徊）。
 */
public enum MaidMode {
    FREEDOM("Freedom", 0),
    ESCORT("Escort", 1),
    TRACER("Tracer", 2),
    /** ご主人の周辺を散歩する（自由行動だが原点はご主人） */
    STROLL("Stroll", 3);

    private final String name;
    private final int id;

    MaidMode(String name, int id) {
        this.name = name;
        this.id = id;
    }

    /**
     * ワールド保存用 Codec。{@link #getName()} の文字列表現でシリアライズし、
     * {@code ValueOutput#store} / {@code ValueInput#read} から利用する。
     * 不明な文字列は {@link #byName(String)} 経由で {@link #FREEDOM} にフォールバックする。
     */
    public static final Codec<MaidMode> CODEC =
            Codec.STRING.xmap(MaidMode::byName, MaidMode::getName);

    /**
     * ネットワーク同期用 StreamCodec。{@link #getId()} を VarInt で送受信する。
     * 既存ペイロード（{@code ByteBufCodecs.VAR_INT.map(...)}）と同一表現で互換。
     */
    public static final StreamCodec<ByteBuf, MaidMode> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(MaidMode::fromId, MaidMode::getId);

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    /**
     * 名前から解決する寛容版（Codec デコード用）。
     * 不明な値は例外を投げず {@link #FREEDOM} を返す。
     */
    public static MaidMode byName(String name) {
        for (MaidMode mode : values()) {
            if (mode.name.equals(name)) {
                return mode;
            }
        }
        return FREEDOM;
    }

    public static MaidMode fromId(int id) {
        for (MaidMode mode : values()) {
            if (mode.getId() == id) {
                return mode;
            }
        }
        throw new IllegalArgumentException("存在しないMaidModeです。 : " + id);
    }
}
