package work.nemonet.littlemaidneo.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
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
        // 26.2: TagAppender.add は ResourceKey を取る
        tag(LMTags.Items.MAIDS_EMPLOYABLE).add(Items.CAKE.builtInRegistryHolder().key());
        tag(LMTags.Items.MAIDS_SALARY).add(Items.SUGAR.builtInRegistryHolder().key());

        tag(LMTags.Items.FENCER_MODE).add(Items.TRIDENT.builtInRegistryHolder().key());
        tag(LMTags.Items.ARCHER_MODE);
        tag(LMTags.Items.COOKING_MODE).add(Items.BOWL.builtInRegistryHolder().key());
        tag(LMTags.Items.PHARMCIST_MODE).add(Items.GLASS_BOTTLE.builtInRegistryHolder().key());
        tag(LMTags.Items.PHARMCIST_INGREDIENTS)
                .add(Items.NETHER_WART.builtInRegistryHolder().key())
                .add(Items.BLAZE_POWDER.builtInRegistryHolder().key())
                .add(Items.GLOWSTONE_DUST.builtInRegistryHolder().key())
                .add(Items.REDSTONE.builtInRegistryHolder().key())
                .add(Items.FERMENTED_SPIDER_EYE.builtInRegistryHolder().key())
                .add(Items.GUNPOWDER.builtInRegistryHolder().key())
                .add(Items.DRAGON_BREATH.builtInRegistryHolder().key())
                .add(Items.SPIDER_EYE.builtInRegistryHolder().key())
                .add(Items.MAGMA_CREAM.builtInRegistryHolder().key())
                .add(Items.GHAST_TEAR.builtInRegistryHolder().key())
                .add(Items.RABBIT_FOOT.builtInRegistryHolder().key())
                .add(Items.GLISTERING_MELON_SLICE.builtInRegistryHolder().key())
                .add(Items.GOLDEN_CARROT.builtInRegistryHolder().key())
                .add(Items.PUFFERFISH.builtInRegistryHolder().key())
                .add(Items.PHANTOM_MEMBRANE.builtInRegistryHolder().key())
                .add(Items.TURTLE_HELMET.builtInRegistryHolder().key());
        tag(LMTags.Items.RIPPER_MODE);
        tag(LMTags.Items.TORCHER_MODE);
        tag(LMTags.Items.HEALER_MODE);
    }
}
