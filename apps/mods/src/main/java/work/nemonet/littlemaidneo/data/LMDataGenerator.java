package work.nemonet.littlemaidneo.data;

import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;

public class LMDataGenerator {
    public static void gatherClientData(GatherDataEvent.Client event) {
        event.createProvider(output -> new LMLanguageProvider(output, "en_us"));
        event.createProvider(output -> new LMLanguageProvider(output, "ja_jp"));
        event.createProvider(LMModelProvider::new);
    }

    public static void gatherServerData(GatherDataEvent.Server event) {
        event.createBlockAndItemTags(
                LMBlockTagsProvider::new,
                LMItemTagsProvider::new
        );

        event.createProvider(LMBiomeTagsProvider::new);
        event.createProvider(LMEntityTypeTagsProvider::new);

        event.createProvider((output, lookup) -> new LMJobDataMapProvider(output, lookup));
    }

    public static void gatherRegistries(GatherDataRegistryEntriesEvent event) {
        event.recipe((recipeCtx, advancementCtx) -> new LMRecipeProvider(recipeCtx, advancementCtx));
        event.lootTable(
                new LootTableProvider.SubProviderEntry(LMLootTableProvider.LMBlockLoot::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(LMLootTableProvider.LMEntityLoot::new, LootContextParamSets.ENTITY));
        event.advancement(LMAdvancementProvider.create());
    }
}
