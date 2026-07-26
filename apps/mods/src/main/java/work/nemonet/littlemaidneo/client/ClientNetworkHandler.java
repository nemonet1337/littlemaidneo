package work.nemonet.littlemaidneo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import work.nemonet.littlemaidneo.client.screen.MaidManagerScreen;
import work.nemonet.littlemaidneo.client.screen.TargetTagScreen;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel.Layer;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel.Part;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayable;
import work.nemonet.littlemaidneo.entity.targeting.TargetIdentifier;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManager;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManagerImpl;
import work.nemonet.littlemaidneo.entity.targeting.TargetingSystem;
import work.nemonet.littlemaidneo.entity.util.MaidManager;
import work.nemonet.littlemaidneo.entity.util.MaidManagerImpl;
import work.nemonet.littlemaidneo.network.LMSoundPayload;
import work.nemonet.littlemaidneo.network.OpenMaidManagerScreenS2CPayload;
import work.nemonet.littlemaidneo.network.OpenTargetTagScreenS2CPayload;
import work.nemonet.littlemaidneo.network.SyncMultiModelPayload;
import work.nemonet.littlemaidneo.network.SyncSoundConfigPayload;
import work.nemonet.littlemaidneo.network.SyncSoundPackPayload;
import work.nemonet.littlemaidneo.resource.holder.ConfigHolder;
import work.nemonet.littlemaidneo.resource.manager.LMConfigManager;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;
import work.nemonet.littlemaidneo.resource.util.TextureColors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
public class ClientNetworkHandler {

    public static void handleSyncMultiModelClient(SyncMultiModelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> applyMultiModelClient(
                payload.entityId(), payload.isContract(), payload.color(),
                payload.textureName(), payload.getArmorSets()));
    }

    private static void applyMultiModelClient(int entityId, boolean isContract, TextureColors color,
                                              String textureName, ArmorSets<String> armorTextureName) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity entity = level.getEntity(entityId);
        if (!(entity instanceof IHasMultiModel multiModel)) return;
        multiModel.setContractMM(isContract);
        multiModel.setColorMM(color);
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        textureManager.getTexture(textureName)
                .filter(th -> multiModel.isAllowChangeTexture(entity, th, Layer.SKIN, Part.HEAD))
                .ifPresent(th -> multiModel.setTextureHolder(th, Layer.SKIN, Part.HEAD));
        for (Part part : Part.values()) {
            String armorName = armorTextureName.getArmor(part)
                    .orElseThrow(() -> new IllegalStateException("テクスチャが存在しません。"));
            textureManager.getTexture(armorName)
                    .filter(th -> multiModel.isAllowChangeTexture(entity, th, Layer.INNER, part))
                    .ifPresent(th -> multiModel.setTextureHolder(th, Layer.INNER, part));
        }
    }

    public static void handleSyncSoundPackClient(SyncSoundPackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            Entity entity = level.getEntity(payload.entityId());
            if (!(entity instanceof SoundPlayable soundPlayable)) return;
            ConfigHolder configHolder = LMConfigManager.INSTANCE.getConfig(payload.soundPackName())
                    .orElse(LMConfigManager.EMPTY_CONFIG);
            soundPlayable.setConfigHolder(configHolder);
        });
    }

    public static void handleLMSoundClient(LMSoundPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            Entity entity = player.level().getEntity(payload.entityId());
            if (entity instanceof SoundPlayable soundPlayable) {
                soundPlayable.play(payload.soundName());
            }
        });
    }

    public static void handleSyncSoundConfigClient(SyncSoundConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            Level world = player.level();
            Entity entity = world.getEntity(payload.entityId());
            if (entity instanceof SoundPlayable soundPlayable) {
                LMConfigManager.INSTANCE.getConfig(payload.configName())
                        .ifPresent(soundPlayable::setConfigHolder);
            }
        });
    }

    public static void handleOpenTargetTagScreenClient(OpenTargetTagScreenS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            Entity entity = player.level().getEntity(payload.entityId());
            if (!(entity instanceof TargetTagManager targetTagManager)) {
                return;
            }
            Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTagMap = new HashMap<>();
            TargetTagManagerImpl.read(targetTagMap, TagValueInput.create(ProblemReporter.DISCARDING, player.level().registryAccess(), payload.nbt()));
            for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                if (entityType.getCategory() != MobCategory.MISC) {
                    TargetIdentifier id = new TargetIdentifier(entityType);
                    if (!targetTagMap.containsKey(id)) {
                        targetTagMap.put(id, targetTagManager.getTargetTag(id));
                    }
                }
            }
            Minecraft.getInstance().gui.setScreen(new TargetTagScreen(entity, targetTagMap));
        });
    }

    public static void handleOpenMaidManagerScreenClient(OpenMaidManagerScreenS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            var lmInfos = new ArrayList<MaidManager.LMInfo>();
            MaidManagerImpl.read(TagValueInput.create(ProblemReporter.DISCARDING, player.level().registryAccess(), payload.nbt()), lmInfos);
            Minecraft.getInstance().gui.setScreen(new MaidManagerScreen(lmInfos));
        });
    }
}
