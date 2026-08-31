package work.nemonet.littlemaidneo.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import work.nemonet.littlemaidneo.client.ClientNetworkHandler;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel.Layer;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel.Part;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayable;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.targeting.TargetIdentifier;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManager;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManagerImpl;
import work.nemonet.littlemaidneo.entity.targeting.TargetingSystem;
import work.nemonet.littlemaidneo.entity.util.MaidManagerImpl;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.resource.holder.ConfigHolder;
import work.nemonet.littlemaidneo.resource.manager.LMConfigManager;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;

import java.util.Map;
import java.util.Set;

public class NetworkHandler {

    public static void register(RegisterPayloadHandlersEvent event) {
        // modelloader 側の SoundPlayableCompound はネットワーク層へ直接依存できないため、
        // サーバー側の音声同期送信をここで注入する
        work.nemonet.littlemaidneo.entity.compound.SoundPlayableCompound
                .setSoundSyncSender(NetworkHandler::sendLMSoundS2C);

        var registrar = event.registrar("1");
        final boolean isClient = net.neoforged.fml.loading.FMLEnvironment.getDist() == Dist.CLIENT;

        // SyncMultiModel (bidirectional)
        registrar.playBidirectional(
                SyncMultiModelPayload.TYPE,
                SyncMultiModelPayload.STREAM_CODEC,
                NetworkHandler::handleSyncMultiModelServer,
                 isClient 
                        ? ClientNetworkHandler::handleSyncMultiModelClient
                        : (payload, context) -> {});

        // SyncSoundPack (bidirectional)
        registrar.playBidirectional(
                SyncSoundPackPayload.TYPE,
                SyncSoundPackPayload.STREAM_CODEC,
                NetworkHandler::handleSyncSoundPackServer,
                 isClient
                        ? ClientNetworkHandler::handleSyncSoundPackClient
                        : (payload, context) -> {});

        // LMSound (S2C only)
        if (isClient) {
            registrar.playToClient(
                    LMSoundPayload.TYPE,
                    LMSoundPayload.STREAM_CODEC,
                    ClientNetworkHandler::handleLMSoundClient);
        } else {
            registrar.playToClient(
                    LMSoundPayload.TYPE,
                    LMSoundPayload.STREAM_CODEC);
        }

        // C2S packets
        registrar.playToServer(
                C2SSetMovingStatePayload.TYPE,
                C2SSetMovingStatePayload.STREAM_CODEC,
                NetworkHandler::handleSetMovingStateServer);
        registrar.playToServer(
                C2SSetBloodSuckPayload.TYPE,
                C2SSetBloodSuckPayload.STREAM_CODEC,
                NetworkHandler::handleSetBloodSuckServer);
        registrar.playToServer(
                C2SSetWorkItemSlotSizePayload.TYPE,
                C2SSetWorkItemSlotSizePayload.STREAM_CODEC,
                NetworkHandler::handleSetWorkItemSlotSizeServer);
        registrar.playToServer(
                C2SSetTargetTagsPayload.TYPE,
                C2SSetTargetTagsPayload.STREAM_CODEC,
                NetworkHandler::handleSetTargetTagsServer);
        registrar.playToServer(
                C2SOpenInventoryPayload.TYPE,
                C2SOpenInventoryPayload.STREAM_CODEC,
                NetworkHandler::handleOpenInventoryServer);
        registrar.playToServer(
                C2SCallWaitPayload.TYPE,
                C2SCallWaitPayload.STREAM_CODEC,
                NetworkHandler::handleCallWaitServer);
        registrar.playToServer(
                C2SSetMaidGroupPayload.TYPE,
                C2SSetMaidGroupPayload.STREAM_CODEC,
                NetworkHandler::handleSetMaidGroupServer);

        // SyncSoundConfig (bidirectional)
        registrar.playBidirectional(
                SyncSoundConfigPayload.TYPE,
                SyncSoundConfigPayload.STREAM_CODEC,
                NetworkHandler::handleSyncSoundConfigServer,
                 isClient
                        ? ClientNetworkHandler::handleSyncSoundConfigClient
                        : (payload, context) -> {});

        // OpenTargetTagScreen (split C2S / S2C)
        registrar.playToServer(
                OpenTargetTagScreenC2SPayload.TYPE,
                OpenTargetTagScreenC2SPayload.STREAM_CODEC,
                NetworkHandler::handleOpenTargetTagScreenServer);
        if (isClient) {
            registrar.playToClient(
                    OpenTargetTagScreenS2CPayload.TYPE,
                    OpenTargetTagScreenS2CPayload.STREAM_CODEC,
                    ClientNetworkHandler::handleOpenTargetTagScreenClient);
        } else {
            registrar.playToClient(
                    OpenTargetTagScreenS2CPayload.TYPE,
                    OpenTargetTagScreenS2CPayload.STREAM_CODEC);
        }

        // OpenMaidManagerScreen (split C2S / S2C)
        registrar.playToServer(
                OpenMaidManagerScreenC2SPayload.TYPE,
                OpenMaidManagerScreenC2SPayload.STREAM_CODEC,
                NetworkHandler::handleOpenMaidManagerScreenServer);
        if (isClient) {
            registrar.playToClient(
                    OpenMaidManagerScreenS2CPayload.TYPE,
                    OpenMaidManagerScreenS2CPayload.STREAM_CODEC,
                    ClientNetworkHandler::handleOpenMaidManagerScreenClient);
        } else {
            registrar.playToClient(
                    OpenMaidManagerScreenS2CPayload.TYPE,
                    OpenMaidManagerScreenS2CPayload.STREAM_CODEC);
        }
    }

    // --- SyncMultiModel ---
    private static ArmorSets<String> collectTextureNames(IHasMultiModel hasMultiModel) {
        ArmorSets<String> sets = new ArmorSets<>();
        sets.setArmor(hasMultiModel.getTextureHolder(Layer.INNER, Part.HEAD).getTextureName(), Part.HEAD);
        sets.setArmor(hasMultiModel.getTextureHolder(Layer.INNER, Part.BODY).getTextureName(), Part.BODY);
        sets.setArmor(hasMultiModel.getTextureHolder(Layer.INNER, Part.LEGS).getTextureName(), Part.LEGS);
        sets.setArmor(hasMultiModel.getTextureHolder(Layer.INNER, Part.FEET).getTextureName(), Part.FEET);
        return sets;
    }

    public static void sendSyncMultiModelC2S(Entity entity, IHasMultiModel hasMultiModel) {
        String textureName = hasMultiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName();
        ClientPacketDistributor.sendToServer(new SyncMultiModelPayload(
                entity.getId(), textureName, collectTextureNames(hasMultiModel),
                hasMultiModel.getColorMM(), hasMultiModel.isContractMM()));
    }

    public static void sendSyncMultiModelS2C(Entity entity, IHasMultiModel hasMultiModel) {
        String textureName = hasMultiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName();
        PacketDistributor.sendToPlayersTrackingEntity(entity,
                new SyncMultiModelPayload(entity.getId(), textureName, collectTextureNames(hasMultiModel),
                        hasMultiModel.getColorMM(), hasMultiModel.isContractMM()));
    }

    private static void handleSyncMultiModelServer(SyncMultiModelPayload payload, IPayloadContext context) {
        PayloadHandlers.resolveEntity(context, payload.entityId(), Entity.class, (player, entity) -> {
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
public static void sendSyncSoundPackC2S(Entity entity, ConfigHolder configHolder) {
        ClientPacketDistributor.sendToServer(new SyncSoundPackPayload(entity.getId(), configHolder.getName()));
    }

    public static void sendSyncSoundPackS2C(Entity entity, ConfigHolder configHolder) {
        PacketDistributor.sendToPlayersTrackingEntity(entity,
                new SyncSoundPackPayload(entity.getId(), configHolder.getName()));
    }

    private static void handleSyncSoundPackServer(SyncSoundPackPayload payload, IPayloadContext context) {
        PayloadHandlers.resolveEntity(context, payload.entityId(), Entity.class, (player, entity) -> {
            if (!(entity instanceof SoundPlayable soundPlayable)) return;
            if (PayloadHandlers.isNotOwner(player, entity)) {
                return;
            }
            ConfigHolder configHolder = LMConfigManager.INSTANCE.getConfig(payload.soundPackName())
                    .orElse(LMConfigManager.EMPTY_CONFIG);
            soundPlayable.setConfigHolder(configHolder);
            sendSyncSoundPackS2C(entity, configHolder);
        });
    }

    // --- LMSound ---

    public static void sendLMSoundS2C(Entity entity, String soundName) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersNear(
                serverLevel,
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                16.0,
                new LMSoundPayload(entity.getId(), soundName));
    }

    // --- C2SSetMovingState ---
public static void sendSetMovingStateC2S(Entity entity, MaidMode state) {
        ClientPacketDistributor.sendToServer(new C2SSetMovingStatePayload(entity.getId(), state));
    }

    private static void handleSetMovingStateServer(C2SSetMovingStatePayload payload, IPayloadContext context) {
        PayloadHandlers.onOwnedMaid(context, payload.entityId(), (player, maid) -> {
            if (maid.isStrike()) {
                return;
            }
            maid.setMaidMode(payload.movingMode());
            maid.getNavigation().stop();
            if (payload.movingMode() == MaidMode.FREEDOM) {
                maid.setFreedomPos(maid.blockPosition());
            }
        });
    }

    // --- C2SSetBloodSuck ---
    public static void sendSetBloodSuckC2S(Entity entity, boolean isBloodSuck) {
        ClientPacketDistributor.sendToServer(new C2SSetBloodSuckPayload(entity.getId(), isBloodSuck));
    }

    private static void handleSetBloodSuckServer(C2SSetBloodSuckPayload payload, IPayloadContext context) {
        PayloadHandlers.onOwnedMaid(context, payload.entityId(), (player, maid) -> maid.setBloodSuck(payload.isBloodSuck()));
    }

    // --- C2SSetWorkItemSlotSize ---
    public static void sendSetWorkItemSlotSizeC2S(LittleMaidEntity entity, int num) {
        ClientPacketDistributor.sendToServer(new C2SSetWorkItemSlotSizePayload(entity.getId(), num));
    }

    private static void handleSetWorkItemSlotSizeServer(C2SSetWorkItemSlotSizePayload payload, IPayloadContext context) {
        PayloadHandlers.onOwnedMaid(context, payload.entityId(), (player, maid) -> maid.setWorkItemSlotNum(payload.num()));
    }

    // --- C2SSetTargetTags ---
    public static <T extends Entity & TargetTagManager> void sendSetTargetTagsC2S(T entity,
            Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags) {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        TargetTagManagerImpl.write(targetTags, output);
        ClientPacketDistributor.sendToServer(new C2SSetTargetTagsPayload(entity.getId(), output.buildResult()));
    }

    private static void handleSetTargetTagsServer(C2SSetTargetTagsPayload payload, IPayloadContext context) {
        PayloadHandlers.resolveEntity(context, payload.entityId(), Entity.class, (player, entity) -> {
            if (!(entity instanceof TargetTagManager targetTagManager)) {
                return;
            }
            if (PayloadHandlers.isNotOwner(player, entity)) {
                return;
            }
            targetTagManager.readTargetTags(TagValueInput.create(ProblemReporter.DISCARDING, player.level().registryAccess(), payload.tag()));
        });
    }

    // --- C2SOpenInventory ---
    public static void sendOpenInventoryC2S(Entity entity) {
        ClientPacketDistributor.sendToServer(new C2SOpenInventoryPayload(entity.getId()));
    }

    private static void handleOpenInventoryServer(C2SOpenInventoryPayload payload, IPayloadContext context) {
        PayloadHandlers.onOwnedMaid(context, payload.entityId(), (player, maid) -> maid.openInventory(player));
    }

    // --- C2SCallWait ---
    public static void sendCallWaitC2S(Entity entity, C2SCallWaitPayload.State state) {
        ClientPacketDistributor.sendToServer(new C2SCallWaitPayload(entity.getId(), state));
    }

    public static void sendSetMaidGroupC2S(java.util.UUID maidId, String group) {
        ClientPacketDistributor.sendToServer(new C2SSetMaidGroupPayload(maidId, group));
    }

    private static void handleSetMaidGroupServer(C2SSetMaidGroupPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            player.getData(ModRegistration.MAID_MANAGER_ATTACHMENT.get())
                    .setGroup(payload.maidId(), payload.group());
        });
    }

    private static void handleCallWaitServer(C2SCallWaitPayload payload, IPayloadContext context) {
        PayloadHandlers.onOwnedMaid(context, payload.entityId(), (player, maid) -> {
            if (maid.isStrike()) {
                return;
            }
            if (payload.state() == C2SCallWaitPayload.State.WAIT) {
                TameableUtil.setWait(maid, true);
            } else {
                TameableUtil.setWait(maid, false);
                maid.setMaidMode(MaidMode.ESCORT);
            }
        });
    }

    // --- SyncSoundConfig ---
    public static void sendSyncSoundConfigC2S(Entity entity, String configName) {
        ClientPacketDistributor.sendToServer(new SyncSoundConfigPayload(entity.getId(), configName));
    }

    public static void sendSyncSoundConfigS2C(Entity entity, String configName) {
        PacketDistributor.sendToPlayersTrackingEntity(entity,
                new SyncSoundConfigPayload(entity.getId(), configName));
    }

    private static void handleSyncSoundConfigServer(SyncSoundConfigPayload payload, IPayloadContext context) {
        PayloadHandlers.resolveEntity(context, payload.entityId(), Entity.class, (player, entity) -> {
            if (!(entity instanceof SoundPlayable soundPlayable)) {
                return;
            }
            if (PayloadHandlers.isNotOwner(player, entity)) {
                return;
            }
            LMConfigManager.INSTANCE.getConfig(payload.configName())
                    .ifPresent(soundPlayable::setConfigHolder);
            sendSyncSoundConfigS2C(entity, payload.configName());
        });
    }

    // --- OpenTargetTagScreen ---
    public static void sendOpenTargetTagScreenC2S(Entity entity) {
        ClientPacketDistributor.sendToServer(new OpenTargetTagScreenC2SPayload(entity.getId()));
    }

    public static void sendOpenTargetTagScreenS2C(Entity entity, Player player) {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        if (entity instanceof TargetTagManager) {
            ((TargetTagManager) entity).writeTargetTags(output);
        } else {
            entity.getData(ModRegistration.TARGET_TAG_ATTACHMENT.get()).writeTargetTags(output);
        }
        PacketDistributor.sendToPlayer((ServerPlayer) player,
                new OpenTargetTagScreenS2CPayload(entity.getId(), output.buildResult()));
    }

    private static void handleOpenTargetTagScreenServer(OpenTargetTagScreenC2SPayload payload, IPayloadContext context) {
        PayloadHandlers.resolveEntity(context, payload.entityId(), Entity.class, (player, entity) -> {
            if ((!(entity instanceof TargetTagManager) && !entity.hasData(ModRegistration.TARGET_TAG_ATTACHMENT.get()))
                    || PayloadHandlers.isNotOwner(player, entity)) {
                return;
            }
            sendOpenTargetTagScreenS2C(entity, player);
        });
    }

    // --- OpenMaidManagerScreen ---
    public static void sendOpenMaidManagerScreenC2S() {
        ClientPacketDistributor.sendToServer(OpenMaidManagerScreenC2SPayload.INSTANCE);
    }

    public static void sendOpenMaidManagerScreenS2C(Player player) {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        var lmInfos = player.getData(ModRegistration.MAID_MANAGER_ATTACHMENT.get()).getMaidList();
        MaidManagerImpl.write(output, lmInfos);
        PacketDistributor.sendToPlayer((ServerPlayer) player, new OpenMaidManagerScreenS2CPayload(output.buildResult()));
    }

    private static void handleOpenMaidManagerScreenServer(OpenMaidManagerScreenC2SPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            sendOpenMaidManagerScreenS2C(player);
        });
    }
}
