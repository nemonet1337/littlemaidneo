package work.nemonet.littlemaidneo.entity.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidCollectSalaryBehavior;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidFollowOwnerBehavior;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidFreedomBehavior;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidHealSelfBehavior;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidMoveToDropItemBehavior;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidPlaySnowBehavior;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidStareBehavior;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidStoreItemBehavior;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidStrollBehavior;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidSwim;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidTargetBehavior;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidTeleportBehavior;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidTraceBehavior;
import work.nemonet.littlemaidneo.entity.ai.behavior.MaidWaitBehavior;
import work.nemonet.littlemaidneo.entity.util.MaidJobManager;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;

/**
 * メイドさんの Brain 構成。
 * CORE は常時。非 CORE は {@link #updateActivity} で PANIC → AVOID → FIGHT → WORK → IDLE の順に一つだけ。
 */
public final class MaidBrain {
    private MaidBrain() {
    }

    public static Brain.Provider<LittleMaidEntity> provider() {
        return Brain.provider(
                ImmutableList.of(
                        ModRegistration.IS_WAITING.get(),
                        ModRegistration.OWNER.get(),
                        ModRegistration.ACTIVE_JOB_NAME.get(),
                        ModRegistration.ACTIVE_BATTLE_MODE.get(),
                        MemoryModuleType.WALK_TARGET,
                        MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                        MemoryModuleType.PATH,
                        MemoryModuleType.DOORS_TO_CLOSE,
                        MemoryModuleType.ATTACK_TARGET
                ),
                ImmutableList.of(
                        ModRegistration.LITTLE_MAID_SENSOR.get()
                ),
                MaidBrain::createActivities
        );
    }

    private static ImmutableList<ActivityData<LittleMaidEntity>> createActivities(LittleMaidEntity entity) {
        return ImmutableList.of(
                ActivityData.create(Activity.CORE, 0, coreBehaviors(entity)),
                ActivityData.create(Activity.PANIC, 0, ImmutableList.<BehaviorControl<? super LittleMaidEntity>>of(
                        entity.panicBehavior
                )),
                ActivityData.create(Activity.AVOID, 0, ImmutableList.<BehaviorControl<? super LittleMaidEntity>>of(
                        entity.avoidBehavior
                )),
                ActivityData.create(Activity.FIGHT, 0, ImmutableList.<BehaviorControl<? super LittleMaidEntity>>of(
                        entity.combatBehavior
                )),
                ActivityData.create(Activity.WORK, 0, workBehaviors(entity)),
                ActivityData.create(Activity.IDLE, 0, idleBehaviors())
        );
    }

    private static ImmutableList<BehaviorControl<? super LittleMaidEntity>> coreBehaviors(LittleMaidEntity entity) {
        return ImmutableList.of(
                new MaidSwim(0.8f),
                net.minecraft.world.entity.ai.behavior.InteractWithDoor.create(),
                new MaidWaitBehavior(),
                new MaidHealSelfBehavior(),
                new MaidTargetBehavior(),
                new MaidTeleportBehavior(),
                entity.lookAroundBehavior,
                new MaidStareBehavior(),
                new net.minecraft.world.entity.ai.behavior.MoveToTargetSink()
        );
    }

    private static ImmutableList<BehaviorControl<? super LittleMaidEntity>> workBehaviors(LittleMaidEntity entity) {
        return ImmutableList.of(
                entity.cookingBehavior,
                entity.healerBehavior,
                entity.pharmcistBehavior,
                entity.ripperBehavior,
                entity.torcherBehavior
        );
    }

    private static ImmutableList<BehaviorControl<? super LittleMaidEntity>> idleBehaviors() {
        return ImmutableList.of(
                new MaidCollectSalaryBehavior(),
                new MaidStoreItemBehavior(),
                new MaidMoveToDropItemBehavior(),
                new MaidFollowOwnerBehavior(),
                new MaidFreedomBehavior(),
                new MaidStrollBehavior(),
                new MaidTraceBehavior(),
                new MaidPlaySnowBehavior()
        );
    }

    public static Brain<LittleMaidEntity> makeBrain(LittleMaidEntity maid, Brain.Packed packed) {
        Brain<LittleMaidEntity> brain = provider().makeBrain(maid, packed);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
        return brain;
    }

    public static void updateActivity(LittleMaidEntity maid) {
        Brain<LittleMaidEntity> brain = maid.getBrain();
        if (shouldPanic(maid)) {
            brain.setActiveActivityIfPossible(Activity.PANIC);
            return;
        }
        if (!maid.fleeEntities.isEmpty()) {
            brain.setActiveActivityIfPossible(Activity.AVOID);
            return;
        }
        String job = maid.getActiveJobName();
        if (MaidJobManager.JOB_COMBAT.equals(job) && brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            brain.setActiveActivityIfPossible(Activity.FIGHT);
            return;
        }
        if (!MaidJobManager.JOB_NONE.equals(job) && !MaidJobManager.JOB_COMBAT.equals(job)) {
            brain.setActiveActivityIfPossible(Activity.WORK);
            return;
        }
        brain.setActiveActivityIfPossible(Activity.IDLE);
    }

    private static boolean shouldPanic(LittleMaidEntity maid) {
        if (TameableUtil.hasTameOwner(maid)) {
            return false;
        }
        boolean recentlyHurt = maid.getLastHurtByMob() != null
                && maid.tickCount - maid.getLastHurtByMobTimestamp() < 100;
        return recentlyHurt || maid.isOnFire();
    }
}
