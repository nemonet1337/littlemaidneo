package work.nemonet.littlemaidneo.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.concurrent.CompletableFuture;

public class LMBiomeTagsProvider extends BiomeTagsProvider {
    public LMBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(LMTags.Biomes.MAID_SPAWN_BIOME);
        tag(LMTags.Biomes.MAID_SPAWN_EXCLUDE_BIOME);
    }
}
