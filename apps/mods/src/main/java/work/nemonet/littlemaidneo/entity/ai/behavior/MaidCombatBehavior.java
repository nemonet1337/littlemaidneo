package work.nemonet.littlemaidneo.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MaidJobManager;
import work.nemonet.littlemaidneo.item.IRangedWeapon;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.util.ReachAttributeUtil;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class MaidCombatBehavior extends AbstractMaidBehavior {
    private final MeleeStyle melee;
    private final RangedStyle ranged;
    @Nullable
    private BattleStyle active;

    public MaidCombatBehavior() {
        super(Map.of(
                work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get(), MemoryStatus.VALUE_PRESENT
        ));
        this.melee = new MeleeStyle(meleeSpeed -> 1.0f); // default speed factor is 1.0
        this.ranged = new RangedStyle();
    }

    private BattleStyle selectStyle(LittleMaidEntity mob) {
        String battleMode = mob.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_BATTLE_MODE.get()).orElse(MaidJobManager.BATTLE_SWORD);
        return battleMode.equals(MaidJobManager.BATTLE_BOW) ? this.ranged : this.melee;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity mob) {
        String job = mob.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get()).orElse("");
        if (!job.equals("combat")) {
            return false;
        }
        BattleStyle style = selectStyle(mob);
        boolean ok = style.shouldExecute(mob);
        this.active = ok ? style : null;
        return ok;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        String job = mob.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get()).orElse("");
        if (!job.equals("combat")) {
            return false;
        }
        BattleStyle a = this.active;
        if (a == null) {
            return false;
        }
        if (selectStyle(mob) != a) {
            return false;
        }
        return a.shouldContinueExecuting(mob);
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        BattleStyle a = this.active;
        if (a != null) {
            a.startExecuting(mob);
        }
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        BattleStyle a = this.active;
        if (a != null) {
            a.tick(mob);
        }
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        BattleStyle a = this.active;
        if (a != null) {
            a.resetTask(mob);
            this.active = null;
        }
    }

    private abstract static class BattleStyle {
        protected LivingEntity target;
        protected ItemStack weaponStack;

        public boolean shouldExecute(LittleMaidEntity mob) {
            if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
                return false;
            }
            this.target = mob.getTarget();
            var main = mob.getMainHandItem();
            if (isWeapon(main)) {
                this.weaponStack = main;
                return true;
            }
            return false;
        }

        public boolean shouldContinueExecuting(LittleMaidEntity mob) {
            return this.shouldExecute(mob);
        }

        public void startExecuting(LittleMaidEntity mob) {}
        public void tick(LittleMaidEntity mob) {}
        public void resetTask(LittleMaidEntity mob) {}

        protected abstract boolean isWeapon(ItemStack stack);
    }

    private static class MeleeStyle extends BattleStyle {
        private final java.util.function.Function<Float, Float> speedFunc;
        private int cooldown;
        private int recalcPathCool = 0;

        public MeleeStyle(java.util.function.Function<Float, Float> speedFunc) {
            this.speedFunc = speedFunc;
        }

        @Override
        public void startExecuting(LittleMaidEntity mob) {
            mob.play(LMSounds.FIND_TARGET_N);
        }

        @Override
        public void tick(LittleMaidEntity mob) {
            this.cooldown = Math.max(0, this.cooldown - 1);
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            recalcPathCool = Math.max(0, recalcPathCool - 1);

            ItemStack offhand = mob.getOffhandItem();
            ItemStack mainhand = mob.getMainHandItem();
            InteractionHand shieldHand = null;
            if (offhand.getItem() instanceof net.minecraft.world.item.ShieldItem) {
                shieldHand = InteractionHand.OFF_HAND;
            } else if (mainhand.getItem() instanceof net.minecraft.world.item.ShieldItem) {
                shieldHand = InteractionHand.MAIN_HAND;
            }

            double boundingDistSq = getBoundingDistanceSq(mob, this.target);
            if (!isClose(mob, boundingDistSq)) {
                if (recalcPathCool <= 0) {
                    int maxRecalcPathCool = 10;
                    recalcPathCool = maxRecalcPathCool;
                    mob.getNavigation().moveTo(this.target, speedFunc.apply(1.0f));
                }
                if (shieldHand != null && !mob.isUsingItem() && mob.getSensing().hasLineOfSight(this.target)) {
                    mob.startUsingItem(shieldHand);
                }
            } else {
                mob.getNavigation().stop();
                if (canAttack(mob)) {
                    if (mob.isUsingItem()) {
                        mob.stopUsingItem();
                    }
                    attack(mob);
                } else {
                    if (shieldHand != null && !mob.isUsingItem()) {
                        mob.startUsingItem(shieldHand);
                    }
                }
            }
        }

        @Override
        public void resetTask(LittleMaidEntity mob) {
            if (mob.isUsingItem()) {
                mob.stopUsingItem();
            }
        }

        private double getBoundingDistanceSq(LittleMaidEntity mob, Entity target) {
            double distance = mob.distanceTo(target) - (mob.getBbWidth() + target.getBbWidth()) / 2;
            return distance * distance;
        }

        private boolean isClose(LittleMaidEntity mob, double distanceSq) {
            return distanceSq < ReachAttributeUtil.getAttackRangeSq(mob)
                    * LMNConfig.get().work.fencerAttackDistanceFactor;
        }

        private boolean canAttack(LittleMaidEntity mob) {
            return this.cooldown <= 0 && this.target.invulnerableTime <= 10
                    && mob.getSensing().hasLineOfSight(this.target);
        }

        private void attack(LittleMaidEntity mob) {
            resetCooldown(mob);
            mob.swing(InteractionHand.MAIN_HAND);
            mob.doHurtTarget((ServerLevel) mob.level(), target);
        }

        private void resetCooldown(LittleMaidEntity mob) {
            double attackSpeed = mob.getAttributeValue(Attributes.ATTACK_SPEED);
            this.cooldown = Mth.ceil(1 / attackSpeed * 20
                    / LMNConfig.get().work.fencerAttackRateFactor);
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

        @Override
        public boolean shouldExecute(LittleMaidEntity mob) {
            return (!mob.getProjectile(mob.getMainHandItem()).isEmpty())
                    && super.shouldExecute(mob);
        }

        @Override
        public void startExecuting(LittleMaidEntity mob) {
            mob.setAggressive(true);
            mob.setAimingBow(true);
            mob.play(LMSounds.FIND_TARGET_N);
            mob.getNavigation().stop();
        }

        @Override
        public void tick(LittleMaidEntity mob) {
            LivingEntity target = mob.getTarget();
            if (target == null) {
                return;
            }
            double distanceSq = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
            boolean canSee = mob.getSensing().hasLineOfSight(target);
            ItemStack itemStack = mob.getMainHandItem();
            float maxRange = getMaxRange(mob, itemStack);
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
                if ((double) mob.getRandom().nextFloat() < 0.1D) {
                    this.strafingClockwise = !this.strafingClockwise;
                }
                this.strafingTime = 0;
            }

            if (maxRange * maxRange < distanceSq) {
                this.strafingBackwards = false;
            } else if (distanceSq < maxRange * maxRange * 0.75F) {
                this.strafingBackwards = true;
            }

            mob.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
            mob.lookAt(target, 30.0F, 30.0F);
            mob.getLookControl().setLookAt(target, 30f, 30f);

            if (!canSee) {
                if (mob.isUsingItem()) {
                    mob.stopUsingItem();
                }
                mob.setChargingCrossbow(false);
                return;
            }

            if (itemStack.getItem() instanceof BowItem) {
                if (0 < --cool) {
                    return;
                }
                if (!mob.isUsingItem()) {
                    mob.play(LMSounds.SIGHTING);
                    mob.startUsingItem(InteractionHand.MAIN_HAND);
                }
                int interval = getInterval(mob, itemStack);
                if (interval <= mob.getTicksUsingItem()) {
                    var result = raycastShootLine(mob, target, maxRange,
                            e -> e instanceof LivingEntity living && mob.isFriend(living));
                    if (result.isPresent()) {
                        this.cool = 10;
                        mob.stopUsingItem();
                    } else {
                        this.cool = 5;
                        mob.stopUsingItem();
                        mob.performRangedAttack(target, 1.0f);
                        mob.play(LMSounds.SHOOT);
                        mob.swing(InteractionHand.MAIN_HAND);
                        itemStack.hurtAndBreak(1, mob, EquipmentSlot.MAINHAND);
                    }
                }
            } else if (itemStack.getItem() instanceof CrossbowItem) {
                if (!itemStack.has(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES)
                        || itemStack.get(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES).isEmpty()) {
                    if (!mob.isCharging() || !mob.isUsingItem()) {
                        mob.startUsingItem(InteractionHand.MAIN_HAND);
                        mob.setChargingCrossbow(true);
                    } else {
                        if (mob.getTicksUsingItem() >= CrossbowItem.getChargeDuration(mob.getUseItem(), mob)) {
                            ItemStack crossbow = mob.getMainHandItem();
                            ItemStack projectile = mob.getProjectile(crossbow);
                            if (!projectile.isEmpty()) {
                                ItemStack loadStack = projectile.copy();
                                loadStack.setCount(1);
                                if (!mob.hasInfiniteMaterials()) {
                                    projectile.shrink(1);
                                }
                                var charged = net.minecraft.world.item.component.ChargedProjectiles.of(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(loadStack));
                                crossbow.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES, charged);
                                mob.playSound(net.minecraft.sounds.SoundEvents.CROSSBOW_LOADING_END.value(), 1.0F, 1.0F / (mob.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
                            }
                            mob.stopUsingItem();
                            mob.setChargingCrossbow(false);
                            this.cool = 5;
                            mob.swing(InteractionHand.MAIN_HAND);
                        }
                    }
                } else {
                    if (0 < --cool) {
                        return;
                    }
                    var result = raycastShootLine(mob, target, maxRange,
                            e -> e instanceof LivingEntity living && mob.isFriend(living));
                    if (result.isPresent()) {
                        this.cool = 10;
                    } else {
                        mob.performRangedAttack(target, 1.0f);
                        mob.play(LMSounds.SHOOT);
                        mob.swing(InteractionHand.MAIN_HAND);
                    }
                }
            }
        }

        private Optional<EntityHitResult> raycastShootLine(LittleMaidEntity mob, LivingEntity target, float maxRange, Predicate<Entity> predicate) {
            var targetAt = target.getEyePosition();
            var toTargetVec = targetAt.subtract(mob.getEyePosition()).normalize();
            Vec3 start = mob.getEyePosition(1F);
            Vec3 end = start.add(toTargetVec.scale(maxRange));
            AABB box = new AABB(start, end).inflate(1D);
            var result = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(mob.level(), mob, start, end, box, predicate, 1.0f);
            return Optional.ofNullable(result);
        }

        private int getInterval(LittleMaidEntity mob, ItemStack itemStack) {
            return Mth.ceil((itemStack.getItem() instanceof IRangedWeapon rangedWeapon
                    ? rangedWeapon.getInterval_LM(itemStack, mob)
                    : 20) / LMNConfig.get().work.archerShootRateFactor);
        }

        private float getMaxRange(LittleMaidEntity mob, ItemStack itemStack) {
            return (itemStack.getItem() instanceof IRangedWeapon rangedWeapon
                    ? rangedWeapon.getMaxRange_LM(itemStack, mob)
                    : 16F)
                    * LMNConfig.get().work.archerShootDistanceFactor;
        }

        @Override
        public void resetTask(LittleMaidEntity mob) {
            mob.setAggressive(false);
            mob.setAimingBow(false);
            this.seeTime = 0;
            this.cool = 5;
            if (mob.isUsingItem()) {
                mob.stopUsingItem();
                mob.setChargingCrossbow(false);
            }
        }

        @Override
        protected boolean isWeapon(ItemStack stack) {
            return true;
        }
    }
}
