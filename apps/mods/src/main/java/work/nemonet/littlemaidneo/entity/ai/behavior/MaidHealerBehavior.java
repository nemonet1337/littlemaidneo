package work.nemonet.littlemaidneo.entity.ai.behavior;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import work.nemonet.littlemaidneo.entity.LMHasInventory;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.mode.ModeHelpers;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.resource.util.LMSounds;

import java.util.Map;

public class MaidHealerBehavior extends AbstractMaidBehavior {
    protected LivingEntity owner;
    protected int foodIndex;
    protected int potionIndex;
    protected int timeToRecalcPath;

    public MaidHealerBehavior() {
        super(Map.of(
                work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get(), MemoryStatus.VALUE_PRESENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity mob) {
        String job = mob.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get()).orElse("");
        if (!job.equals("healer")) {
            return false;
        }

        if (mob.getRandom().nextFloat() > 1 / 20f) {
            return false;
        }
        LivingEntity o = TameableUtil.getTameOwner(mob).orElse(null);
        if (!(o instanceof Player)) return false;
        double range = LittleMaidEntity.getConfig().work.maxTargetRange;
        if (mob.distanceToSqr(o) > range * range) return false;
        this.owner = o;
        boolean isHunger = ((Player) o).getFoodData().needsFood();
        boolean fullHealth = o.getHealth() >= o.getMaxHealth();
        return searchInventory(mob, o, isHunger, fullHealth);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        String job = mob.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get()).orElse("");
        if (!job.equals("healer")) {
            return false;
        }
        LivingEntity o = TameableUtil.getTameOwner(mob).orElse(null);
        if (!(o instanceof Player)) return false;
        double range = LittleMaidEntity.getConfig().work.maxTargetRange;
        if (mob.distanceToSqr(o) > range * range) return false;
        boolean isHunger = ((Player) o).getFoodData().needsFood();
        boolean fullHealth = o.getMaxHealth() <= o.getHealth();
        this.owner = o;
        return searchInventory(mob, o, isHunger, fullHealth);
    }

    public boolean searchInventory(LittleMaidEntity mob, LivingEntity owner, boolean isHunger, boolean fullHealth) {
        boolean result = false;
        foodIndex = -1;
        potionIndex = -1;
        Container inventory = LMHasInventory.getInvAndHands(mob);
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack stack = inventory.getItem(i);
            if (isHunger && foodIndex == -1 && isFood(stack)) {
                foodIndex = i;
                result = true;
            }
            if (potionIndex == -1 && isBeneficialPotion(owner, stack, fullHealth)) {
                potionIndex = i;
                result = true;
            }
        }
        return result;
    }

    public boolean isFood(ItemStack stack) {
        if (!stack.has(DataComponents.FOOD)) return false;
        var consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable == null) return true;
        return consumable.onConsumeEffects().stream()
                .filter(e -> e instanceof ApplyStatusEffectsConsumeEffect)
                .map(e -> (ApplyStatusEffectsConsumeEffect) e)
                .flatMap(e -> e.effects().stream())
                .noneMatch(e -> e.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL);
    }

    public boolean isBeneficialPotion(LivingEntity owner, ItemStack stack, boolean fullHealth) {
        var contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty()) {
            return false;
        }
        var potion = contents.potion().get().value();
        if (potion.getEffects().stream().anyMatch(e -> e.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL)) {
            return false;
        }
        return potion.getEffects().stream()
                .filter(e -> e.getEffect() != MobEffects.INSTANT_HEALTH || !fullHealth)
                .anyMatch(e -> owner.getActiveEffects().isEmpty() ||
                        owner.getActiveEffects().stream()
                                .noneMatch(oE -> oE.getEffect() == e.getEffect() && e.getAmplifier() <= oE.getAmplifier()));
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        if (owner == null) {
            return;
        }
        mob.getLookControl().setLookAt(owner, 10.0f, mob.getMaxHeadXRot());
        var navResult = ModeHelpers.approach(mob, owner, 1.0, timeToRecalcPath, 10, 2.0, 1);
        timeToRecalcPath = navResult.nextTimer();
        if (navResult.unreachable()) {
            return;
        }
        if (mob.distanceToSqr(owner) > 4.0) {
            return;
        }
        mob.getNavigation().stop();

        Container inventory = LMHasInventory.getInvAndHands(mob);
        if (foodIndex != -1 && foodIndex >= 0 && foodIndex < inventory.getContainerSize()) {
            ItemStack stack = inventory.getItem(foodIndex);
            if (isFood(stack)) {
                stack = stack.finishUsingItem(owner.level(), owner);
                if (stack.isEmpty()) {
                    inventory.removeItemNoUpdate(foodIndex);
                } else {
                    inventory.setItem(foodIndex, stack);
                }
                mob.play(LMSounds.HEALING);
            }
            foodIndex = -1;
        }
        if (potionIndex != -1 && potionIndex >= 0 && potionIndex < inventory.getContainerSize()) {
            ItemStack stack = inventory.getItem(potionIndex);
            boolean fullHealth = owner.getHealth() >= owner.getMaxHealth();
            if (isBeneficialPotion(owner, stack, fullHealth)) {
                stack = stack.finishUsingItem(owner.level(), owner);
                if (stack.isEmpty()) {
                    inventory.removeItemNoUpdate(potionIndex);
                } else {
                    inventory.setItem(potionIndex, stack);
                }
                owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                        SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);
                mob.play(LMSounds.HEALING_POTION);
            }
            potionIndex = -1;
        }
    }
}
