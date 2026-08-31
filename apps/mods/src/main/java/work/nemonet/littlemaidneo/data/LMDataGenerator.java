package work.nemonet.littlemaidneo.data;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.List;
import java.util.Set;

public class LMDataGenerator {
    public static void gatherClientData(GatherDataEvent.Client event) {
        // Language Providers
        event.createProvider(output -> new LMLanguageProvider(output, "en_us"));
        event.createProvider(output -> new LMLanguageProvider(output, "ja_jp"));
        event.createProvider(LMModelProvider::new);
    }

    public static void gatherServerData(GatherDataEvent.Server event) {
        // blockTags & itemTags
        event.createBlockAndItemTags(
                LMBlockTagsProvider::new,
                LMItemTagsProvider::new
        );

        // BiomeTags & EntityTypeTags
        event.createProvider(LMBiomeTagsProvider::new);
        event.createProvider(LMEntityTypeTagsProvider::new);

        // Recipes & LootTables
        event.createProvider(LMRecipeProvider.Runner::new);
        event.createProvider(LMLootTableProvider::create);

        // Advancements
        event.createProvider(LMAdvancementProvider::create);
        event.createProvider(LMJobDataMapProvider::new);

        // Datapack built-in entries for Biome Modifiers
        RegistrySetBuilder registrySetBuilder = new RegistrySetBuilder()
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, context -> context.register(
                        ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "maid_spawn")),
                        new BiomeModifiers.AddSpawnsBiomeModifier(
                                 context.lookup(Registries.BIOME).getOrThrow(LMTags.Biomes.MAID_SPAWN_BIOME),
                                WeightedList.of(List.of(new Weighted<>(new MobSpawnSettings.SpawnerData(
                                         ModRegistration.LITTLE_MAID_ENTITY.get(),
                                         1, 3
                                ), 5)))
                        )
                ));
        event.createProvider((output, lookup) -> new DatapackBuiltinEntriesProvider(output, lookup, registrySetBuilder, Set.of(LittleMaidNeo.MODID)));
    }
}
