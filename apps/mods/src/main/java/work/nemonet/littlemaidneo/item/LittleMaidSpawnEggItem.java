package work.nemonet.littlemaidneo.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.setup.ModRegistration;

public class LittleMaidSpawnEggItem extends SpawnEggItem {
    public LittleMaidSpawnEggItem() {
        super(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "little_maid_spawn_egg")))
                .component(DataComponents.ENTITY_DATA,
                        TypedEntityData.of(ModRegistration.littleMaidEntityTypeInstance != null
                                ? ModRegistration.littleMaidEntityTypeInstance
                                : (net.minecraft.world.entity.EntityType<?>) ModRegistration.LITTLE_MAID_ENTITY.get(), new CompoundTag())));
    }
}
