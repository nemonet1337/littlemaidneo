package work.nemonet.littlemaidneo.entity.soul;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.UUIDUtil;
import java.util.UUID;
import java.util.Optional;

public final class MaidDataFixer {
    private static final int CURRENT_VERSION = 1;

    private MaidDataFixer() {}

    /**
     * 古い形式のNBTデータを最新のスキーマにアップグレードします。
     * @param tag 変換対象のNBTタグ（非破壊的コピーを推奨するが、ここではインプレースで変換を行う）
     * @return 変換後のNBTタグ
     */
    public static CompoundTag fix(CompoundTag tag) {
        int version = tag.getInt("dataVersion").orElse(0);
        if (version >= CURRENT_VERSION) {
            return tag;
        }

        // UUIDの修復 (UUIDMost/UUIDLeast または 文字列 から int配列へ)
        Optional<Long> uuidMost = tag.getLong("UUIDMost");
        Optional<Long> uuidLeast = tag.getLong("UUIDLeast");
        if (uuidMost.isPresent() && uuidLeast.isPresent()) {
            UUID uuid = new UUID(uuidMost.get(), uuidLeast.get());
            tag.putIntArray("UUID", UUIDUtil.uuidToIntArray(uuid));
            tag.remove("UUIDMost");
            tag.remove("UUIDLeast");
        } else {
            tag.getString("UUID").ifPresent(str -> {
                try {
                    UUID uuid = UUID.fromString(str);
                    tag.putIntArray("UUID", UUIDUtil.uuidToIntArray(uuid));
                } catch (IllegalArgumentException ignored) {}
            });
        }

        // Ownerの修復 (OwnerUUID, OwnerMost/OwnerLeast から Owner int配列へ)
        tag.getString("OwnerUUID").ifPresent(str -> {
            try {
                UUID uuid = UUID.fromString(str);
                tag.putIntArray("Owner", UUIDUtil.uuidToIntArray(uuid));
                tag.remove("OwnerUUID");
            } catch (IllegalArgumentException ignored) {}
        });

        Optional<Long> ownerMost = tag.getLong("OwnerMost");
        Optional<Long> ownerLeast = tag.getLong("OwnerLeast");
        if (ownerMost.isPresent() && ownerLeast.isPresent()) {
            UUID uuid = new UUID(ownerMost.get(), ownerLeast.get());
            tag.putIntArray("Owner", UUIDUtil.uuidToIntArray(uuid));
            tag.remove("OwnerMost");
            tag.remove("OwnerLeast");
        }

        tag.putInt("dataVersion", CURRENT_VERSION);
        return tag;
    }
}
