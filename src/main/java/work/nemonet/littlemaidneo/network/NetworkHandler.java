package work.nemonet.littlemaidneo.network;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel.Layer;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel.Part;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayable;
import work.nemonet.littlemaidneo.resource.holder.ConfigHolder;
import work.nemonet.littlemaidneo.resource.manager.LMConfigManager;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;
import work.nemonet.littlemaidneo.resource.util.TextureColors;

public class NetworkHandler {

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(
                SyncMultiModelPayload.TYPE,
                SyncMultiModelPayload.STREAM_CODEC,
                NetworkHandler::handleSyncMultiModelClient);
        registrar.playToClient(
                SyncSoundPackPayload.TYPE,
                SyncSoundPackPayload.STREAM_CODEC,
                NetworkHandler::handleSyncSoundPackClient);
        registrar.playToClient(
                LMSoundPayload.TYPE,
                LMSoundPayload.STREAM_CODEC,
                NetworkHandler::handleLMSoundClient);
        registrar.playToServer(
                SyncMultiModelPayload.TYPE,
                SyncMultiModelPayload.STREAM_CODEC,
                NetworkHandler::handleSyncMultiModelServer);
        registrar.playToServer(
                SyncSoundPackPayload.TYPE,
                SyncSoundPackPayload.STREAM_CODEC,
                NetworkHandler::handleSyncSoundPackServer);
    }

    // --- SyncMultiModel ---

    @OnlyIn(Dist.CLIENT)
    public static void sendSyncMultiModelC2S(Entity entity, IHasMultiModel hasMultiModel) {
        String textureName = hasMultiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName();
        String armorHead = hasMultiModel.getTextureHolder(Layer.INNER, Part.HEAD).getTextureName();
        String armorBody = hasMultiModel.getTextureHolder(Layer.INNER, Part.BODY).getTextureName();
        String armorLegs = hasMultiModel.getTextureHolder(Layer.INNER, Part.LEGS).getTextureName();
        String armorFeet = hasMultiModel.getTextureHolder(Layer.INNER, Part.FEET).getTextureName();
        PacketDistributor.sendToServer(new SyncMultiModelPayload(
                entity.getId(), textureName, armorHead, armorBody, armorLegs, armorFeet,
                hasMultiModel.getColorMM(), hasMultiModel.isContractMM()));
    }

    public static void sendSyncMultiModelS2C(Entity entity, IHasMultiModel hasMultiModel) {
        String textureName = hasMultiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName();
        String armorHead = hasMultiModel.getTextureHolder(Layer.INNER, Part.HEAD).getTextureName();
        String armorBody = hasMultiModel.getTextureHolder(Layer.INNER, Part.BODY).getTextureName();
        String armorLegs = hasMultiModel.getTextureHolder(Layer.INNER, Part.LEGS).getTextureName();
        String armorFeet = hasMultiModel.getTextureHolder(Layer.INNER, Part.FEET).getTextureName();
        PacketDistributor.sendToPlayersTrackingEntity(entity,
                new SyncMultiModelPayload(entity.getId(), textureName, armorHead, armorBody, armorLegs, armorFeet,
                        hasMultiModel.getColorMM(), hasMultiModel.isContractMM()));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleSyncMultiModelClient(SyncMultiModelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> applyMultiModelClient(
                payload.entityId(), payload.isContract(), payload.color(),
                payload.textureName(), payload.getArmorSets()));
    }

    @OnlyIn(Dist.CLIENT)
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

    private static void handleSyncMultiModelServer(SyncMultiModelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Entity entity = player.level().getEntity(payload.entityId());
            if (!(entity instanceof IHasMultiModel multiModel)) return;
            multiModel.setContractMM(payload.isContract());
            multiModel.setColorMM(payload.color());
            LMTextureManager textureManager = LMTextureManager.INSTANCE;
            textureManager.getTexture(payload.textureName())
                    .filter(th -> multiModel.isAllowChangeTexture(entity, th, Layer.SKIN, Part.HEAD))
                    .ifPresent(th -> multiModel.setTextureHolder(th, Layer.SKIN, Part.HEAD));
            ArmorSets<String> armorSets = payload.getArmorSets();
            for (Part part : Part.values()) {
                String armorName = armorSets.getArmor(part)
                        .orElseThrow(() -> new IllegalStateException("テクスチャが存在しません。"));
                textureManager.getTexture(armorName)
                        .filter(th -> multiModel.isAllowChangeTexture(entity, th, Layer.INNER, part))
                        .ifPresent(th -> multiModel.setTextureHolder(th, Layer.INNER, part));
            }
            sendSyncMultiModelS2C(entity, multiModel);
        });
    }

    // --- SyncSoundPack ---

    @OnlyIn(Dist.CLIENT)
    public static void sendSyncSoundPackC2S(Entity entity, ConfigHolder configHolder) {
        PacketDistributor.sendToServer(new SyncSoundPackPayload(entity.getId(), configHolder.getName()));
    }

    public static void sendSyncSoundPackS2C(Entity entity, ConfigHolder configHolder) {
        PacketDistributor.sendToPlayersTrackingEntity(entity,
                new SyncSoundPackPayload(entity.getId(), configHolder.getName()));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleSyncSoundPackClient(SyncSoundPackPayload payload, IPayloadContext context) {
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

    private static void handleSyncSoundPackServer(SyncSoundPackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Entity entity = player.level().getEntity(payload.entityId());
            if (!(entity instanceof SoundPlayable soundPlayable)) return;
            ConfigHolder configHolder = LMConfigManager.INSTANCE.getConfig(payload.soundPackName())
                    .orElse(LMConfigManager.EMPTY_CONFIG);
            soundPlayable.setConfigHolder(configHolder);
            sendSyncSoundPackS2C(entity, configHolder);
        });
    }

    // --- LMSound ---

    public static void sendLMSoundS2C(Entity entity, String soundName) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        var payload = new LMSoundPayload(entity.getId(), soundName);
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(entity) < 16 * 16) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleLMSoundClient(LMSoundPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            Entity entity = player.level().getEntity(payload.entityId());
            if (entity instanceof SoundPlayable soundPlayable) {
                soundPlayable.play(payload.soundName());
            }
        });
    }
}
