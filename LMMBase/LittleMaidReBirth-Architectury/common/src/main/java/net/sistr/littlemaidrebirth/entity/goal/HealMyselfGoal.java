package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.util.HasInventory;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class HealMyselfGoal<T extends PathfinderMob & HasInventory> extends Goal {
    protected final T mob;
    protected final Supplier<Integer> healInterval;
    protected final Supplier<Integer> healAmount;
    protected final Predicate<ItemStack> healItemPred;
    protected int cool;
    protected int healItemSlot = -1;

    public HealMyselfGoal(T mob, Supplier<Integer> healInterval, Supplier<Integer> healAmount, Predicate<ItemStack> healItemPred) {
        this.mob = mob;
        this.healInterval = healInterval;
        this.healAmount = healAmount;
        this.healItemPred = healItemPred;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        //体力がフルならfalse
        if (isHealthFull()) {
            return false;
        }

        //無敵時間中で、体力に余裕があるならfalse
        if (hasHurtTime() && isEnoughHealth()) {
            return false;
        }

        //回復アイテム存在チェック
        this.healItemSlot = findHealItemSlot();
        if (this.healItemSlot == -1) {
            return false;
        }
        return true;
    }

    protected boolean isHealthFull() {
        return mob.getHealth() >= mob.getMaxHealth();
    }

    protected boolean hasHurtTime() {
        return mob.hurtTime > 0;
    }

    protected boolean isEnoughHealth() {
        return mob.getHealth() / mob.getMaxHealth() > LMRBMod.getConfig().health.healDelayThreshold;
    }

    @Override
    public boolean canContinueToUse() {
        //体力満タンで終了
        if (isHealthFull()) {
            return false;
        }
        //回復アイテムスロットを更新
        healItemSlot = findHealItemSlot();
        return healItemSlot != -1;
    }

    @Override
    public void start() {
        super.start();
        this.mob.getNavigation().stop();
        cool = 0;
    }

    @Override
    public void tick() {
        //回復インターバル
        if (0 < cool--) {
            return;
        }
        cool = healInterval.get();

        var healItem = getHealItem(healItemSlot);
        //回復アイテム存在チェック
        if (!isHealItem(healItem)) {
            healItemSlot = -1;
            return;
        }

        //回復実行
        heal(healItem);
    }

    public void heal(ItemStack healItem) {
        //回復
        mob.heal(healAmount.get());
        //アイテム消費
        consumeHealItem(healItem);
        //回復演出
        mob.playSound(SoundEvents.ITEM_PICKUP, 1.0F, mob.getRandom().nextFloat() * 0.1F + 1.0F);
        mob.swing(InteractionHand.MAIN_HAND);
    }

    public int findHealItemSlot() {
        var inventory = this.mob.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (healItemPred.test(slotStack)) {
                healItemSlot = i;
                return i;
            }
        }
        return -1;
    }

    public ItemStack getHealItem(int slot) {
        if (slot == -1) return ItemStack.EMPTY;

        var stack = this.mob.getInventory().getItem(slot);
        if (!isHealItem(stack)) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    public boolean isHealItem(ItemStack stack) {
        return healItemPred.test(stack);
    }

    public void consumeHealItem(ItemStack healItem) {
        //消費量のコンフィグ追加はさすがに要らない？
        healItem.shrink(1);
        if (healItem.isEmpty()) {
            this.mob.getInventory().removeItemNoUpdate(healItemSlot);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
