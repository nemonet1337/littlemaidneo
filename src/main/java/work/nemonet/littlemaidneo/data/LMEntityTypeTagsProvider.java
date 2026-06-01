package work.nemonet.littlemaidneo.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import work.nemonet.littlemaidneo.tags.LMEntityTags;

import java.util.concurrent.CompletableFuture;

public class LMEntityTypeTagsProvider extends EntityTypeTagsProvider {
    public LMEntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(LMEntityTags.ATTACK_PROHIBITED);
        tag(LMEntityTags.APPROACH_PROHIBITED);
        tag(LMEntityTags.PREEMPTIVE_ATTACK_PROHIBITED);
        tag(LMEntityTags.RANGED_WEAPON_PROHIBITED);
        tag(LMEntityTags.MELEE_WEAPON_PROHIBITED);
    }
}
