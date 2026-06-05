package work.nemonet.littlemaidneo.entity.mode;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import work.nemonet.littlemaidneo.api.mode.IRangedWeapon;
import work.nemonet.littlemaidneo.api.mode.Mode;
import work.nemonet.littlemaidneo.api.mode.ModeType;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.tags.LMTags;
import work.nemonet.littlemaidneo.util.ReachAttributeUtil;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * 戦闘モードを統合した単一モード（AI-4）。
 *
 * <p>旧 {@code Fencer}（近接）と {@code Archer}（射撃）の 2 つの登録モードを 1 つに統合し、
 * メインハンドの武器種に応じて近接／射撃のスタイルを動的に切り替える。
 */
public class CombatMode extends Mode {

    private final LittleMaidEntity mob;
    private final MeleeStyle melee;
    private final RangedStyle ranged;
    @Nullable
    private BattleStyle active;

    public CombatMode(ModeType<? extends Mode> modeType, String name, LittleMaidEntity mob, float meleeSpeed) {
        super(modeType, name);
        this.mob = mob;
        this.melee = new MeleeStyle(mob, meleeSpeed);
        this.ranged = new RangedStyle(mob);
    }

    /**
     * メインハンドの武器種から使用すべき戦闘スタイルを選ぶ。
     * 近接武器でもあり射撃武器でもある場合は近接を優先（旧登録順 fencer&lt;archer のタイブレーク互換）。
     */
    private BattleStyle selectStyle() {
        ItemStack main = mob.getMainHandItem();
        Item item = main.getItem();
        boolean melee = main.has(DataComponents.WEAPON)
                || item instanceof AxeItem
                || main.is(LMTags.Items.FENCER_MODE);
        if (melee) {
            return this.melee;
        }
        boolean ranged = item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof IRangedWeapon
                || main.is(LMTags.Items.ARCHER_MODE);
        return ranged ? this.ranged : this.melee;
    }

    @Override
    public boolean shouldExecute() {
        BattleStyle style = selectStyle();
        boolean ok = style.shouldExecute();
        this.active = ok ? style : null;
        return ok;
    }

    @Override
    public boolean shouldContinueExecuting() {
        BattleStyle a = this.active;
        if (a == null) {
            return false;
        }
        // 武器を持ち替えてスタイルが変わったら一旦終了させ、再選択させる。
        if (selectStyle() != a) {
            return false;
        }
        return a.shouldContinueExecuting();
    }

    @Override
    public void startExecuting() {
        BattleStyle a = this.active;
        if (a != null) {
            a.startExecuting();
        }
    }

    @Override
    public void tick() {
        BattleStyle a = this.active;
        if (a != null) {
            a.tick();
        }
    }

    @Override
    public void resetTask() {
        BattleStyle a = this.active;
        if (a != null) {
            a.resetTask();
            this.active = null;
        }
    }

    @Override
    public boolean isBattleMode() {
        return true;
    }

    @Override
    public BattleModeType getBattleModeType() {
        BattleStyle a = this.active;
        // 非実行時（ターゲット未取得など）でも武器種から推定して返す。
        return (a != null ? a : selectStyle()).getBattleModeType();
    }

    /**
     * 描画 caps（{@code caps_job}）用のジョブ名。外部モデルパック互換のため
     * 従来の {@code "fencer"} / {@code "archer"} を返す。
     */
    @Override
    public String getJobName() {
        return selectStyle() == this.ranged ? "archer" : "fencer";
    }

    // -----------------------------------------------------------------
    // 内部スタイル基盤および実装クラス
    // -----------------------------------------------------------------

    private abstract static class BattleStyle {
        protected final LittleMaidEntity mob;
        protected LivingEntity target;
        protected ItemStack weaponStack;

        protected BattleStyle(LittleMaidEntity mob) {
            this.mob = mob;
        }

        public boolean shouldExecute() {
            if (this.mob.getTarget() == null || !this.mob.getTarget().isAlive()) {
                return false;
            }
            this.target = this.mob.getTarget();

            var main = this.mob.getMainHandItem();
            if (isWeapon(main)) {
                this.weaponStack = main;
                return true;
            }
            return false;
        }

        public boolean shouldContinueExecuting() {
            return this.shouldExecute();
        }

        public void startExecuting() {}
        public void tick() {}
        public void resetTask() {}

        public abstract BattleModeType getBattleModeType();
        protected abstract boolean isWeapon(ItemStack stack);
    }

    private static class MeleeStyle extends BattleStyle {
        private final float speed;
        private int cooldown;
        private int recalcPathCool = 0;
        private final int maxRecalcPathCool = 10;

        public MeleeStyle(LittleMaidEntity mob, float speed) {
            super(mob);
            this.speed = speed;
        }

        @Override
        public void startExecuting() {
            this.mob.play(LMSounds.FIND_TARGET_N);
        }

        @Override
        public void tick() {
            this.cooldown = Math.max(0, this.cooldown - 1);
            
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            recalcPathCool = Math.max(0, recalcPathCool - 1);

            double boundingDistSq = getBoundingDistanceSq(this.target);
            if (!isClose(boundingDistSq)) {
                if (recalcPathCool <= 0) {
                    recalcPathCool = maxRecalcPathCool;
                    this.mob.getNavigation().moveTo(this.target, this.speed);
                }
            } else {
                this.mob.getNavigation().stop();
                if (canAttack()) {
                    attack();
                }
            }
        }

        private double getBoundingDistanceSq(Entity target) {
            double distance = this.mob.distanceTo(target) - (this.mob.getBbWidth() + target.getBbWidth()) / 2;
            return distance * distance;
        }

        private boolean isClose(double distanceSq) {
            return distanceSq < ReachAttributeUtil.getAttackRangeSq(mob)
                    * LMRBConfig.get().work.fencerAttackDistanceFactor;
        }

        private boolean canAttack() {
            return this.cooldown <= 0 && this.target.invulnerableTime <= 10 
                    && this.mob.getSensing().hasLineOfSight(this.target);
        }

        private void attack() {
            resetCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget((ServerLevel) this.mob.level(), target);
        }

        private void resetCooldown() {
            double attackSpeed = this.mob.getAttributeValue(Attributes.ATTACK_SPEED);
            this.cooldown = Mth.ceil(1 / attackSpeed * 20
                    / LMRBConfig.get().work.fencerAttackRateFactor);
        }

        @Override
        public BattleModeType getBattleModeType() {
            return BattleModeType.SWORD;
        }

        @Override
        protected boolean isWeapon(ItemStack stack) {
            return true;
        }
    }

    private static class RangedStyle extends BattleStyle {
        private int seeTime;
        private boolean strafingClockwise;
        private boolean strafingBackwards;
        private int strafingTime = -1;
        private int cool;

        public RangedStyle(LittleMaidEntity mob) {
            super(mob);
        }

        @Override
        public boolean shouldExecute() {
            return (!this.mob.getProjectile(this.mob.getMainHandItem()).isEmpty())
                    && super.shouldExecute();
        }

        @Override
        public void startExecuting() {
            this.mob.setAggressive(true);
            this.mob.setAimingBow(true);
            this.mob.play(LMSounds.FIND_TARGET_N);
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) {
                return;
            }
            double distanceSq = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
            boolean canSee = this.mob.getSensing().hasLineOfSight(target);
            ItemStack itemStack = this.mob.getMainHandItem();
            float maxRange = getMaxRange(itemStack);
            boolean prevCanSee = 0 < this.seeTime;
            if (canSee != prevCanSee) {
                this.seeTime = 0;
            }
            if (prevCanSee && !canSee) {
                this.strafingTime = 0;
                this.strafingClockwise = !this.strafingClockwise;
            }

            if (canSee) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            if (distanceSq < maxRange * maxRange) {
                ++this.strafingTime;
            } else {
                this.strafingTime = 0;
            }

            if (20 <= this.strafingTime) {
                if ((double) this.mob.getRandom().nextFloat() < 0.1D) {
                    this.strafingClockwise = !this.strafingClockwise;
                }
                this.strafingTime = 0;
            }

            if (maxRange * maxRange < distanceSq) {
                this.strafingBackwards = false;
            } else if (distanceSq < maxRange * maxRange * 0.75F) {
                this.strafingBackwards = true;
            }

            this.mob.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
            this.mob.lookAt(target, 30.0F, 30.0F);
            this.mob.getLookControl().setLookAt(target, 30f, 30f);

            if (!canSee) {
                if (this.mob.isUsingItem()) {
                    this.mob.stopUsingItem();
                }
                this.mob.setChargingCrossbow(false);
                return;
            }

            if (itemStack.getItem() instanceof BowItem) {
                if (0 < --cool) {
                    return;
                }
                if (!this.mob.isUsingItem()) {
                    mob.play(LMSounds.SIGHTING);
                    this.mob.startUsingItem(InteractionHand.MAIN_HAND);
                }
                int interval = getInterval(itemStack);
                if (interval <= this.mob.getTicksUsingItem()) {
                    var result = raycastShootLine(target, maxRange,
                            e -> e instanceof LivingEntity living && this.mob.isFriend(living));
                    if (result.isPresent()) {
                        this.cool = 10;
                        this.mob.stopUsingItem();
                    } else {
                        this.cool = 5;
                        this.mob.stopUsingItem();
                        this.mob.performRangedAttack(target, 1.0f);
                        this.mob.play(LMSounds.SHOOT);
                        this.mob.swing(InteractionHand.MAIN_HAND);
                        itemStack.hurtAndBreak(1, this.mob, EquipmentSlot.MAINHAND);
                    }
                }
            } else if (itemStack.getItem() instanceof CrossbowItem) {
                if (!itemStack.has(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES)
                        || itemStack.get(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES).isEmpty()) {
                    if (!this.mob.isCharging() || !this.mob.isUsingItem()) {
                        this.mob.startUsingItem(InteractionHand.MAIN_HAND);
                        this.mob.setChargingCrossbow(true);
                    } else {
                        if (this.mob.getTicksUsingItem() >= CrossbowItem.getChargeDuration(this.mob.getUseItem(),
                                this.mob)) {
                            this.mob.releaseUsingItem();
                            this.mob.setChargingCrossbow(false);
                            this.cool = 5;
                            this.mob.swing(InteractionHand.MAIN_HAND);
                        }
                    }
                } else {
                    if (0 < --cool) {
                        return;
                    }
                    var result = raycastShootLine(target, maxRange,
                            e -> e instanceof LivingEntity living && this.mob.isFriend(living));
                    if (result.isPresent()) {
                        this.cool = 10;
                    } else {
                        this.mob.performRangedAttack(target, 1.0f);
                        this.mob.play(LMSounds.SHOOT);
                        this.mob.swing(InteractionHand.MAIN_HAND);
                    }
                }
            }
        }

        private Optional<EntityHitResult> raycastShootLine(LivingEntity target, float maxRange, Predicate<Entity> predicate) {
            var targetAt = target.getEyePosition();
            var toTargetVec = targetAt.subtract(this.mob.getEyePosition()).normalize();
            Vec3 start = this.mob.getEyePosition(1F);
            Vec3 end = start.add(toTargetVec.scale(maxRange));
            AABB box = new AABB(start, end).inflate(1D);
            var result = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(mob.level(), this.mob, start, end, box, predicate, 1.0f);
            return Optional.ofNullable(result);
        }

        private int getInterval(ItemStack itemStack) {
            return Mth.ceil((itemStack.getItem() instanceof IRangedWeapon rangedWeapon
                    ? rangedWeapon.getInterval_LMRB(itemStack, this.mob)
                    : 20) / LMRBConfig.get().work.archerShootRateFactor);
        }

        private float getMaxRange(ItemStack itemStack) {
            return (itemStack.getItem() instanceof IRangedWeapon rangedWeapon
                    ? rangedWeapon.getMaxRange_LMRB(itemStack, this.mob)
                    : 16F)
                    * LMRBConfig.get().work.archerShootDistanceFactor;
        }

        @Override
        public void resetTask() {
            this.mob.setAggressive(false);
            this.mob.setAimingBow(false);
            this.seeTime = 0;
            this.cool = 5;
            if (this.mob.isUsingItem()) {
                this.mob.stopUsingItem();
                this.mob.setChargingCrossbow(false);
            }
        }

        @Override
        public BattleModeType getBattleModeType() {
            return BattleModeType.BOW;
        }

        @Override
        protected boolean isWeapon(ItemStack stack) {
            return true;
        }
    }
}
