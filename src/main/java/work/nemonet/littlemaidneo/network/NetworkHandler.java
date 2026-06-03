package work.nemonet.littlemaidneo.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel.Layer;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel.Part;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayable;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.targeting.TargetIdentifier;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManager;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManagerImpl;
import work.nemonet.littlemaidneo.entity.targeting.TargetingSystem;
import work.nemonet.littlemaidneo.entity.util.MaidManager;
import work.nemonet.littlemaidneo.entity.util.MaidManagerImpl;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.resource.holder.ConfigHolder;
import work.nemonet.littlemaidneo.resource.manager.LMConfigManager;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;
import work.nemonet.littlemaidneo.util.PlayerList;

import java.util.Map;
import java.util.Set;

public class NetworkHandler {

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        final boolean isClient = net.neoforged.fml.loading.FMLEnvironment.getDist() == Dist.CLIENT;

        // SyncMultiModel (bidirectional)
        registrar.playBidirectional(
                SyncMultiModelPayload.TYPE,
                SyncMultiModelPayload.STREAM_CODEC,
                NetworkHandler::handleSyncMultiModelServer,
                isClient 
                        ? (payload, context) -> work.nemonet.littlemaidneo.client.network.ClientNetworkHandler.handleSyncMultiModelClient(payload, context)
                        : (payload, context) -> {});

        // SyncSoundPack (bidirectional)
        registrar.playBidirectional(
                SyncSoundPackPayload.TYPE,
                SyncSoundPackPayload.STREAM_CODEC,
                NetworkHandler::handleSyncSoundPackServer,
                isClient
                        ? (payload, context) -> work.nemonet.littlemaidneo.client.network.ClientNetworkHandler.handleSyncSoundPackClient(payload, context)
                        : (payload, context) -> {});

        // LMSound (S2C only)
        if (isClient) {
            registrar.playToClient(
                    LMSoundPayload.TYPE,
                    LMSoundPayload.STREAM_CODEC,
                    (payload, context) -> work.nemonet.littlemaidneo.client.network.ClientNetworkHandler.handleLMSoundClient(payload, context));
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

        // SyncSoundConfig (bidirectional)
        registrar.playBidirectional(
                SyncSoundConfigPayload.TYPE,
                SyncSoundConfigPayload.STREAM_CODEC,
                NetworkHandler::handleSyncSoundConfigServer,
                isClient
                        ? (payload, context) -> work.nemonet.littlemaidneo.client.network.ClientNetworkHandler.handleSyncSoundConfigClient(payload, context)
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
                    (payload, context) -> work.nemonet.littlemaidneo.client.network.ClientNetworkHandler.handleOpenTargetTagScreenClient(payload, context));
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
                    (payload, context) -> work.nemonet.littlemaidneo.client.network.ClientNetworkHandler.handleOpenMaidManagerScreenClient(payload, context));
        } else {
            registrar.playToClient(
                    OpenMaidManagerScreenS2CPayload.TYPE,
                    OpenMaidManagerScreenS2CPayload.STREAM_CODEC);
        }
    }

    // --- SyncMultiModel ---
public static void sendSyncMultiModelC2S(Entity entity, IHasMultiModel hasMultiModel) {
        String textureName = hasMultiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName();
        String armorHead = hasMultiModel.getTextureHolder(Layer.INNER, Part.HEAD).getTextureName();
        String armorBody = hasMultiModel.getTextureHolder(Layer.INNER, Part.BODY).getTextureName();
        String armorLegs = hasMultiModel.getTextureHolder(Layer.INNER, Part.LEGS).getTextureName();
        String armorFeet = hasMultiModel.getTextureHolder(Layer.INNER, Part.FEET).getTextureName();
        ClientPacketDistributor.sendToServer(new SyncMultiModelPayload(
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
public static void sendSyncSoundPackC2S(Entity entity, ConfigHolder configHolder) {
        ClientPacketDistributor.sendToServer(new SyncSoundPackPayload(entity.getId(), configHolder.getName()));
    }

    public static void sendSyncSoundPackS2C(Entity entity, ConfigHolder configHolder) {
        PacketDistributor.sendToPlayersTrackingEntity(entity,
                new SyncSoundPackPayload(entity.getId(), configHolder.getName()));
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
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        var payload = new LMSoundPayload(entity.getId(), soundName);
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(entity) < 16 * 16) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    // --- C2SSetMovingState ---
public static void sendSetMovingStateC2S(Entity entity, MovingMode state) {
        ClientPacketDistributor.sendToServer(new C2SSetMovingStatePayload(entity.getId(), state));
    }

    private static void handleSetMovingStateServer(C2SSetMovingStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Entity entity = player.level().getEntity(payload.entityId());
            if (!(entity instanceof LittleMaidEntity maid)
                    || TameableUtil.getTameOwnerUuid(maid)
                            .filter(ownerId -> ownerId.equals(player.getUUID()))
                            .isEmpty()) {
                return;
            }
            if (maid.isStrike()) {
                return;
            }
            maid.setMovingMode(payload.movingMode());
            maid.getNavigation().stop();
            if (payload.movingMode() == MovingMode.FREEDOM) {
                maid.setFreedomPos(entity.blockPosition());
            }
        });
    }

    // --- C2SSetBloodSuck ---
public static void sendSetBloodSuckC2S(Entity entity, boolean isBloodSuck) {
        ClientPacketDistributor.sendToServer(new C2SSetBloodSuckPayload(entity.getId(), isBloodSuck));
    }

    private static void handleSetBloodSuckServer(C2SSetBloodSuckPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Entity entity = player.level().getEntity(payload.entityId());
            if (!(entity instanceof LittleMaidEntity maid)) {
                return;
            }
            if (TameableUtil.getTameOwnerUuid(maid)
                    .filter(uuid -> player.getUUID().equals(uuid))
                    .isPresent()) {
                maid.setBloodSuck(payload.isBloodSuck());
            }
        });
    }

    // --- C2SSetWorkItemSlotSize ---
public static void sendSetWorkItemSlotSizeC2S(LittleMaidEntity entity, int num) {
        ClientPacketDistributor.sendToServer(new C2SSetWorkItemSlotSizePayload(entity.getId(), num));
    }

    private static void handleSetWorkItemSlotSizeServer(C2SSetWorkItemSlotSizePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Entity entity = player.level().getEntity(payload.entityId());
            if (!(entity instanceof LittleMaidEntity maid)) {
                return;
            }
            if (TameableUtil.getTameOwnerUuid(maid)
                    .filter(uuid -> player.getUUID().equals(uuid))
                    .isPresent()) {
                maid.setWorkItemSlotNum(payload.num());
            }
        });
    }

    // --- C2SSetTargetTags ---
public static <T extends Entity & TargetTagManager> void sendSetTargetTagsC2S(T entity,
            Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags) {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        TargetTagManagerImpl.write(targetTags, output);
        ClientPacketDistributor.sendToServer(new C2SSetTargetTagsPayload(entity.getId(), output.buildResult()));
    }

    private static void handleSetTargetTagsServer(C2SSetTargetTagsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Entity entity = player.level().getEntity(payload.entityId());
            if (!(entity instanceof TargetTagManager targetTagManager)) {
                return;
            }
            if (entity instanceof TamableAnimal tamable
                    && TameableUtil.getTameOwnerUuid(tamable)
                            .filter(id -> id.equals(player.getUUID()))
                            .isEmpty()) {
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
        context.enqueueWork(() -> {
            Player player = context.player();
            Entity entity = player.level().getEntity(payload.entityId());
            if (!(entity instanceof LittleMaidEntity maid)) {
                return;
            }
            if (TameableUtil.getTameOwnerUuid(maid)
                    .filter(uuid -> player.getUUID().equals(uuid))
                    .isPresent()) {
                maid.openInventory(player);
            }
        });
    }

    // --- C2SCallWait ---
public static void sendCallWaitC2S(Entity entity, C2SCallWaitPayload.State state) {
        ClientPacketDistributor.sendToServer(new C2SCallWaitPayload(entity.getId(), state));
    }

    private static void handleCallWaitServer(C2SCallWaitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Entity entity = player.level().getEntity(payload.entityId());
            if (!(entity instanceof LittleMaidEntity maid)
                    || !TameableUtil.isTameOwner(maid, player)) {
                return;
            }
            if (maid.isStrike()) {
                return;
            }
            if (payload.state() == C2SCallWaitPayload.State.WAIT) {
                TameableUtil.setWait(maid, true);
            } else {
                TameableUtil.setWait(maid, false);
                maid.setMovingMode(MovingMode.ESCORT);
            }
        });
    }

    // --- SyncSoundConfig ---
public static void sendSyncSoundConfigC2S(Entity entity, String configName) {
        ClientPacketDistributor.sendToServer(new SyncSoundConfigPayload(entity.getId(), configName));
    }

    public static void sendSyncSoundConfigS2C(Entity entity, String configName) {
        for (ServerPlayer player : PlayerList.tracking(entity)) {
            PacketDistributor.sendToPlayer(player, new SyncSoundConfigPayload(entity.getId(), configName));
        }
    }

    private static void handleSyncSoundConfigServer(SyncSoundConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level world = player.level();
            Entity entity = world.getEntity(payload.entityId());
            if (!(entity instanceof SoundPlayable soundPlayable)) {
                return;
            }
            if (entity instanceof OwnableEntity tameable
                    && TameableUtil.getTameOwnerUuid(tameable)
                            .filter(ownerId -> ownerId.equals(player.getUUID()))
                            .isEmpty()) {
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
        context.enqueueWork(() -> {
            Player player = context.player();
            Entity entity = player.level().getEntity(payload.entityId());
            if (entity == null || (!(entity instanceof TargetTagManager) && !entity.hasData(ModRegistration.TARGET_TAG_ATTACHMENT.get()))
                    || (entity instanceof TamableAnimal tamable
                            && TameableUtil.getTameOwnerUuid(tamable)
                                     .filter(id -> id.equals(player.getUUID()))
                                     .isEmpty())) {
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
