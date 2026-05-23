package net.sistr.littlemaidrebirth.api.mode;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.resources.ResourceLocation;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * モードタイプを管理するクラス
 * メイド専用
 */
public class ModeManager {
    public static ModeManager INSTANCE = new ModeManager();
    private final BiMap<ResourceLocation, ModeType<? extends Mode>> MODE_TYPES = HashBiMap.create();

    public void register(ResourceLocation id, ModeType<? extends Mode> type) {
        MODE_TYPES.put(id, type);
    }

    public Optional<ResourceLocation> getId(Mode mode) {
        return getId(mode.getModeType());
    }

    public Optional<ResourceLocation> getId(ModeType<?> modeType) {
        return Optional.ofNullable(MODE_TYPES.inverse().get(modeType));
    }

    public Optional<ModeType<? extends Mode>> getType(ResourceLocation id) {
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
