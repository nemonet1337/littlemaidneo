package work.nemonet.littlemaidneo.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import work.nemonet.littlemaidneo.LittleMaidNeo;

/**
 * メイドさんに関するタグを置いとくとこ
 */
public class LMTags {

    public static class Items {
        public static final TagKey<Item> MAIDS_EMPLOYABLE = register("maids_employable");
        public static final TagKey<Item> MAIDS_SALARY = register("maids_salary");

        public static final TagKey<Item> FENCER_MODE = register("fencer_mode");
        public static final TagKey<Item> ARCHER_MODE = register("archer_mode");
        public static final TagKey<Item> COOKING_MODE = register("cooking_mode");
        public static final TagKey<Item> PHARMCIST_MODE = register("pharmcist_mode");
        public static final TagKey<Item> PHARMCIST_INGREDIENTS = register("pharmcist_ingredients");
        public static final TagKey<Item> RIPPER_MODE = register("ripper_mode");
        public static final TagKey<Item> TORCHER_MODE = register("torcher_mode");
        public static final TagKey<Item> HEALER_MODE = register("healer_mode");

        private static TagKey<Item> register(String id) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, id));
        }
    }

    public static class Blocks {
        public static final TagKey<Block> MAID_ALTER_COMPONENT_BLOCKS = register();

        private static TagKey<Block> register() {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "maid_alter_component_blocks"));
        }
    }

    public static class Biomes {
        public static final TagKey<Biome> MAID_SPAWN_BIOME = register("maid_spawn_biome");
        public static final TagKey<Biome> MAID_SPAWN_EXCLUDE_BIOME = register("maid_spawn_exclude_biome");

        private static TagKey<Biome> register(String id) {
            return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, id));
        }
    }
}
