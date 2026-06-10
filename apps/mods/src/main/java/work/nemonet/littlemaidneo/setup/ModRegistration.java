package work.nemonet.littlemaidneo.setup;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import work.nemonet.littlemaidneo.entity.util.MaidManagerImpl;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManagerImpl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.block.SalaryBoxBlock;
import work.nemonet.littlemaidneo.block.SalaryBoxBlockEntity;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.LittleMaidScreenHandler;
import work.nemonet.littlemaidneo.entity.MaidSoulEntity;
import work.nemonet.littlemaidneo.entity.MultiModelEntity;
import work.nemonet.littlemaidneo.item.LittleMaidSpawnEggItem;
import work.nemonet.littlemaidneo.entity.DummyModelEntity;

import net.minecraft.core.component.DataComponentType;
import work.nemonet.littlemaidneo.entity.soul.MaidSoulData;

public class ModRegistration {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, LittleMaidNeo.MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, LittleMaidNeo.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MaidSoulData>> MAID_SOUL =
            DATA_COMPONENT_TYPES.register("maid_soul", () -> DataComponentType.<MaidSoulData>builder()
                    .persistent(MaidSoulData.CODEC)
                    .networkSynchronized(MaidSoulData.STREAM_CODEC)
                    .build());
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, LittleMaidNeo.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, LittleMaidNeo.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, LittleMaidNeo.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LittleMaidNeo.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LittleMaidNeo.MODID);
    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_MODULES =
            DeferredRegister.create(Registries.MEMORY_MODULE_TYPE, LittleMaidNeo.MODID);
    public static final DeferredRegister<SensorType<?>> SENSORS =
            DeferredRegister.create(Registries.SENSOR_TYPE, LittleMaidNeo.MODID);

    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<net.minecraft.util.Unit>> IS_WAITING =
            MEMORY_MODULES.register("is_waiting", () -> new MemoryModuleType<>(java.util.Optional.empty()));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<net.minecraft.world.entity.player.Player>> OWNER =
            MEMORY_MODULES.register("owner", () -> new MemoryModuleType<>(java.util.Optional.empty()));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<String>> ACTIVE_JOB_NAME =
            MEMORY_MODULES.register("active_job_name", () -> new MemoryModuleType<>(java.util.Optional.of(com.mojang.serialization.Codec.STRING)));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<String>> ACTIVE_BATTLE_MODE =
            MEMORY_MODULES.register("active_battle_mode", () -> new MemoryModuleType<>(java.util.Optional.of(com.mojang.serialization.Codec.STRING)));

    public static final DeferredHolder<SensorType<?>, SensorType<work.nemonet.littlemaidneo.entity.ai.LittleMaidSensor>> LITTLE_MAID_SENSOR =
            SENSORS.register("little_maid_sensor", () -> new SensorType<>(work.nemonet.littlemaidneo.entity.ai.LittleMaidSensor::new));

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, LittleMaidNeo.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<MaidManagerImpl>> MAID_MANAGER_ATTACHMENT =
            ATTACHMENT_TYPES.register("maid_manager", () -> AttachmentType.builder(MaidManagerImpl::new)
                    .serialize(new IAttachmentSerializer<MaidManagerImpl>() {
                        @Override
                        public MaidManagerImpl read(IAttachmentHolder holder, ValueInput input) {
                            MaidManagerImpl manager = new MaidManagerImpl();
                            manager.readMaidManager(input);
                            return manager;
                        }
                        @Override
                        public boolean write(MaidManagerImpl manager, ValueOutput output) {
                            manager.writeMaidManager(output);
                            return true;
                        }
                    })
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TargetTagManagerImpl>> TARGET_TAG_ATTACHMENT =
            ATTACHMENT_TYPES.register("target_tag", () -> AttachmentType.builder(() -> new TargetTagManagerImpl(null))
                    .serialize(new IAttachmentSerializer<TargetTagManagerImpl>() {
                        @Override
                        public TargetTagManagerImpl read(IAttachmentHolder holder, ValueInput input) {
                            TargetTagManagerImpl manager = new TargetTagManagerImpl(null);
                            manager.readTargetTags(input);
                            return manager;
                        }
                        @Override
                        public boolean write(TargetTagManagerImpl manager, ValueOutput output) {
                            manager.writeTargetTags(output);
                            return true;
                        }
                    })
                    .copyOnDeath()
                    .build());

    // Static instances for safe cross-registry reference during registration
    public static SalaryBoxBlock salaryBoxBlockInstance;
    public static net.minecraft.world.entity.EntityType<LittleMaidEntity> littleMaidEntityTypeInstance;

    // LML entities
    public static final DeferredHolder<EntityType<?>, EntityType<MultiModelEntity>> MULTI_MODEL_ENTITY =
            ENTITIES.register("multi_model_entity", () ->
                    EntityType.Builder.<MultiModelEntity>of(MultiModelEntity::new, MobCategory.MISC)
                            .sized(0.5F, 1.35F)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "multi_model_entity"))));

    public static final DeferredHolder<EntityType<?>, EntityType<DummyModelEntity>> DUMMY_MODEL_ENTITY =
            ENTITIES.register("dummy_model_entity", () ->
                    EntityType.Builder.<DummyModelEntity>of(DummyModelEntity::new, MobCategory.MISC)
                            .sized(0.5F, 1.35F)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "dummy_model_entity"))));

    // ReBirth entities
    public static final DeferredHolder<EntityType<?>, EntityType<LittleMaidEntity>> LITTLE_MAID_ENTITY =
            ENTITIES.register("little_maid_mob", () -> {
                littleMaidEntityTypeInstance = EntityType.Builder.<LittleMaidEntity>of(LittleMaidEntity::new, MobCategory.CREATURE)
                        .sized(0.5F, 1.35F)
                        .build(ResourceKey.create(Registries.ENTITY_TYPE,
                                Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "little_maid_mob")));
                return littleMaidEntityTypeInstance;
            });

    public static final DeferredHolder<EntityType<?>, EntityType<MaidSoulEntity>> MAID_SOUL_ENTITY =
            ENTITIES.register("maid_soul", () ->
                    EntityType.Builder.<MaidSoulEntity>of(MaidSoulEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "maid_soul"))));

    // Blocks
    public static final DeferredHolder<Block, SalaryBoxBlock> SALARY_BOX_BLOCK =
            BLOCKS.register("salary_box", () -> {
                salaryBoxBlockInstance = new SalaryBoxBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.WOOD)
                                .instrument(NoteBlockInstrument.BASS)
                                .strength(2.5f)
                                .sound(SoundType.WOOD)
                                .ignitedByLava()
                                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "salary_box"))));
                return salaryBoxBlockInstance;
            });

    // Items
    public static final DeferredHolder<Item, LittleMaidSpawnEggItem> LITTLE_MAID_SPAWN_EGG_ITEM =
            ITEMS.register("little_maid_spawn_egg", LittleMaidSpawnEggItem::new);

    public static final DeferredHolder<Item, Item> SALARY_BOX_BLOCK_ITEM =
            ITEMS.register("salary_box", () ->
                    new BlockItem(salaryBoxBlockInstance, new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "salary_box")))));

    // Creative tab (declared after items it references to avoid illegal forward reference)
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ITEM_GROUP =
            CREATIVE_TABS.register("common", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.littlemaidneo"))
                            .icon(Items.CAKE::getDefaultInstance)
                            .displayItems((params, output) -> {
                                output.accept(LITTLE_MAID_SPAWN_EGG_ITEM.get());
                                output.accept(SALARY_BOX_BLOCK_ITEM.get());
                            })
                            .build());

    // Menus
    public static final DeferredHolder<MenuType<?>, MenuType<LittleMaidScreenHandler>> LITTLE_MAID_SCREEN_HANDLER =
            MENUS.register("little_maid", () ->
                    IMenuTypeExtension.create(LittleMaidScreenHandler::new));

    // Block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SalaryBoxBlockEntity>> SALARY_BOX_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("salary_box", () ->
                    new BlockEntityType<>(SalaryBoxBlockEntity::new, salaryBoxBlockInstance));
}
