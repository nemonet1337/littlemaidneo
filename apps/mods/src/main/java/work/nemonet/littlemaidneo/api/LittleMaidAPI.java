package work.nemonet.littlemaidneo.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * 外部 Mod 向けの薄い公開 API。
 * <p>
 * 実装詳細（Brain / Attachment / ネットワーク）には依存せず、
 * よく使う問い合わせだけを安定したシグネチャで提供する。
 * 破壊的変更を避けるため、ここから返す型は可能な限りバニラ＋本 API の enum に留める。
 */
public final class LittleMaidAPI {
    private LittleMaidAPI() {}

    /** エンティティがリトルメイドさんならそのインスタンス。 */
    public static Optional<LittleMaidEntity> asMaid(@Nullable Entity entity) {
        if (entity instanceof LittleMaidEntity maid) {
            return Optional.of(maid);
        }
        return Optional.empty();
    }

    public static boolean isLittleMaid(@Nullable Entity entity) {
        return entity instanceof LittleMaidEntity;
    }

    /** テイム済みかつ所有者 UUID が一致するか。 */
    public static boolean isOwnedBy(LittleMaidEntity maid, Player player) {
        return TameableUtil.isTameOwner(maid, player);
    }

    public static Optional<UUID> getOwnerUuid(LittleMaidEntity maid) {
        return TameableUtil.getTameOwnerUuid(maid);
    }

    public static MaidMode getMovingMode(LittleMaidEntity maid) {
        return maid.getMaidMode();
    }

    public static void setMovingMode(LittleMaidEntity maid, MaidMode mode) {
        maid.setMaidMode(mode);
        if (mode == MaidMode.FREEDOM) {
            maid.setFreedomPos(maid.blockPosition());
        }
    }

    public static boolean isWaiting(LittleMaidEntity maid) {
        return TameableUtil.isWait(maid);
    }

    public static void setWaiting(LittleMaidEntity maid, boolean wait) {
        TameableUtil.setWait(maid, wait);
    }

    /** 現在の作業ジョブ名（"combat" / "cooking" / "none" 等）。 */
    public static String getActiveJobName(LittleMaidEntity maid) {
        return maid.getActiveJobName();
    }

    /** 表示用モード名（"Fencer" / "Archer" / "" 等）。 */
    public static Optional<String> getDisplayModeName(LittleMaidEntity maid) {
        return maid.getModeName();
    }

    public static boolean isStrike(LittleMaidEntity maid) {
        return maid.isStrike();
    }
}
