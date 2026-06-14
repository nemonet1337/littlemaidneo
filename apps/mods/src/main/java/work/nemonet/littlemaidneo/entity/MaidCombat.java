package work.nemonet.littlemaidneo.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.util.CrossbowItemInvoker;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.entity.util.EPEntityUtil;

import java.util.function.Predicate;

/**
 * メイドさんの「戦闘」ロジックの移譲先。
 */
public final class MaidCombat {
    private MaidCombat() {}

    public static boolean doHurtTarget(LittleMaidEntity mob, ServerLevel serverLevel, Entity target, boolean superResult) {
        if (mob.isBloodSuck()) {
            mob.play(LMSounds.ATTACK_BLOOD_SUCK);
        } else {
            mob.play(LMSounds.ATTACK);
        }
        if (superResult) {
            ItemStack mainHandStack = mob.getMainHandItem();
            Entity entity = target;
            if (target instanceof EnderDragonPart) {
                entity = ((EnderDragonPart) target).parentMob;
            }
            if (!mainHandStack.isEmpty() && entity instanceof LivingEntity) {
                try {
                    mainHandStack.getItem().hurtEnemy(mainHandStack, (LivingEntity) entity, mob);
                } catch (Exception e) {
                    LittleMaidNeo.LOGGER.error("メイドさんの攻撃時に例外が発生しました。", e);
                }
                if (mainHandStack.isEmpty()) {
                    mob.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
        }
        return superResult;
    }

    public static boolean hurtServer(LittleMaidEntity mob, ServerLevel serverLevel, DamageSource source, float amount, boolean isHurtTime, BiHurtSuper superHurt) {
        if (mob.isDeadOrDying()) {
            return superHurt.apply(serverLevel, source, amount);
        }
        if (amount <= 0 && source.getDirectEntity() instanceof Snowball) {
            mob.play(LMSounds.HURT_SNOW);
            return false;
        }
        LMNConfig config = LittleMaidEntity.getConfig();
        if (config.health.nonMobDamageImmunity && source.getEntity() == null) {
            return false;
        }
        if (config.health.immortal &&
                !source.is(DamageTypes.FELL_OUT_OF_WORLD) &&
                !source.isCreativePlayer()) {
            return false;
        }
        if (config.health.fallImmunity && source.is(DamageTypes.FALL)) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (!config.health.enableFriendlyFire &&
                attacker instanceof LivingEntity &&
                mob.isFriend((LivingEntity) attacker)) {
            return false;
        }

        float factor = config.health.generalMaidDamageFactor;
        if ((config.health.enableWorkInEmergency || !mob.isEmergency()) &&
                !TameableUtil.isWait(mob) &&
                mob.getActiveJobName().equals("combat")) {
            factor *= config.health.battleModeMaidDamageFactor;
        } else {
            factor *= config.health.nonBattleModeMaidDamageFactor;
        }
        amount *= factor;

        boolean result = superHurt.apply(serverLevel, source, amount);
        if (!isHurtTime) {
            if (result &&
                    0 < amount &&
                    TameableUtil.isWait(mob) &&
                    TameableUtil.getTameOwnerUuid(mob).isPresent()) {
                TameableUtil.setWait(mob, false);
            }
            if (!result || amount <= 0F) {
                mob.play(LMSounds.HURT_NO_DAMAGE);
            } else if (amount > 0F && mob.isBlocking()) {
                mob.play(LMSounds.HURT_GUARD);
            } else if (source.is(DamageTypes.FALL)) {
                mob.play(LMSounds.HURT_FALL);
            } else if (source.type().effects() == net.minecraft.world.damagesource.DamageEffects.BURNING) {
                mob.play(LMSounds.HURT_FIRE);
            } else {
                mob.play(LMSounds.HURT);
            }
        }
        return result;
    }

    public static boolean killedEntity(LittleMaidEntity mob, ServerLevel world, LivingEntity other, DamageSource source, boolean superResult) {
        if (mob.isBloodSuck()) {
            mob.play(LMSounds.LAUGHTER);
        }
        int xp = other.getExperienceReward(world, source.getEntity());
        if (xp > 0) {
            mob.setXpReward_LM(Math.min(100000, mob.getXpReward_LM() + xp));
        }
        return superResult;
    }

    public static void performRangedAttack(LittleMaidEntity mob, LivingEntity target, float pullProgress) {
        var stack = mob.getMainHandItem();
        var arrowStack = mob.getProjectile(stack);
        boolean isInfinite = false;
        if (arrowStack.isEmpty() && !isInfinite) {
            return;
        }
        if (stack.getItem() instanceof BowItem bowItem) {
            var arrow = ProjectileUtil.getMobArrow(mob, arrowStack, pullProgress, stack);
            if (arrowStack.getItem() instanceof ArrowItem && !isInfinite) {
                arrow.pickup = AbstractArrow.Pickup.ALLOWED;
            }
            arrow = EPEntityUtil.arrowCustomHook(bowItem, arrow);
            double xDiff = target.getX() - mob.getX();
            double yDiff = target.getEyeY() - arrow.getY();
            double zDiff = target.getZ() - mob.getZ();
            double horizonLen = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
            arrow.shoot(
                    xDiff,
                    yDiff + horizonLen * 0.025,
                    zDiff,
                    pullProgress * 3.0f * LittleMaidEntity.getConfig().work.archerShootVelocityFactor,
                    14 - 2 * 4);
            mob.playSound(
                    SoundEvents.ARROW_SHOOT,
                    1.0f,
                    1.0f / (mob.getRandom().nextFloat() * 0.4f + 1.2f) + pullProgress * 0.5f);
            mob.level().addFreshEntity(arrow);
            arrowStack.shrink(1);
        } else if (stack.getItem() instanceof CrossbowItem) {
            mob.performCrossbowAttack(mob, CrossbowItemInvoker.getSpeed(stack.get(DataComponents.CHARGED_PROJECTILES)));
        }
    }

    public static void hurtArmor(LittleMaidEntity mob, DamageSource source, float amount) {
        if (!(amount <= 0.0f)) {
            if ((amount /= 4.0f) < 1.0f) {
                amount = 1.0f;
            }
            EquipmentSlot[] armorSlots = {
                    EquipmentSlot.FEET,
                    EquipmentSlot.LEGS,
                    EquipmentSlot.CHEST,
                    EquipmentSlot.HEAD,
            };
            for (EquipmentSlot slot : armorSlots) {
                ItemStack stack = mob.getItemBySlot(slot);
                if (stack.isEmpty())
                    continue;
                var resistant = stack.get(DataComponents.DAMAGE_RESISTANT);
                if (resistant != null && resistant.isResistantTo(source))
                    continue;
                if (!stack.has(DataComponents.EQUIPPABLE))
                    continue;
                stack.hurtAndBreak((int) amount, mob, slot);
            }
        }
    }

    public static void hurtHelmet(LittleMaidEntity mob, DamageSource source, float amount) {
        if (!(amount <= 0.0f)) {
            if ((amount /= 4.0f) < 1.0f) {
                amount = 1.0f;
            }
            var stack = mob.getItemBySlot(EquipmentSlot.HEAD);
            if (stack.isEmpty())
                return;
            var resistant = stack.get(DataComponents.DAMAGE_RESISTANT);
            if (resistant != null && resistant.isResistantTo(source))
                return;
            if (!stack.has(DataComponents.EQUIPPABLE))
                return;
            stack.hurtAndBreak((int) amount, mob, EquipmentSlot.HEAD);
        }
    }

    public static ItemStack getProjectile(LittleMaidEntity mob, ItemStack stack) {
        if (!(stack.getItem() instanceof ProjectileWeaponItem ranged)) {
            return ItemStack.EMPTY;
        }
        Predicate<ItemStack> predicate = ranged.getSupportedHeldProjectiles();
        ItemStack itemStack = ProjectileWeaponItem.getHeldProjectile(mob, predicate);
        if (!itemStack.isEmpty()) {
            return EPEntityUtil.arrowCustomHook(mob, stack, itemStack);
        }
        predicate = ranged.getAllSupportedProjectiles();
        var inv = mob.getInventory();
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack itemStack2 = inv.getItem(i);
            if (predicate.test(itemStack2)) {
                return EPEntityUtil.arrowCustomHook(mob, stack, itemStack2);
            }
        }
        return EPEntityUtil.arrowCustomHook(mob, stack, ItemStack.EMPTY);
    }

    @FunctionalInterface
    public interface BiHurtSuper {
        boolean apply(ServerLevel serverLevel, DamageSource source, float amount);
    }
}
