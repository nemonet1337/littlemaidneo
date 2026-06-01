package work.nemonet.littlemaidneo.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import work.nemonet.littlemaidneo.setup.ModRegistration;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class LMLootTableProvider {
    public static LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(LMBlockLoot::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(LMEntityLoot::new, LootContextParamSets.ENTITY)
        ), registries);
    }

    public static class LMBlockLoot extends BlockLootSubProvider {
        protected LMBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            add(ModRegistration.SALARY_BOX_BLOCK.get(), this::createNameableBlockEntityTable);
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(ModRegistration.SALARY_BOX_BLOCK.get());
        }
    }

    public static class LMEntityLoot extends EntityLootSubProvider {
        protected LMEntityLoot(HolderLookup.Provider registries) {
            super(FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        public void generate() {
            add(ModRegistration.LITTLE_MAID_ENTITY.get(), LootTable.lootTable());
        }

        @Override
        protected Stream<net.minecraft.world.entity.EntityType<?>> getKnownEntityTypes() {
            return Stream.of(ModRegistration.LITTLE_MAID_ENTITY.get());
        }
    }
}
