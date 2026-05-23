package net.sistr.littlemaidrebirth.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.sistr.littlemaidrebirth.LMRBMod;

/**
 * メイドさんに関するタグを置いとくとこ
 */
// TODO 判定をタグとコンフィグで行えるように仕様を調整
public class LMTags {

    public static class Items {
        public static final TagKey<Item> MAIDS_EMPLOYABLE = register("maids_employable");
        public static final TagKey<Item> MAIDS_SALARY = register("maids_salary");

        public static final TagKey<Item> FENCER_MODE = register("fencer_mode");
        public static final TagKey<Item> ARCHER_MODE = register("archer_mode");
        public static final TagKey<Item> COOKING_MODE = register("cooking_mode");
        public static final TagKey<Item> RIPPER_MODE = register("ripper_mode");
        public static final TagKey<Item> TORCHER_MODE = register("torcher_mode");
        public static final TagKey<Item> HEALER_MODE = register("healer_mode");

        private static TagKey<Item> register(String id) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID, id));
        }
    }

    public static class Blocks {
        public static final TagKey<Block> MAID_ALTER_COMPONENT_BLOCKS = register("maid_alter_component_blocks");

        private static TagKey<Block> register(String id) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID, id));
        }
    }

    public static class Biomes {
        public static final TagKey<Biome> MAID_SPAWN_BIOME = register("maid_spawn_biome");
        public static final TagKey<Biome> MAID_SPAWN_EXCLUDE_BIOME = register("maid_spawn_exclude_biome");

        private static TagKey<Biome> register(String id) {
            return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID, id));
        }
    }
}
