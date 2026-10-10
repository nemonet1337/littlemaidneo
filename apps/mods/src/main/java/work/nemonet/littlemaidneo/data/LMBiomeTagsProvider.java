package work.nemonet.littlemaidneo.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.tags.BiomeTags;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.concurrent.CompletableFuture;

public class LMBiomeTagsProvider extends BiomeTagsProvider {
    public LMBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // メイドさんが自然スポーンするバイーム（村があるバイーム）。
        // 26.3 では村タグが minecraft:has_structure/village_* に移動している。
        tag(LMTags.Biomes.MAID_SPAWN_BIOME)
                .addTag(BiomeTags.HAS_VILLAGE_DESERT)
                .addTag(BiomeTags.HAS_VILLAGE_PLAINS)
                .addTag(BiomeTags.HAS_VILLAGE_SAVANNA)
                .addTag(BiomeTags.HAS_VILLAGE_SNOWY)
                .addTag(BiomeTags.HAS_VILLAGE_TAIGA);
        tag(LMTags.Biomes.MAID_SPAWN_EXCLUDE_BIOME);
    }
}
