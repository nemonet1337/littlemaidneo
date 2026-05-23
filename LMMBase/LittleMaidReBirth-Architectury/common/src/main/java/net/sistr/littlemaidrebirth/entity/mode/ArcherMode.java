package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.api.mode.IRangedWeapon;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.Optional;

public class ArcherMode extends AbstractArcherMode<Item> {
    protected int cool;

    public ArcherMode(ModeType<? extends ArcherMode> modeType, String name,
            LittleMaidEntity mob) {
        super(modeType, name, mob);
    }

    @Override
    public boolean shouldExecute() {
        return (!this.mob.getProjectile(this.mob.getMainHandItem()).isEmpty())
                && super.shouldExecute();
    }

    // TODO 処理の見直し
    @Override
    protected void tickRangedAttack(LivingEntity target, ItemStack itemStack, boolean canSee, double distanceSq,
            float maxRange) {
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
                // 射線チェック、射線に味方が居る場合は撃たない
                var result = this.raycastShootLine(target, maxRange,
                        e -> e instanceof LivingEntity living && this.mob.isFriend(living));
                if (result.isPresent()) {
                    this.cool = 10;
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
                // チャージ前か、チャージしていない
                if (!this.mob.isCharging() || !this.mob.isUsingItem()) {
                    this.mob.startUsingItem(InteractionHand.MAIN_HAND);
                    this.mob.setChargingCrossbow(true);
                } else {// チャージ中
                    // チャージが終わった
                    if (this.mob.getTicksUsingItem() >= CrossbowItem.getChargeDuration(this.mob.getUseItem(),
                            this.mob)) {
                        // チャージはこのメソッドから行われる
                        this.mob.releaseUsingItem();
                        this.mob.setChargingCrossbow(false);
                        this.cool = 5;
                        this.mob.swing(InteractionHand.MAIN_HAND);
                    }
                }
            } else {// チャージ完了
                if (0 < --cool) {
                    return;
                }
                // 射線チェック
                var result = raycastShootLine(target, maxRange,
                        e -> e instanceof LivingEntity living && this.mob.isFriend(living));
                if (result.isPresent()) {
                    this.cool = 10;
                } else {// 射撃
                    this.mob.performRangedAttack(target, 1.0f);
                    // 1.21.1: CrossbowItem.setCharged removed, projectiles are consumed on
                    // performCrossbowAttack
                    this.mob.play(LMSounds.SHOOT);
                    this.mob.swing(InteractionHand.MAIN_HAND);
                }
            }
        }
    }

    protected int getInterval(ItemStack itemStack) {
        return Mth.ceil((itemStack.getItem() instanceof IRangedWeapon rangedWeapon
                ? rangedWeapon.getInterval_LMRB(itemStack, this.mob)
                : 20) / LMRBMod.getConfig().work.archerShootRateFactor);
    }

    @Override
    protected float getMaxRange(ItemStack itemStack) {
        return (itemStack.getItem() instanceof IRangedWeapon rangedWeapon
                ? rangedWeapon.getMaxRange_LMRB(itemStack, this.mob)
                : 16F)
                * LMRBMod.getConfig().work.archerShootDistanceFactor;
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.cool = 5;
        if (this.mob.isUsingItem()) {
            this.mob.stopUsingItem();
            this.mob.setChargingCrossbow(false);
        }
    }

    @Override
    protected Optional<Item> getWeaponInstance(ItemStack stack) {
        var item = stack.getItem();
        return Optional.of(item);
    }
}
