package work.nemonet.littlemaidneo.entity.goal;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManager;
import work.nemonet.littlemaidneo.entity.targeting.TargetingConfig;
import work.nemonet.littlemaidneo.entity.targeting.TargetingSystem;
import work.nemonet.littlemaidneo.entity.targeting.TargetingSystem.Maid;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

/**
 * メイドさんのターゲット選択ゴール
 * 3段階優先度システムで敵を選択し、危険な敵からの避難も処理する
 * <p>
 * 優先度階層:
 * - CRITICAL: 自分を攻撃した敵
 * - HIGH: ご主人を攻撃した敵、ご主人が攻撃した敵
 * - NORMAL: 他のメイドさんを攻撃した敵、周囲の敵対モブ
 */
public class LMTargetGoal extends Goal {

    private final LittleMaidEntity maid;
    private Mob target;
    private int recalc = 0;

    public LMTargetGoal(LittleMaidEntity maid) {
        this.maid = maid;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        int chance = LMRBConfig.get().target.targetingFrequency;
        if (this.maid.getRandom().nextInt(adjustedTickDelay(chance)) != 0) {
            return false;
        }

        return targeting();
    }

    private boolean targeting() {
        // 範囲内に敵がいるかチェック
        var aroundMobs = getAroundMobs();
        if (aroundMobs.isEmpty()) {
            this.maid.setTarget(null);
            return false;
        }
        var aroundMaids = getAroundMaids();
        TargetTagManager targetTagManager = this.maid;

        // 3段階優先度システムでターゲット選択、分散ターゲティングも考慮
        var target = TargetingSystem.selectTarget(
            new TargetingSystem.Maid(this.maid),
            aroundMobs
                .stream()
                .map(mob -> new TargetingSystem.Mob(mob))
                .toList(),
            TameableUtil.getTameOwner(this.maid)
                .map(TargetingSystem.Master::new)
                .orElse(null),
            aroundMaids.stream().map(TargetingSystem.Maid::new).toList(),
            this.maid.isBloodSuck(),
            targetTagManager
        );

        // 危険敵からの避難処理（クリーパー等から距離を取る）
        var enemies = aroundMobs
            .stream()
            .map(mob -> new TargetingSystem.Mob(mob))
            .toList();
        var maidWrapper = new TargetingSystem.Maid(this.maid);
        if (
            TargetingSystem.needsEvacuation(
                maidWrapper,
                enemies,
                targetTagManager
            )
        ) {
            TargetingSystem.getDangerousEnemies(
                maidWrapper,
                enemies,
                targetTagManager
            ).forEach(mob ->
                this.maid.addFleeEntity(
                    mob.getMob(),
                    e ->
                        !e.isAlive() ||
                        this.maid.distanceToSqr(e) >
                            (TargetingConfig.getDangerousAvoidDistance() + 4) *
                                (TargetingConfig.getDangerousAvoidDistance() +
                                    4)
                )
            );
        }

        // ターゲット設定
        if (target.isPresent()) {
            this.target = target.get();
            this.maid.setTarget(target.get());
            return true;
        }

        this.maid.setTarget(null);
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        // 攻撃を受けたら再計算(tick順の関係で実行されないことを防ぐため、ageに-1する)
        if (
            adjustedTickDelay(this.maid.getLastHurtByMobTimestamp()) ==
            adjustedTickDelay(this.maid.tickCount - 1)
        ) {
            return targeting();
        }
        // 現在のターゲットがまだ有効かチェック
        if (!isTargetable(this.target, TargetingConfig.getAlertRange())) {
            // ターゲットが居なくなったら再計算
            return targeting();
        }
        // 再計算カウンター
        recalc = Math.max(0, recalc - 1);
        if (recalc > 0) {
            recalc = adjustedTickDelay(10);
            return true;
        }
        // 状況の変化により優先度を再計算する
        return targeting();
    }

    @Override
    public void start() {
        super.start();
        // ターゲット確定時の初期設定
        recalc = adjustedTickDelay(10);
    }

    @Override
    public void stop() {
        super.stop();
        // ターゲットのクリア
        recalc = 0;
        this.target = null;
        this.maid.setTarget(null);
    }

    private List<Mob> getAroundMobs() {
        float distance = TargetingConfig.getAlertRange();
        return this.maid.level().getEntitiesOfClass(
            Mob.class,
            this.maid
                .getBoundingBox()
                .inflate(distance, distance / 2f, distance)
                .inflate(1),
            mob ->
                mob != this.maid &&
                isTargetable(mob, distance) &&
                this.maid.getSensing().hasLineOfSight(mob)
        );
    }

    private boolean isTargetable(Mob mob, float distance) {
        return (
            this.maid.distanceToSqr(mob) <= distance * distance &&
            maid.canAttack(mob) && // isFriend()とcanTakeDamage()判定込み
            mob.isAlive()
        );
    }

    private List<LittleMaidEntity> getAroundMaids() {
        float distance = TargetingConfig.getAlertRange();
        return this.maid.level().getEntitiesOfClass(
            LittleMaidEntity.class,
            this.maid
                .getBoundingBox()
                .inflate(distance, distance / 2f, distance)
                .inflate(1),
            maid -> maid != this.maid
        );
    }
}
