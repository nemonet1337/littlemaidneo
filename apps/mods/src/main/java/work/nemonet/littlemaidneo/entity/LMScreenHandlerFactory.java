package work.nemonet.littlemaidneo.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class LMScreenHandlerFactory implements MenuProvider {
    private final LittleMaidEntity maid;

    public LMScreenHandlerFactory(LittleMaidEntity maid) {
        this.maid = maid;
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
