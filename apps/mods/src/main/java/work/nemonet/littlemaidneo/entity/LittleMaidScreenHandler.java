package work.nemonet.littlemaidneo.entity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.setup.ModRegistration;

public class LittleMaidScreenHandler extends AbstractContainerMenu {
    private final Inventory playerInventory;
    private final Container maidInventory;
    private final Container handInventory;
    private final Container armorInventory;
    private final Container headCosmeticInventory;
    private final LittleMaidEntity maid;
    private final int unpaidDays;
    private final int workItemSlotSize;

    public LittleMaidScreenHandler(int syncId, Inventory playerInventory, FriendlyByteBuf packet) {
        this(syncId, playerInventory, packet.readVarInt(), packet.readByte(), packet.readByte());
    }

    public LittleMaidScreenHandler(int syncId, Inventory playerInventory, int entityId, int unpaidDays,
            int workItemSlotSize) {
        super(ModRegistration.LITTLE_MAID_SCREEN_HANDLER.get(), syncId);
        this.playerInventory = playerInventory;
        this.unpaidDays = unpaidDays;
        this.workItemSlotSize = workItemSlotSize;

        LittleMaidEntity maid = (LittleMaidEntity) playerInventory.player.level().getEntity(entityId);
        this.maid = maid;
        if (maid == null) {
            throw new RuntimeException("メイドさんが存在しません。");
        } else {
            this.maidInventory = maid.getInventory();
            this.handInventory = new Container() {
                private EquipmentSlot index(int slot) {
                    return slot == 0 ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                }

                @Override
                public ItemStack getItem(int slot) {
                    EquipmentSlot equipmentSlot = index(slot);
                    return maid.getItemBySlot(equipmentSlot);
                }

                @Override
                public ItemStack removeItem(int slot, int amount) {
                    EquipmentSlot equipmentSlot = index(slot);
                    return maid.getItemBySlot(equipmentSlot).split(amount);
                }

                @Override
                public ItemStack removeItemNoUpdate(int slot) {
                    ItemStack result;
                    EquipmentSlot equipmentSlot = index(slot);
                    result = maid.getItemBySlot(equipmentSlot);
                    maid.setItemSlot(equipmentSlot, ItemStack.EMPTY);
                    return result;
                }

                @Override
                public void setItem(int slot, ItemStack stack) {
                    EquipmentSlot equipmentSlot = index(slot);
                    maid.setItemSlot(equipmentSlot, stack);
                }

                @Override
                public boolean isEmpty() {
                    return maid.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()
                            && maid.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty();
                }

                @Override
                public int getContainerSize() {
                    return 2;
                }

                @Override
                public boolean stillValid(Player player) {
                    return true;
                }

                @Override
                public void setChanged() {

                }

                @Override
                public void clearContent() {

                }
            };
            this.armorInventory = new Container() {
                private EquipmentSlot index(int slot) {
                    return switch (slot) {
                        case 0 -> EquipmentSlot.FEET;
                        case 1 -> EquipmentSlot.LEGS;
                        case 2 -> EquipmentSlot.CHEST;
                        default -> EquipmentSlot.HEAD;
                    };
                }

                @Override
                public ItemStack getItem(int slot) {
                    EquipmentSlot equipmentSlot = index(slot);
                    return maid.getItemBySlot(equipmentSlot);
                }

                @Override
                public ItemStack removeItem(int slot, int amount) {
                    EquipmentSlot equipmentSlot = index(slot);
                    return maid.getItemBySlot(equipmentSlot).split(amount);
                }

                @Override
                public ItemStack removeItemNoUpdate(int slot) {
                    ItemStack result;
                    EquipmentSlot equipmentSlot = index(slot);
                    result = maid.getItemBySlot(equipmentSlot);
                    maid.setItemSlot(equipmentSlot, ItemStack.EMPTY);
                    return result;
                }

                @Override
                public void setItem(int slot, ItemStack stack) {
                    EquipmentSlot equipmentSlot = index(slot);
                    maid.setItemSlot(equipmentSlot, stack);
                }

                @Override
                public boolean isEmpty() {
                    return maid.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                            && maid.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                            && maid.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                            && maid.getItemBySlot(EquipmentSlot.FEET).isEmpty();
                }

                @Override
                public int getContainerSize() {
                    return 4;
                }

                @Override
                public boolean stillValid(Player player) {
                    return true;
                }

                @Override
                public void setChanged() {

                }

                @Override
                public void clearContent() {

                }
            };
            this.headCosmeticInventory = new Container() {
                @Override
                public ItemStack getItem(int slot) {
                    return maid.getHeadCosmetic();
                }

                @Override
                public ItemStack removeItem(int slot, int amount) {
                    ItemStack current = maid.getHeadCosmetic();
                    ItemStack taken = current.split(amount);
                    maid.setHeadCosmetic(current);
                    return taken;
                }

                @Override
                public ItemStack removeItemNoUpdate(int slot) {
                    ItemStack result = maid.getHeadCosmetic();
                    maid.setHeadCosmetic(ItemStack.EMPTY);
                    return result;
                }

                @Override
                public void setItem(int slot, ItemStack stack) {
                    maid.setHeadCosmetic(stack);
                }

                @Override
                public boolean isEmpty() {
                    return maid.getHeadCosmetic().isEmpty();
                }

                @Override
                public int getContainerSize() {
                    return 1;
                }

                @Override
                public boolean stillValid(Player player) {
                    return true;
                }

                @Override
                public void setChanged() {
                }

                @Override
                public void clearContent() {
                    maid.setHeadCosmetic(ItemStack.EMPTY);
                }
            };
        }

        maidInventory.startOpen(playerInventory.player);

        layoutMaidInventorySlots();
        layoutPlayerInventorySlots(8, 126);
    }

    public LittleMaidEntity getGuiEntity() {
        return maid;
    }

    public int getUnpaidDays() {
        return unpaidDays;
    }

    public int getWorkItemSlotSize() {
        return workItemSlotSize;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.maid != null && this.maid.isAlive() && this.maid.distanceToSqr(player) < 8.0F * 8.0F;
    }

    // 18 + 2 + 4 + 1 = 25、25 + 4 * 9 = 61
    // 0~17メイドインベントリ、18~19メインサブ、20~23防具、24頭飾り、25~60プレイヤーインベントリ
    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot == null || !slot.hasItem()) {
            return newStack;
        }
        ItemStack originalStack = slot.getItem();
        newStack = originalStack.copy();
        if (invSlot < 18) {// メイド->プレイヤー
            if (!this.moveItemStackTo(originalStack, 25, 61, false)) {
                return ItemStack.EMPTY;
            }
        } else if (invSlot < 25) {// ハンド、防具、頭飾り->メイド
            if (!this.moveItemStackTo(originalStack, 0, 18, true)) {
                return ItemStack.EMPTY;
            }
        } else {// プレイヤー->防具/頭飾り、だめならメイドインベントリ
            if (!this.moveItemStackTo(originalStack, 20, 25, false)
                    && !this.moveItemStackTo(originalStack, 0, 18, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (originalStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return newStack;
    }

    private int addSlotRange(Container inventory, int index, int x, int y, int amount, int dx) {
        for (int i = 0; i < amount; i++) {
            addSlot(new Slot(inventory, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    private void addSlotBox(Container inventory, int index, int x, int y, int verAmount) {
        for (int j = 0; j < verAmount; j++) {
            index = addSlotRange(inventory, index, x, y, 9, 18);
            y += 18;
        }
    }

    private void layoutPlayerInventorySlots(int leftCol, int topRow) {
        // 24~50
        // Player inventory
        addSlotBox(playerInventory, 9, leftCol, topRow, 3);

        // 51~59
        // Hotbar
        topRow += 58;
        addSlotRange(playerInventory, 0, leftCol, topRow, 9, 18);
    }

    private void layoutMaidInventorySlots() {
        // index 0~17
        addSlotBox(maidInventory, 0, 8, 76, 2);

        // 18~19
        addSlot(new Slot(handInventory, 0, 116, 44));
        addSlot(new Slot(handInventory, 1, 152, 44) {
            @Override
            public Identifier getNoItemIcon() {
                return InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
            }
        });

        // 20~23
        addSlot(new Slot(armorInventory, EquipmentSlot.HEAD.getIndex(), 8, 8) {

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return LittleMaidEntity.isHeadArmorItem(stack);
            }

            @Override
            public Identifier getNoItemIcon() {
                return InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
            }
        });
        addSlot(new Slot(armorInventory, EquipmentSlot.CHEST.getIndex(), 8, 44) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return maid.getEquipmentSlotForItem(stack) == EquipmentSlot.CHEST;
            }

            @Override
            public Identifier getNoItemIcon() {
                return InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
            }
        });
        addSlot(new Slot(armorInventory, EquipmentSlot.LEGS.getIndex(), 80, 8) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return maid.getEquipmentSlotForItem(stack) == EquipmentSlot.LEGS;
            }

            @Override
            public Identifier getNoItemIcon() {
                return InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
            }
        });
        addSlot(new Slot(armorInventory, EquipmentSlot.FEET.getIndex(), 80, 44) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return maid.getEquipmentSlotForItem(stack) == EquipmentSlot.FEET;
            }

            @Override
            public Identifier getNoItemIcon() {
                return InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
            }
        });

        // 24 頭飾り（ヘルメットと分離。カボチャ・頭蓋骨など）
        addSlot(new Slot(headCosmeticInventory, 0, 8, 26) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return LittleMaidEntity.isHeadCosmeticItem(stack);
            }
        });
    }

}
