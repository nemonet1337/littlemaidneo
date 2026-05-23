package net.sistr.littlemaidrebirth.setup;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.sistr.littlemaidrebirth.block.SalaryBoxBlock;
import net.sistr.littlemaidrebirth.block.SalaryBoxBlockEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidScreenHandler;
import net.sistr.littlemaidrebirth.entity.MaidSoulEntity;
import net.sistr.littlemaidrebirth.item.LittleMaidSpawnEggItem;

import static net.sistr.littlemaidrebirth.LMRBMod.MODID;

public class Registration {
        private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(MODID,
                        Registries.ENTITY_TYPE);
        private static final DeferredRegister<CreativeModeTab> ITEM_GROUPS = DeferredRegister.create(MODID,
                        Registries.CREATIVE_MODE_TAB);
        private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MODID, Registries.ITEM);
        private static final DeferredRegister<MenuType<?>> SCREEN_HANDLERS = DeferredRegister.create(MODID,
                        Registries.MENU);
        private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MODID, Registries.BLOCK);
        private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(MODID,
                        Registries.BLOCK_ENTITY_TYPE);

        public static void init() {
                ENTITIES.register();
                ITEM_GROUPS.register();
                BLOCKS.register();
                ITEMS.register();
                SCREEN_HANDLERS.register();
                BLOCK_ENTITIES.register();
        }

        // エンティティ
        public static final RegistrySupplier<EntityType<LittleMaidEntity>> LITTLE_MAID_MOB = ENTITIES.register(
                        "little_maid_mob",
                        () -> EntityType.Builder.<LittleMaidEntity>of(LittleMaidEntity::new, MobCategory.CREATURE)
                                        .sized(0.5F, 1.35F).build("little_maid_mob"));
        public static final RegistrySupplier<EntityType<MaidSoulEntity>> MAID_SOUL_ENTITY = ENTITIES.register(
                        "maid_soul", () -> EntityType.Builder.<MaidSoulEntity>of(MaidSoulEntity::new, MobCategory.MISC)
                                        .sized(0.5F, 0.5F).build("maid_soul"));

        // アイテムグループ
        public static final RegistrySupplier<CreativeModeTab> ITEM_GROUP = ITEM_GROUPS.register("common",
                        () -> CreativeTabRegistry.create(Component.translatable("itemGroup.littlemaidrebirth.common"),
                                        Items.CAKE::getDefaultInstance));

        // ブロック
        public static final RegistrySupplier<SalaryBoxBlock> SALARY_BOX_BLOCK = BLOCKS.register("salary_box",
                        () -> new SalaryBoxBlock(
                                        BlockBehaviour.Properties
                                                        .of()
                                                        .mapColor(MapColor.WOOD)
                                                        .instrument(NoteBlockInstrument.BASS)
                                                        .strength(2.5f)
                                                        .sound(SoundType.WOOD)
                                                        .ignitedByLava()));

        // アイテム
        public static final RegistrySupplier<Item> LITTLE_MAID_SPAWN_EGG_ITEM = ITEMS.register("little_maid_spawn_egg",
                        LittleMaidSpawnEggItem::new);

        // ブロックアイテム
        public static final RegistrySupplier<Item> SALARY_BOX_BLOCK_ITEM = ITEMS.register("salary_box",
                        () -> new BlockItem(SALARY_BOX_BLOCK.get(), new Item.Properties()
                                        .arch$tab(ITEM_GROUP)));

        // スクリーンハンドラ
        public static final RegistrySupplier<MenuType<LittleMaidScreenHandler>> LITTLE_MAID_SCREEN_HANDLER = SCREEN_HANDLERS
                        .register("little_maid", () -> MenuRegistry.ofExtended(LittleMaidScreenHandler::new));

        // ブロックエンティティ
        public static final RegistrySupplier<BlockEntityType<SalaryBoxBlockEntity>> SALARY_BOX_BLOCK_ENTITY = BLOCK_ENTITIES
                        .register("salary_box",
                                        () -> BlockEntityType.Builder
                                                        .of(SalaryBoxBlockEntity::new, SALARY_BOX_BLOCK.get())
                                                        .build(null));
}
