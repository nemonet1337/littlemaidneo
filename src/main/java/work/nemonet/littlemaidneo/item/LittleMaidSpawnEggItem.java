package work.nemonet.littlemaidneo.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import work.nemonet.littlemaidneo.setup.Registration;

public class LittleMaidSpawnEggItem extends SpawnEggItem {
    public LittleMaidSpawnEggItem() {
        super(Registration.LITTLE_MAID_ENTITY.get(), 0xFFFFFF, 0x804000, new Item.Properties());
    }
}
