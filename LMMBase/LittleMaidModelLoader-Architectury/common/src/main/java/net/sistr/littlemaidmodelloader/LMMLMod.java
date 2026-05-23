package net.sistr.littlemaidmodelloader;

import com.google.common.collect.ImmutableMap;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.sistr.littlemaidmodelloader.client.resource.loader.LMSoundLoader;
import net.sistr.littlemaidmodelloader.resource.loader.LMTextureLoader;
import net.sistr.littlemaidmodelloader.client.resource.manager.LMSoundManager;
import net.sistr.littlemaidmodelloader.config.LMMLConfig;
import net.sistr.littlemaidmodelloader.entity.MultiModelEntity;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.maidmodel.*;
import net.sistr.littlemaidmodelloader.resource.classloader.MultiModelClassLoader;
import net.sistr.littlemaidmodelloader.resource.loader.LMConfigLoader;
import net.sistr.littlemaidmodelloader.resource.loader.LMFileLoader;
import net.sistr.littlemaidmodelloader.resource.loader.LMMultiModelLoader;
import net.sistr.littlemaidmodelloader.resource.manager.LMConfigManager;
import net.sistr.littlemaidmodelloader.resource.manager.LMModelManager;
import net.sistr.littlemaidmodelloader.resource.manager.LMTextureManager;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidmodelloader.resource.util.ResourceHelper;
import net.sistr.littlemaidmodelloader.setup.Registration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.Collection;

// 1.21.1移植: YarnマッピングからMojangマッピングへ変更
// - net.neoforged.api.distmarker.Dist → net.fabricmc.api.EnvType (Architectury共通モジュールのため)
// - @OnlyIn → @Environment
// - ResourceLocation → ResourceLocation
// - SoundEvent.getId() → SoundEvent.getLocation()
// - listResourcesの戻り値型変更
public class LMMLMod {
    public static final String MODID = "littlemaidmodelloader";
    public static final Logger LOGGER = LogManager.getLogger();
    private static ConfigHolder<LMMLConfig> CONFIG_HOLDER;

    public static void init() {
        AutoConfig.register(LMMLConfig.class, GsonConfigSerializer::new);
        CONFIG_HOLDER = AutoConfig.getConfigHolder(LMMLConfig.class);

        initFileLoader();
        initModelLoader();
        //TODO クライアントセットアップ
        // 1.21.1: NeoForgeのDistの代わりにFabricのEnvTypeを使用（Architectury共通モジュールのため）
        if (Platform.getEnv() == EnvType.CLIENT) {
            addGhastMaidVoice();
            initTextureLoader();
            initSoundLoader();
            ClientLifecycleEvent.CLIENT_STARTED.register(cs -> {
                //このパスにあるテクスチャすべてを受け取る(リソパ及びModリソースからも抜ける)
                // 1.21.1: listResourcesの戻り値がMap<ResourceLocation, Resource>に変更
                Collection<ResourceLocation> resourceLocations = cs.getResourceManager()
                        .listResources("textures/entity/littlemaid", s -> true)
                        .keySet();
                //テクスチャを読み込む
                resourceLocations.forEach(resourcePath -> {
                    String path = resourcePath.getPath();
                    ResourceHelper.getTexturePackName(path, false).ifPresent(textureName -> {
                        String modelName = ResourceHelper.getModelName(textureName);
                        int index = ResourceHelper.getIndex(path);
                        if (index != -1) {
                            LMTextureManager.INSTANCE
                                    .addTexture(ResourceHelper.getFileName(path, false), textureName, modelName, index, resourcePath);
                        }
                    });
                });
            });
        }
        Registration.init();
        registerAttribute();

        //ForgeだとModSetupじゃ遅いためここに
        LMFileLoader.INSTANCE.load();
    }

    public static void initFileLoader() {
        LMFileLoader fileLoader = LMFileLoader.INSTANCE;
        fileLoader.addLoadFolderPath(Paths.get(Platform.getGameFolder().toString(), "LMMLResources"));
        fileLoader.addLoader(new LMMultiModelLoader(LMModelManager.INSTANCE, new MultiModelClassLoader(fileLoader.getFolderPaths())));
        fileLoader.addLoader(new LMConfigLoader(LMConfigManager.INSTANCE));
    }

    public static void initModelLoader() {
        //モデルを読み込む
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
        modelManager.setDefaultModel(modelManager.getModel("Default", IHasMultiModel.Layer.SKIN)
                .orElseThrow(RuntimeException::new));
    }

    @Environment(EnvType.CLIENT)
    public static void initTextureLoader() {
        LMFileLoader fileLoader = LMFileLoader.INSTANCE;
        LMTextureLoader textureProcessor = new LMTextureLoader(LMTextureManager.INSTANCE);
        textureProcessor.addPathConverter("assets/", "");
        textureProcessor.addPathConverter("mob/", "minecraft/textures/entity/");
        fileLoader.addLoader(textureProcessor);
    }

    @Environment(EnvType.CLIENT)
    public static void initSoundLoader() {
        LMFileLoader.INSTANCE.addLoader(new LMSoundLoader(LMSoundManager.INSTANCE));
    }

    public static void registerAttribute() {
        // 1.21.1: EntityAttributeRegistry.registerのシグネチャ確認
        EntityAttributeRegistry.register(Registration.MULTI_MODEL_ENTITY, MultiModelEntity::createAttributes);
        EntityAttributeRegistry.register(Registration.DUMMY_MODEL_ENTITY, MultiModelEntity::createAttributes);
    }

    public static LMMLConfig getConfig() {
        return CONFIG_HOLDER.getConfig();
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

    private static void addVoice(String soundName, SoundEvent soundId, ImmutableMap.Builder<String, String> configMap) {
        // 1.21.1: SoundEvent.getId() → SoundEvent.getLocation()
        configMap.put(soundName, soundId.getLocation().toString());
    }

}
