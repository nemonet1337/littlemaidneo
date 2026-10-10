package work.nemonet.littlemaidneo.entity;

import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.util.HasInventory;

public class LMHasInventory implements HasInventory {
    private final Container inventory;
    private int workItemSlotSize = LMNConfig.get().work.defaultWorkItemSlotSize;

    public LMHasInventory() {
        this.inventory = new SimpleContainer(18);
    }

    public LMHasInventory(int workItemSlotSize) {
        this.inventory = new SimpleContainer(18);
        this.workItemSlotSize = workItemSlotSize;
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    public int getWorkItemSlotSize() {
        return workItemSlotSize;
    }

    public void setWorkItemSlotSize(int workItemSlotSize) {
        this.workItemSlotSize = workItemSlotSize;
    }

    @Override
    public void writeInventory(ValueOutput output) {
        var list = output.childrenList("Inventory");
        for (int i = 0; i < 18; ++i) {
            var stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                var entry = list.addChild();
                entry.putByte("Slot", (byte) i);
                entry.store(ItemStack.MAP_CODEC, stack);
            }
        }
        output.putByte("workItemSlotSize", (byte) this.workItemSlotSize);
    }

    @Override
    public void readInventory(ValueInput input) {
        this.inventory.clearContent();
        for (var entry : input.childrenListOrEmpty("Inventory")) {
            int slot = entry.getByteOr("Slot", (byte) 0) & 0xFF;
            var stack = entry.read(ItemStack.MAP_CODEC).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty() && slot < 18) {
                this.inventory.setItem(slot, stack);
            }
        }
        this.workItemSlotSize = input.getByteOr("workItemSlotSize", (byte) this.workItemSlotSize) & 0xFF;
    }

    /**
     * 作業用ビュー: メインハンド → オフハンド → 18 スロット（計 20）。
     *
     * <p>仕事（料理・治癒・醸造等）では「手足も道具として使う」ため、ハンドを含めた
     * 20 スロットで走査する。Behavior 側はこのビューを使うこと。
     */
    public static Container getWorkView(LittleMaidEntity maid) {
        var inv = maid.getInventory();
        return new Container() {
            @Override
            public int getContainerSize() {
                return 20;
            }

            @Override
            public boolean isEmpty() {
                return inv.isEmpty()
                        && maid.getMainHandItem().isEmpty()
                        && maid.getOffhandItem().isEmpty();
            }

            @Override
            public ItemStack getItem(int slot) {
                if (isHandSlot(slot)) {
                    return maid.getItemInHand(handOf(slot));
                }
                return inv.getItem(slot - HAND_VIEW_SLOTS);
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                if (isHandSlot(slot)) {
                    return splitHandItem(maid, slot, amount);
                }
                return inv.removeItem(slot - HAND_VIEW_SLOTS, amount);
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                if (isHandSlot(slot)) {
                    return takeHandItem(maid, slot);
                }
                return inv.removeItemNoUpdate(slot - HAND_VIEW_SLOTS);
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                if (isHandSlot(slot)) {
                    maid.setItemInHand(handOf(slot), stack);
                } else {
                    inv.setItem(slot - HAND_VIEW_SLOTS, stack);
                }
            }

            @Override
            public void setChanged() {
                inv.setChanged();
            }

            @Override
            public boolean stillValid(Player player) {
                return inv.stillValid(player);
            }

            @Override
            public void clearContent() {
                inv.clearContent();
            }
        };
    }

    /**
     * 生活用ビュー: 18 スロットのみ（ハンドを含まない）。
     *
     * <p>給料受け取りや自己回復など「手足は別の用途で使う」処理向け。
     */
    public static Container getLifeView(LittleMaidEntity maid) {
        return maid.getInventory();
    }

    /** 作業用ビューのうちハンドを占める先頭スロット数（0=メインハンド, 1=オフハンド）。 */
    public static final int HAND_VIEW_SLOTS = 2;

    private static boolean isHandSlot(int slot) {
        return slot < HAND_VIEW_SLOTS;
    }

    private static InteractionHand handOf(int slot) {
        return slot == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    private static ItemStack splitHandItem(LittleMaidEntity maid, int slot, int amount) {
        var hand = handOf(slot);
        var stack = maid.getItemInHand(hand);
        if (stack.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        var result = stack.split(amount);
        maid.setItemInHand(hand, stack);
        return result;
    }

    private static ItemStack takeHandItem(LittleMaidEntity maid, int slot) {
        var hand = handOf(slot);
        var stack = maid.getItemInHand(hand);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        maid.setItemInHand(hand, ItemStack.EMPTY);
        return stack;
    }

}
