package net.sistr.littlemaidrebirth.entity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class LMScreenHandlerFactory implements ExtendedMenuProvider {
    private final LittleMaidEntity maid;

    public LMScreenHandlerFactory(LittleMaidEntity maid) {
        this.maid = maid;
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeVarInt(maid.getId());
        buf.writeByte(maid.getUnpaidDays());
        buf.writeByte(maid.getWorkItemSlotSize());
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new LittleMaidScreenHandler(syncId, inv, maid.getId(), maid.getUnpaidDays(), maid.getWorkItemSlotSize());
    }

    @Override
    public Component getDisplayName() {
        return maid.getDisplayName();
    }

}
