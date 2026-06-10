package work.nemonet.littlemaidneo.entity.targeting;

import work.nemonet.littlemaidneo.config.LMNConfig;

/**
 * メイドさんターゲティングシステムの設定クラス
 * 3段階優先度システム用のシンプル設定
 */
public class TargetingConfig {

    // ========== 距離関連設定 ==========

    /**
     * 警戒範囲（先制攻撃やターゲット検出の範囲）
     */
    public static int getAlertRange() {
        return LMNConfig.get().target.alertRange;
    }

    /**
     * 戦闘範囲（実際に戦闘を行う範囲）
     */
    public static int getCombatRange() {
        return LMNConfig.get().target.combatRange;
    }

    /**
     * 危険対象回避距離
     */
    public static int getDangerousAvoidDistance() {
        return LMNConfig.get().target.dangerousAvoidDistance;
    }

    // ========== 分散ターゲティング設定 ==========

    /**
     * 分散ターゲティングの比率（メイドさん数の50%）
     */
    public static double getDistributionRatio() {
        return LMNConfig.get().target.distributionRatio;
    }

    /**
     * 一体あたりの最大攻撃者数
     */
    public static int getMaxAttackersPerTarget() {
        return LMNConfig.get().target.maxAttackersPerTarget;
    }

    // ========== 体力関連設定 ==========

    /**
     * 負傷判定の体力閾値（50%）
     */
    public static float getInjuredThreshold() {
        return LMNConfig.get().target.injuredThreshold;
    }

    /**
     * 攻撃判定の有効tick数
     */
    public static int getAttackedByValidTicks() {
        return LMNConfig.get().target.attackedByValidTicks;
    }

}