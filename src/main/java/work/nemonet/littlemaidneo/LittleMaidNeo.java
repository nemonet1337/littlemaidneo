package work.nemonet.littlemaidneo;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import work.nemonet.littlemaidneo.advancement.criterion.LMRBCriteria;
import work.nemonet.littlemaidneo.client.resource.loader.LMSoundLoader;
import work.nemonet.littlemaidneo.client.resource.manager.LMSoundManager;
import work.nemonet.littlemaidneo.config.LMMLConfig;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.MultiModelEntity;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.*;
import work.nemonet.littlemaidneo.network.NetworkHandler;
import work.nemonet.littlemaidneo.resource.classloader.MultiModelClassLoader;
import work.nemonet.littlemaidneo.resource.loader.LMConfigLoader;
import work.nemonet.littlemaidneo.resource.loader.LMFileLoader;
import work.nemonet.littlemaidneo.resource.loader.LMMultiModelLoader;
import work.nemonet.littlemaidneo.resource.loader.LMTextureLoader;
import work.nemonet.littlemaidneo.resource.manager.LMConfigManager;
import work.nemonet.littlemaidneo.resource.manager.LMModelManager;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.setup.ModSetup;
import work.nemonet.littlemaidneo.setup.ModRegistration;

import java.nio.file.Paths;

@Mod(LittleMaidNeo.MODID)
public class LittleMaidNeo {
    public static final String MODID = "littlemaidneo";
    public static final Logger LOGGER = LogManager.getLogger();

    public LittleMaidNeo(IEventBus modEventBus, ModContainer modContainer) {
        ModRegistration.ENTITIES.register(modEventBus);
        ModRegistration.BLOCKS.register(modEventBus);
        ModRegistration.ITEMS.register(modEventBus);
        ModRegistration.MENUS.register(modEventBus);
        ModRegistration.CREATIVE_TABS.register(modEventBus);
        ModRegistration.BLOCK_ENTITIES.register(modEventBus);
        ModRegistration.MEMORY_MODULES.register(modEventBus);
        ModRegistration.SENSORS.register(modEventBus);
        ModRegistration.ATTACHMENT_TYPES.register(modEventBus);

        modEventBus.addListener(work.nemonet.littlemaidneo.data.LMDataGenerator::gatherClientData);
        modEventBus.addListener(work.nemonet.littlemaidneo.data.LMDataGenerator::gatherServerData);

        modContainer.registerConfig(ModConfig.Type.COMMON, LMMLConfig.SPEC, "littlemaidneo-lmml-common.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, LMRBConfig.SPEC, "littlemaidneo-server.toml");

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onRegisterPayloadHandlers);
        modEventBus.addListener(this::onEntityAttributeCreation);
        modEventBus.addListener(this::onRegisterSpawnPlacements);
        modEventBus.addListener(this::onRegisterEvent);
        modEventBus.addListener(this::onModConfig);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModSetup.init();
            initFileLoader();
            initModelLoader();
            if (FMLEnvironment.getDist() == Dist.CLIENT) {
                addGhastMaidVoice();
                initTextureLoader();
                initSoundLoader();
            }
            LMFileLoader.INSTANCE.load();
        });
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        NetworkHandler.register(event);
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModRegistration.MULTI_MODEL_ENTITY.get(), MultiModelEntity.createAttributes().build());
        event.put(ModRegistration.DUMMY_MODEL_ENTITY.get(), MultiModelEntity.createAttributes().build());
        event.put(ModRegistration.LITTLE_MAID_ENTITY.get(), LittleMaidEntity.createLittleMaidAttributes().build());
    }

    private void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        ModSetup.onRegisterSpawnPlacements(event);
    }

    private void onRegisterEvent(RegisterEvent event) {
        event.register(Registries.TRIGGER_TYPE, helper -> LMRBCriteria.init());
    }

    private void onModConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() == LMRBConfig.SPEC && !(event instanceof ModConfigEvent.Unloading)) {
            LMRBConfig.bake();
        }
    }

    public static void initFileLoader() {
        LMFileLoader fileLoader = LMFileLoader.INSTANCE;
        fileLoader.addLoadFolderPath(Paths.get(FMLPaths.GAMEDIR.get().toString(), "LMMLResources"));
        fileLoader.addLoader(new LMMultiModelLoader(LMModelManager.INSTANCE,
                new MultiModelClassLoader(fileLoader.getFolderPaths())));
        fileLoader.addLoader(new LMConfigLoader(LMConfigManager.INSTANCE));
    }

    public static void initModelLoader() {
        LMModelManager modelManager = LMModelManager.INSTANCE;
        modelManager.addModel("Default", ModelLittleMaid_Orign.class);
        modelManager.addModel("SR2", ModelLittleMaid_SR2.class);
        modelManager.addModel("Aug", ModelLittleMaid_Aug.class);
        modelManager.addModel("Archetype", ModelLittleMaid_Archetype.class);
        modelManager.addModel("Steve", ModelMulti_Steve.class);
        modelManager.addModel("Stef", ModelMulti_Stef.class);
        modelManager.addModel("Classic64", ModelMulti_Classic64.class);
        modelManager.addModel("Slim64", ModelMulti_Slim64.class);
        modelManager.addModel("Beverly7", ModelLittleMaid_Beverly7.class);
        modelManager.addModel("Chloe2", ModelLittleMaid_Chloe2.class);
        modelManager.addModel("Elsa5", ModelLittleMaid_Elsa5.class);
        modelManager.addModel("AC", ModelLittleMaid_AC.class);
        modelManager.addModel("RX0", ModelLittleMaid_RX0.class);
        modelManager.setDefaultModel(modelManager.getModel("Default", IHasMultiModel.Layer.SKIN)
                .orElseThrow(RuntimeException::new));
    }

    public static void initTextureLoader() {
        LMFileLoader fileLoader = LMFileLoader.INSTANCE;
        LMTextureLoader textureProcessor = new LMTextureLoader(LMTextureManager.INSTANCE);
        textureProcessor.addPathConverter("assets/", "");
        textureProcessor.addPathConverter("mob/", "minecraft/textures/entity/");
        fileLoader.addLoader(textureProcessor);
    }

    public static void initSoundLoader() {
        LMFileLoader.INSTANCE.addLoader(new LMSoundLoader(LMSoundManager.INSTANCE));
    }

    public static void addGhastMaidVoice() {
        String packName = "DefaultGhast";
        var configMap = new ImmutableMap.Builder<String, String>();
        addVoice(LMSounds.HURT, SoundEvents.GHAST_HURT, configMap);
        addVoice(LMSounds.HURT_FIRE, SoundEvents.GHAST_HURT, configMap);
        addVoice(LMSounds.HURT_FALL, SoundEvents.GHAST_HURT, configMap);
        addVoice(LMSounds.DEATH, SoundEvents.GHAST_DEATH, configMap);
        addVoice(LMSounds.ATTACK, SoundEvents.GHAST_WARN, configMap);
        addVoice(LMSounds.ATTACK_BLOOD_SUCK, SoundEvents.GHAST_WARN, configMap);
        addVoice(LMSounds.SHOOT, SoundEvents.GHAST_WARN, configMap);
        addVoice(LMSounds.SHOOT_BURST, SoundEvents.GHAST_WARN, configMap);
        addVoice(LMSounds.LIVING_DAYTIME, SoundEvents.GHAST_AMBIENT, configMap);
        addVoice(LMSounds.LIVING_MORNING, SoundEvents.GHAST_AMBIENT, configMap);
        addVoice(LMSounds.LIVING_NIGHT, SoundEvents.GHAST_AMBIENT, configMap);
        addVoice(LMSounds.LIVING_WHINE, SoundEvents.GHAST_AMBIENT, configMap);
        addVoice(LMSounds.LIVING_RAIN, SoundEvents.GHAST_AMBIENT, configMap);
        addVoice(LMSounds.LIVING_SNOW, SoundEvents.GHAST_AMBIENT, configMap);
        addVoice(LMSounds.LIVING_COLD, SoundEvents.GHAST_AMBIENT, configMap);
        addVoice(LMSounds.LIVING_HOT, SoundEvents.GHAST_AMBIENT, configMap);
        LMConfigManager.INSTANCE.addConfig(packName, "", "littlemaidmob", configMap.build());
    }

    private static void addVoice(String soundName, SoundEvent soundEvent,
                                  ImmutableMap.Builder<String, String> configMap) {
        configMap.put(soundName, soundEvent.location().toString());
    }
}
