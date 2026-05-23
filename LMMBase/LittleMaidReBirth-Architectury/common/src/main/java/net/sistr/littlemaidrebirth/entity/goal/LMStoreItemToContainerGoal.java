package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class LMStoreItemToContainerGoal<T extends LittleMaidEntity> extends StoreItemToContainerGoal<T> {

    public LMStoreItemToContainerGoal(T mob, Predicate<ItemStack> exceptItems,
            Supplier<Float> searchRange) {
        super(mob, exceptItems, searchRange);
    }

    @Override
    public boolean canUse() {
        return !this.mob.isStrike()
                && TameableUtil.getTameOwnerUuid(mob).isPresent()
                && !TameableUtil.isWait(mob)
                && (this.mob.getMovingMode() == MovingMode.FREEDOM
                        || this.mob.getMovingMode() == MovingMode.TRACER)
                && super.canUse();
    }

    @Override
    protected boolean hasStoreItems() {
        Container inventory = this.mob.getInventory();
        boolean hasStoreItem = false;
        for (int i = this.mob.getWorkItemSlotSize(); i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                return false;
            }
            // 仕舞うべきアイテムならフラグを立てる
            if (!hasStoreItem && !this.exceptItems.test(stack)) {
                hasStoreItem = true;
            }
        }
        // 仕舞うべきアイテムがあればtrue
        return hasStoreItem;
    }

    // TODO チェストに仕舞うときの演出を強化する
    // TODO チェストに仕舞わない条件を追加する
    @Override
    protected void storeItems() {
        if (containerPos == null) {
            return;
        }

        Container container = HopperBlockEntity.getContainerAt(this.mob.level(), containerPos);
        if (container == null) {
            return;
        }

        this.mob.level().playSound(null, containerPos,
                SoundEvents.CHEST_OPEN, SoundSource.BLOCKS,
                1.0f, 1.0f);
        this.mob.swing(InteractionHand.MAIN_HAND);

        var inventory = this.mob.getInventory();
        for (int i = this.mob.getWorkItemSlotSize(); i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (this.exceptItems.test(stack)) {
                continue;
            }
            var newStack = HopperBlockEntity.addItem(inventory, container, stack, Direction.UP);
            inventory.setItem(i, newStack);
        }
    }

}
