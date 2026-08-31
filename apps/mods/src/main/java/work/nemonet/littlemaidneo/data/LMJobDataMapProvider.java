package work.nemonet.littlemaidneo.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import work.nemonet.littlemaidneo.entity.util.MaidJobEntry;
import work.nemonet.littlemaidneo.entity.util.MaidJobManager;
import work.nemonet.littlemaidneo.setup.LMDataMaps;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.concurrent.CompletableFuture;

public class LMJobDataMapProvider extends DataMapProvider {
    public LMJobDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        builder(LMDataMaps.MAID_JOB)
                .add(LMTags.Items.FENCER_MODE, new MaidJobEntry(MaidJobManager.JOB_COMBAT, 400), false)
                .add(LMTags.Items.ARCHER_MODE, new MaidJobEntry(MaidJobManager.JOB_COMBAT, 400), false)
                .add(LMTags.Items.COOKING_MODE, new MaidJobEntry(MaidJobManager.JOB_COOKING, 400), false)
                .add(LMTags.Items.RIPPER_MODE, new MaidJobEntry(MaidJobManager.JOB_RIPPER, 400), false)
                .add(LMTags.Items.TORCHER_MODE, new MaidJobEntry(MaidJobManager.JOB_TORCHER, 400), false)
                .add(LMTags.Items.HEALER_MODE, new MaidJobEntry(MaidJobManager.JOB_HEALER, 400), false)
                .add(LMTags.Items.PHARMCIST_MODE, new MaidJobEntry(MaidJobManager.JOB_PHARMCIST, 400), false)
                .add(LMTags.Items.PHARMCIST_INGREDIENTS, new MaidJobEntry(MaidJobManager.JOB_PHARMCIST, 100), false);
    }
}
