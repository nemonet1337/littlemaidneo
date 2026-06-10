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

    public static Container getInvAndHands(LittleMaidEntity maid) {
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
                if (slot == 0) {
                    return maid.getMainHandItem();
                } else if (slot == 1) {
                    return maid.getOffhandItem();
                }
                return inv.getItem(slot - 2);
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                if (slot == 0) {
                    ItemStack itemStack = maid.getMainHandItem();
                    if (itemStack.isEmpty() || amount <= 0) {
                        return ItemStack.EMPTY;
                    }
                    itemStack = itemStack.split(amount);
                    if (!itemStack.isEmpty()) {
                        this.setChanged();
                    }
                    return itemStack;
                } else if (slot == 1) {
                    ItemStack itemStack = maid.getOffhandItem();
                    if (itemStack.isEmpty() || amount <= 0) {
                        return ItemStack.EMPTY;
                    }
                    itemStack = itemStack.split(amount);
                    if (!itemStack.isEmpty()) {
                        this.setChanged();
                    }
                    return itemStack;
                }
                return inv.removeItem(slot - 2, amount);
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                if (slot == 0) {
                    var stack = maid.getMainHandItem();
                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    maid.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    return stack;
                } else if (slot == 1) {
                    var stack = maid.getOffhandItem();
                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    maid.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                    return stack;
                }
                return inv.removeItemNoUpdate(slot - 2);
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                if (slot == 0) {
                    maid.setItemInHand(InteractionHand.MAIN_HAND, stack);
                } else if (slot == 1) {
                    maid.setItemInHand(InteractionHand.OFF_HAND, stack);
                } else {
                    inv.setItem(slot - 2, stack);
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

}
