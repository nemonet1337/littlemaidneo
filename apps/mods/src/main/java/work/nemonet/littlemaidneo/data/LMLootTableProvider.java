package work.nemonet.littlemaidneo.data;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import work.nemonet.littlemaidneo.setup.ModRegistration;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class LMLootTableProvider {
    public static class LMBlockLoot extends BlockLootSubProvider {
        protected LMBlockLoot(LootTableSubProvider.Context output) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), output);
        }

        @Override
        protected void generate() {
            add(ModRegistration.SALARY_BOX_BLOCK.get(), this::createNameableBlockEntityTable);
        }

        @Override
        protected Iterable<net.minecraft.world.level.block.Block> getKnownBlocks() {
            return List.of(ModRegistration.SALARY_BOX_BLOCK.get());
        }
    }

    public static class LMEntityLoot extends EntityLootSubProvider {
        protected LMEntityLoot(LootTableSubProvider.Context output) {
            super(FeatureFlags.REGISTRY.allFlags(), output);
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
