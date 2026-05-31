package work.nemonet.littlemaidneo.entity;

import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import work.nemonet.littlemaidneo.entity.util.Contractable;
import work.nemonet.littlemaidneo.entity.util.HasInventory;

import java.util.function.Predicate;
import java.util.function.Supplier;

// クライアント側では概ね役に立たない
// TODO クライアント側でも活用するか、無理なら動かさずに何とかする方法を考える
public class ItemContractable<T extends LivingEntity & HasInventory> implements Contractable {
    protected final T mob;
    protected final Supplier<Integer> maxConsumeInterval;
    protected final Supplier<Integer> maxUnpaidTimes;
    protected final Predicate<ItemStack> salaryItems;
    protected int consumeInterval;
    protected int unpaidTimes;
    protected boolean contract;
    protected boolean strike;

    public ItemContractable(T mob, Supplier<Integer> maxConsumeInterval, Supplier<Integer> maxUnpaidTimes,
            Predicate<ItemStack> salaryItems) {
        this.mob = mob;
        this.maxConsumeInterval = maxConsumeInterval;
        this.maxUnpaidTimes = maxUnpaidTimes;
        this.salaryItems = salaryItems;
    }

    public void tick() {
        if (mob.level().isClientSide() || !this.contract) {
            return;
        }
        this.consumeInterval++;
        if ((mob.getId() + mob.tickCount) % 20 != 0) {
            return;
        }

        intervalTick();
    }

    protected void intervalTick() {
        if (this.maxConsumeInterval.get() < this.consumeInterval) {
            this.consumeInterval = 0;
            this.unpaidTimes++;
        }

        if (this.strike) {
            return;
        }

        nonStrikeIntervalTick();
    }

    protected void nonStrikeIntervalTick() {
        if (0 < unpaidTimes) {
            receiveSalary(mob.getInventory());
            if (maxUnpaidTimes.get() < unpaidTimes) {
                this.strike = true;
                onStrike();
            }
        }
    }

    protected void onStrike() {
    }

    public boolean isSalary(ItemStack stack) {
        return !stack.isEmpty() && this.salaryItems.test(stack);
    }

    public void receiveSalary(Container inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            while (0 < this.unpaidTimes && isSalary(stack)) {
                this.unpaidTimes--;
                stack.shrink(1);
                postReceive();
                if (stack.isEmpty()) {
                    inventory.removeItemNoUpdate(i);
                }
            }
        }
    }

    public int checkSalarySlots() {
        int count = 0;
        var inv = mob.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isSalary(stack)) {
                count++;
            }
        }
        return count;
    }

    protected void postReceive() {

    }

    public void setUnpaidTimes(int unpaidTimes) {
        this.unpaidTimes = unpaidTimes;
    }

    public int getUnpaidTimes() {
        return unpaidTimes;
    }

    @Override
    public boolean isContract() {
        return contract;
    }

    @Override
    public void setContract(boolean isContract) {
        this.contract = isContract;
    }

    @Override
    public boolean isStrike() {
        return this.strike;
    }

    @Override
    public void setStrike(boolean strike) {
        this.strike = strike;
    }

    @Override
    public void writeContractable(ValueOutput output) {
        var child = output.child("ItemContractable");
        child.putBoolean("contract", contract);
        child.putBoolean("strike", strike);
        child.putInt("consumeInterval", consumeInterval);
    }

    @Override
    public void readContractable(ValueInput input) {
        input.child("ItemContractable").ifPresent(child -> {
            contract = child.getBooleanOr("contract", false);
            strike = child.getBooleanOr("strike", false);
            consumeInterval = child.getIntOr("consumeInterval", 0);
        });
    }
}
