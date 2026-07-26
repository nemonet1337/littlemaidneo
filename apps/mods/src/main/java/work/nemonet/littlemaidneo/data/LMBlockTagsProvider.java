package work.nemonet.littlemaidneo.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.concurrent.CompletableFuture;

public class LMBlockTagsProvider extends BlockTagsProvider {
    public LMBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, LittleMaidNeo.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // 26.2: TagAppender.add は ResourceKey を取る
        tag(LMTags.Blocks.MAID_ALTER_COMPONENT_BLOCKS)
                .add(Blocks.SUGAR_CANE.builtInRegistryHolder().key());
    }
}
