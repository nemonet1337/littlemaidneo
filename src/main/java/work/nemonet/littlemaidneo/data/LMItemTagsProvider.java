package work.nemonet.littlemaidneo.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import net.minecraft.world.item.Items;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.concurrent.CompletableFuture;

public class LMItemTagsProvider extends ItemTagsProvider {
    public LMItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagsProvider.TagLookup<net.minecraft.world.level.block.Block>> blockTags) {
        super(output, lookupProvider, LittleMaidNeo.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(LMTags.Items.MAIDS_EMPLOYABLE).add(Items.CAKE);
        tag(LMTags.Items.MAIDS_SALARY).add(Items.SUGAR);

        tag(LMTags.Items.FENCER_MODE).add(Items.TRIDENT);
        tag(LMTags.Items.ARCHER_MODE);
        tag(LMTags.Items.COOKING_MODE).add(Items.BOWL);
        tag(LMTags.Items.PHARMCIST_MODE).add(Items.GLASS_BOTTLE);
        tag(LMTags.Items.RIPPER_MODE);
        tag(LMTags.Items.TORCHER_MODE);
        tag(LMTags.Items.HEALER_MODE);
    }
}
