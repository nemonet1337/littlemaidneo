package work.nemonet.littlemaidneo.setup;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import work.nemonet.littlemaidneo.common.LMNLib;
import work.nemonet.littlemaidneo.entity.util.MaidJobEntry;

/**
 * NeoForge Data Map 定義。JSON は {@code data/littlemaidneo/data_maps/item/maid_job.json}。
 */
public final class LMDataMaps {
    private LMDataMaps() {
    }

    public static final DataMapType<Item, MaidJobEntry> MAID_JOB = DataMapType.builder(
            Identifier.fromNamespaceAndPath(LMNLib.MODID, "maid_job"),
            Registries.ITEM,
            MaidJobEntry.CODEC
    ).build();
}
