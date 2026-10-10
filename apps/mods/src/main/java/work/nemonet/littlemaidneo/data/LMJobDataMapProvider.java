package work.nemonet.littlemaidneo.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import work.nemonet.littlemaidneo.entity.util.MaidJob;
import work.nemonet.littlemaidneo.entity.util.MaidJobEntry;
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
                .add(LMTags.Items.FENCER_MODE, new MaidJobEntry(MaidJob.COMBAT, 400), false)
                .add(LMTags.Items.ARCHER_MODE, new MaidJobEntry(MaidJob.COMBAT, 400), false)
                .add(LMTags.Items.COOKING_MODE, new MaidJobEntry(MaidJob.COOKING, 400), false)
                .add(LMTags.Items.RIPPER_MODE, new MaidJobEntry(MaidJob.RIPPER, 400), false)
                .add(LMTags.Items.TORCHER_MODE, new MaidJobEntry(MaidJob.TORCHER, 400), false)
                .add(LMTags.Items.HEALER_MODE, new MaidJobEntry(MaidJob.HEALER, 400), false)
                .add(LMTags.Items.PHARMACIST_MODE, new MaidJobEntry(MaidJob.PHARMACIST, 400), false)
                .add(LMTags.Items.PHARMACIST_INGREDIENTS, new MaidJobEntry(MaidJob.PHARMACIST, 100), false);
    }
}
