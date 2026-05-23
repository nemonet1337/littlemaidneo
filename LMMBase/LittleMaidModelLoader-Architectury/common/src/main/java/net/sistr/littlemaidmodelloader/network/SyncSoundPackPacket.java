package net.sistr.littlemaidmodelloader.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.sistr.littlemaidmodelloader.LMMLMod;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.holder.ConfigHolder;
import net.sistr.littlemaidmodelloader.resource.manager.LMConfigManager;
import net.sistr.littlemaidmodelloader.util.PlayerList;

public class SyncSoundPackPacket {
    // 1.21.1変更: ResourceLocationコンストラクタが非公開になり、fromNamespaceAndPathを使用
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(LMMLMod.MODID, "sync_sound_pack");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, ConfigHolder configHolder) {
        RegistryFriendlyByteBuf passedData = createC2SPacket(entity, configHolder);
        NetworkManager.sendToServer(ID, passedData);
    }

    public static RegistryFriendlyByteBuf createC2SPacket(Entity entity, ConfigHolder configHolder) {
        RegistryFriendlyByteBuf passedData = new RegistryFriendlyByteBuf(Unpooled.buffer(), null);
        passedData.writeInt(entity.getId());
        passedData.writeUtf(configHolder.getName());
        return passedData;
    }

    public static void sendS2CPacket(Entity entity, ConfigHolder configHolder) {
        RegistryFriendlyByteBuf passedData = createS2CPacket(entity, configHolder);
        NetworkManager.sendToPlayers(PlayerList.tracking(entity), ID, passedData);
    }

    public static RegistryFriendlyByteBuf createS2CPacket(Entity entity, ConfigHolder configHolder) {
        RegistryFriendlyByteBuf passedData = new RegistryFriendlyByteBuf(Unpooled.buffer(), null);
        passedData.writeInt(entity.getId());
        passedData.writeUtf(configHolder.getName());
        return passedData;
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int entityId = buf.readInt();
        String soundPackName = buf.readUtf();
        context.queue(() ->
                applyMultiModelClient(entityId, soundPackName));
    }

    //context.getTaskQueue().execute()の中では@Environmentの効力が及ばないため別メソッドに分離
    @Environment(EnvType.CLIENT)
    public static void applyMultiModelClient(int entityId, String soundPackName) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity entity = level.getEntity(entityId);
        if (!(entity instanceof SoundPlayable soundPlayable)) return;
        ConfigHolder configHolder = LMConfigManager.INSTANCE.getConfig(soundPackName).orElse(LMConfigManager.EMPTY_CONFIG);
        soundPlayable.setConfigHolder(configHolder);
    }

    public static void receiveC2SPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int entityId = buf.readInt();
        String soundPackName = buf.readUtf();
        context.queue(() ->
                applyMultiModelServer(context.getPlayer(), entityId, soundPackName));
    }

    //クライアントに倣って分離
    public static void applyMultiModelServer(Player player, int entityId, String soundPackName) {
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof SoundPlayable soundPlayable)) return;
        ConfigHolder configHolder = LMConfigManager.INSTANCE.getConfig(soundPackName).orElse(LMConfigManager.EMPTY_CONFIG);
        soundPlayable.setConfigHolder(configHolder);
        sendS2CPacket(entity, configHolder);
    }

}
