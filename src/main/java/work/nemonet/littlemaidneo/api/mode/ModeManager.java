package work.nemonet.littlemaidneo.api.mode;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * モードタイプを管理するクラス
 * メイド専用
 */
public class ModeManager {
    public static ModeManager INSTANCE = new ModeManager();
    private final BiMap<Identifier, ModeType<? extends Mode>> MODE_TYPES = HashBiMap.create();

    /**
     * 登録済みモードタイプを {@link Identifier} 文字列でシリアライズする Codec。
     *
     * <p>移動軸の {@code MaidMode.CODEC} と対をなし、作業モードの永続化を
     * 手書きの {@code ModeID} 文字列ではなく Codec ベースへ統一する（AI-2）。
     * 未登録 ID のデコードは {@link DataResult} のエラーとして扱う。
     */
    public final Codec<ModeType<? extends Mode>> CODEC = Identifier.CODEC.comapFlatMap(
            id -> getType(id)
                    .<DataResult<ModeType<? extends Mode>>>map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown maid mode type: " + id)),
            type -> getId(type)
                    .orElseThrow(() -> new IllegalStateException(
                            "Unregistered maid mode type: " + type)));

    public void register(Identifier id, ModeType<? extends Mode> type) {
        MODE_TYPES.put(id, type);
    }

    public Optional<Identifier> getId(Mode mode) {
        return getId(mode.getModeType());
    }

    public Optional<Identifier> getId(ModeType<?> modeType) {
        return Optional.ofNullable(MODE_TYPES.inverse().get(modeType));
    }

    public Optional<ModeType<? extends Mode>> getType(Identifier id) {
        return Optional.ofNullable(MODE_TYPES.get(id));
    }

    /**
     * メイドのモードを新規作成
     */
    public Collection<Mode> createModes(LittleMaidEntity maid) {
        return MODE_TYPES.values().stream()
                .map(type -> type.create(maid))
                .collect(Collectors.toList());
    }

}
