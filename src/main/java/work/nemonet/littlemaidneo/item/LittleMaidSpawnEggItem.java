package work.nemonet.littlemaidneo.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import work.nemonet.littlemaidneo.setup.ModRegistration;

public class LittleMaidSpawnEggItem extends SpawnEggItem {
    public LittleMaidSpawnEggItem() {
        super(new Item.Properties()
                .component(DataComponents.ENTITY_DATA,
                        TypedEntityData.of((net.minecraft.world.entity.EntityType<?>) ModRegistration.LITTLE_MAID_ENTITY.get(), new CompoundTag())));
    }
}
